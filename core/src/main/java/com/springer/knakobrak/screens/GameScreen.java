package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.NetworkListener;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.*;
import com.springer.knakobrak.util.Constants;

import java.util.HashMap;
import java.util.Map;

import static com.springer.knakobrak.util.Constants.*;

public class GameScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private Stage stage;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture background;
    private Texture wallTexture;
    private Texture spawnTexture;

    private Viewport worldViewPort;

    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private Label timeLabel;
    private Label coordinatesLabel;
    private Label chatLog;
    private ScrollPane chatScroll;
    private TextField chatInput;
    private Table rootTable;

    private boolean chatMode = false;

    private PhysicsSimulation simulation;
    private PlayerState localPlayer;
    private Map<Integer, Texture> playerSkins = new HashMap<>();
    private Map<Integer, Texture> ballSkins = new HashMap<>();

    int nextInputId = 0;
    volatile PlayerInputMessage latestInput;

    private WorldSnapshotMessage previousSnapshot;
    private WorldSnapshotMessage latestSnapshot;

    static final float INTERPOLATION_DELAY = 0.15f;

    private float localTime;

    Map<Integer, PredictedProjectile> predictedProjectiles = new HashMap<>();
    int[][] wallGrid;

    public GameScreen(LanPvpGame game) {
        this.game = game;
        this.batch = game.batch;
        this.simulation = game.simulation;
        this.localPlayer = game.localPlayer;
        this.localTime = 0f;
        this.wallGrid = simulation.getWallGrid();
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);


        timeLabel = new Label("Time: 0.0s", game.uiSkin);

        coordinatesLabel = new Label("x: 0.0, y: 0.0", game.uiSkin);
        coordinatesLabel.setFontScale(1.5f);

        chatLog = new Label("", game.uiSkin);
        chatLog.setWrap(true);

        chatScroll = new ScrollPane(chatLog, game.uiSkin);
        chatScroll.setFadeScrollBars(false);

        chatInput = new TextField("", game.uiSkin);
        chatInput.setVisible(false);


        rootTable.add(coordinatesLabel).width(150).left();
        rootTable.add(timeLabel).expandX().top();
        rootTable.add(new Label("", game.uiSkin)).width(150);
        rootTable.row().padTop(30).padBottom(50);

        rootTable.add(new Label("", game.uiSkin));
        rootTable.add(new Label("", game.uiSkin)).expand();
        rootTable.add(new Label("", game.uiSkin));
        rootTable.row();

        rootTable.add(chatInput).width(400).height(30).left().bottom().padBottom(30);
        rootTable.add(new Label("", game.uiSkin));
        rootTable.add(chatScroll).width(400).height(150).right().bottom().padBottom(30);

        //rootTable.setDebug(true);


        shapeRenderer = new ShapeRenderer();
        background = new Texture("grass_bg.png");
        wallTexture = new Texture("tiles/wall.png");
        spawnTexture = new Texture("tiles/spawn.png");

        int i = 0;
        for (PlayerState ps : simulation.getPlayers().values()) {
            playerSkins.put(i, new Texture("characters/p" + ps.playerIcon + ".png"));
            ballSkins.put(i, new Texture("balls/b" + ps.ballIcon + ".png"));
            i++;
        }
    }

    float physicsAccumulator = 0f;
    float FIXED_DT = 1 / 60f;

    int secondsCounter = 0;
    float secondsAccumulator = 0f;

    @Override
    public void render(float delta) {

        if (game.localPlayer == null) {
            System.out.println("Local player ["+game.playerId+"] is null!");
            game.localPlayer = simulation.getPlayer(game.playerId);
            return;
        }

        game.dispatchNetworkMessages();

        handlePlayerReconciliation();

        input(delta);

        physicsAccumulator += delta;
        while (physicsAccumulator >= FIXED_DT) {
            simulation.step(FIXED_DT);
            physicsAccumulator -= FIXED_DT;
        }

        syncBody();

        handlePlayerInterpolation();

        localLogic();

        syncProjectileBodies();

        draw();

        stage.act(delta);
        stage.draw();

        localTime += delta;

        secondsAccumulator += delta;
        if (secondsAccumulator > 1f) {
            ++secondsCounter;
            System.out.println("Time: " + secondsCounter);
            secondsAccumulator -= 1f;
        }
        //System.out.println(localTime);
    }

    private void handleProjectiles() {
        if (latestSnapshot == null || previousSnapshot == null) return;

        for (ProjectileSnapshot pss : latestSnapshot.projectiles) {
            if (pss.ownerId == localPlayer.id) {

                PredictedProjectile predicted = predictedProjectiles.remove(pss.fireSequence);

                if (predicted != null) {
                    destroy(predicted.projectile);

                    ProjectileState proj = spawnProjectileFromSnapshot(pss);
                    simulation.addProjectile(pss.id, proj);
                    //simulation.projectiles.put(pss.id, proj);
                    continue;
                }
            }

            if (!simulation.containsProjectileKey(pss.id)) {
                simulation.addProjectile(pss.id, spawnProjectileFromSnapshot(pss));
            }
        }
    }

    private void destroy(ProjectileState p) {
        if (p == null) return;
        if (p.body != null) {
            simulation.destroyBody(p.body);
            p.body = null;
        }
        predictedProjectiles.remove(p.id);
    }

    private ProjectileState spawnProjectileFromSnapshot(ProjectileSnapshot ps) {
        System.out.println("Spawning official bullet!");
        ProjectileState proj = new ProjectileState();
        proj.id = ps.id;
        proj.ownerId = ps.ownerId;
        //System.out.println("[2]: Firing sequence: " + ps.fireSequence);
        Vector2 dir = new Vector2(ps.vx, ps.vy).nor();
        Vector2 spawnPos = new Vector2(ps.x, ps.y)
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));
        proj.body = LoadUtillities.createProjectile(
            simulation.getWorld(),
            spawnPos.x,
            spawnPos.y,
            proj.id
        );
        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );

        return proj;
    }

    private void syncBody() {
        Vector2 pos = localPlayer.body.getPosition();
        localPlayer.x = pos.x;
        localPlayer.y = pos.y;
    }

    private void syncProjectileBodies() {
        for (PredictedProjectile ps : predictedProjectiles.values()) {
            Vector2 pos = ps.projectile.body.getPosition();
            ps.projectile.x = pos.x;
            ps.projectile.y = pos.y;
        }
        for (ProjectileState ps : simulation.getProjectiles().values()) {
            Vector2 pos = ps.body.getPosition();
            ps.x = pos.x;
            ps.y = pos.y;
        }
    }

    private void enterChatMode() {
        chatInput.setVisible(true);
        stage.setKeyboardFocus(chatInput);
    }

    private void exitChatMode() {
        String msg = chatInput.getText();

        if (!msg.isEmpty()) {
            // Send to server
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.message = msg;
            game.client.send(chatMessage);
        }

        chatInput.setText("");
        chatInput.setVisible(false);
        stage.unfocusAll();
    }

    private float inputAccumulator = 0f;

    private float intentDx = 0, intentDy = 0;
    private void input(float delta) {

        inputAccumulator += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            chatMode = !chatMode;
            if (chatMode) {
                enterChatMode();
            }
            else {
                exitChatMode();
            }
        }

        intentDx = 0;
        intentDy = 0;
        if (!chatMode) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) intentDy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) intentDy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) intentDx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) intentDx += 1;
        }

        if (inputAccumulator >= FIXED_DT) {
            inputAccumulator -= FIXED_DT;

            PlayerInputMessage pim = new PlayerInputMessage();
            pim.playerId = localPlayer.id;
            pim.sequence = nextInputId++;
            pim.dx = intentDx;
            pim.dy = intentDy;

            latestInput = pim;
            game.client.send(latestInput);

            applyMovement(pim);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 mouse = new Vector3(
                Gdx.input.getX(),
                Gdx.input.getY(),
                0
            );

            worldViewPort.unproject(mouse);

            float mouseDx = mouse.x - Constants.metersToPx(localPlayer.x);
            float mouseDy = mouse.y - Constants.metersToPx(localPlayer.y);

            float len = (float)Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
            if (len == 0) return;

            mouseDx /= len;
            mouseDy /= len;

            ProjectileState p = spawnLocalProjectile(mouseDx, mouseDy);
            PredictedProjectile pp = new PredictedProjectile();
            pp.fireSequence = nextProjectileId++;
            pp.projectile = p;
            predictedProjectiles.put(pp.fireSequence, pp);

            SpawnProjectileMessage spm = new SpawnProjectileMessage();
            spm.ownerId = localPlayer.id;
            spm.fireSequence = pp.fireSequence;
            spm.dx = p.body.getLinearVelocity().x;
            spm.dy = p.body.getLinearVelocity().y;
            game.client.send(spm);
            System.out.println("Bodies in world: " + simulation.getWorld().getBodyCount());
        }
    }

    private ProjectileState spawnLocalProjectile(float dx, float dy) {
        System.out.println("Spawning predicted bullet!");
        ProjectileState p = new ProjectileState();
        p.ownerId = localPlayer.id;
        Vector2 dir = new Vector2(dx, dy).nor();

        Vector2 spawnPos = localPlayer.body.getPosition()
            .cpy()
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));

        p.body = LoadUtillities.createPredictedProjectile(
            simulation.getWorld(),
            spawnPos.x,
            spawnPos.y,
            p.id
        );

        p.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );

        return p;
    }

    private int nextProjectileId = 1;
    private ProjectileState spawnProjectile(PlayerState ps, float dx, float dy) {
        ProjectileState proj = new ProjectileState();
        proj.ownerId = ps.id;
        Vector2 dir = new Vector2(dx, dy).nor();

        Vector2 spawnPos = ps.body.getPosition()
            .cpy()
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));

        proj.body = LoadUtillities.createProjectile(
            simulation.getWorld(),
            spawnPos.x,
            spawnPos.y,
            proj.id
        );

        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );

        return proj;
    }

    private void applyMovement(PlayerInputMessage input) {
        Vector2 desiredVelocity = new Vector2(input.dx, input.dy)
            .nor()
            .scl(PLAYER_SPEED_MPS);
        localPlayer.body.setLinearVelocity(desiredVelocity);
    }

    private void moveCameraToPlayer() {
        if (localPlayer != null) {
            float x = Constants.metersToPx(localPlayer.x);
            float y = Constants.metersToPx(localPlayer.y);
            OrthographicCamera cam = (OrthographicCamera) worldViewPort.getCamera();
            cam.position.set(x, y, 0);
        }
    }

    private void localLogic() {
        moveCameraToPlayer();
        updateCoordinateLabel();
        updateTimeLabel();
    }

    private void handlePlayerReconciliation() {
        if (latestSnapshot == null || previousSnapshot == null) return;
        if (latestSnapshot.serverTime <= previousSnapshot.serverTime) return;
        PlayerSnapshot serverSelf = latestSnapshot.players.get(localPlayer.id);

        if (serverSelf == null) return;

        Vector2 pos = localPlayer.body.getPosition();

        float errorX = serverSelf.x - localPlayer.body.getPosition().x;
        float errorY = serverSelf.y - localPlayer.body.getPosition().y;

        float errorSq = errorX * errorX +errorY * errorY;
        if (errorSq > 0.25f) {
            localPlayer.body.setTransform(serverSelf.x, serverSelf.y, localPlayer.body.getAngle());
            localPlayer.body.setLinearVelocity(0, 0);
        }

        float correctionStrength = 0.1f; // tweak (0.05–0.2)
        localPlayer.body.setTransform(
            pos.x + errorX * correctionStrength,
            pos.y + errorY * correctionStrength,
            localPlayer.body.getAngle()
        );
    }

    private void handlePlayerInterpolation() {
        if (latestSnapshot == null || previousSnapshot == null) return;
        if (latestSnapshot.serverTime <= previousSnapshot.serverTime) return;

        float renderTime = localTime - INTERPOLATION_DELAY;
        float span = latestSnapshot.serverTime - previousSnapshot.serverTime;

        if (span <= 0f) return;

        float t = (renderTime - previousSnapshot.serverTime) / span;
        t = MathUtils.clamp(t, 0f, 1f);

        for (PlayerState ps : simulation.getPlayers().values()) {
            if (ps.id == localPlayer.id) continue;

            PlayerSnapshot p0 = previousSnapshot.players.get(ps.id);
            PlayerSnapshot p1 = latestSnapshot.players.get(ps.id);

            if (p0 == null || p1 == null) continue;

            ps.x = MathUtils.lerp(p0.x, p1.x, t);
            ps.y = MathUtils.lerp(p0.y, p1.y, t);
        }
    }

    private void updateCoordinateLabel() {
        coordinatesLabel.setText(String.format("x: %.1f, y: %.1f", localPlayer.x, localPlayer.y));
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("Time: %.1f", localTime));
    }

    private void draw() {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0.2f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();
        batch.draw(background, PIXELS_PER_METER/2, PIXELS_PER_METER/2, (game.worldWidth-1) * PIXELS_PER_METER, (game.worldHeight-1) * PIXELS_PER_METER);
        for (int i = 0; i < wallGrid.length; i++) {
            for (int j = 0; j < wallGrid[0].length; j++) {
                int w = wallGrid[i][j];
                if (w == 1) {
                    batch.draw(wallTexture, PIXELS_PER_METER * j, PIXELS_PER_METER * i, PIXELS_PER_METER, PIXELS_PER_METER);
                    continue;
                }
                if (w == 0) {
                    continue;
                }
                if (w == 2) {
                    batch.draw(spawnTexture, PIXELS_PER_METER * j, PIXELS_PER_METER * i, PIXELS_PER_METER, PIXELS_PER_METER);
                }
            }
        }
        for (PlayerState p : simulation.getPlayers().values()) {
            batch.draw(playerSkins.get(p.id), Constants.metersToPx(p.x) - PLAYER_RADIUS_PX, Constants.metersToPx(p.y) - PLAYER_RADIUS_PX, PLAYER_RADIUS_PX*2, PLAYER_RADIUS_PX*2);
        }
        for (ProjectileState p : simulation.getProjectiles().values()) {
            batch.draw(ballSkins.get(p.ownerId), Constants.metersToPx(p.x) - BULLET_RADIUS_PX, Constants.metersToPx(p.y) - BULLET_RADIUS_PX, BULLET_RADIUS_PX*2, BULLET_RADIUS_PX*2);
        }
        batch.end();
        //drawGrid();
    }

    private void drawGrid() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLUE);
        for (int i = -20; i <= 20; i++) {
            float p = Constants.metersToPx(i);
            shapeRenderer.line(p, -1000, p, 1000); // vertical
            shapeRenderer.line(-1000, p, 1000, p); // horizontal
        }
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        worldViewPort.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        background.dispose();
    }

    private void handleWorldSnapshot(WorldSnapshotMessage msg) {
        previousSnapshot = latestSnapshot;
        latestSnapshot = msg;
        handleProjectiles();
    }

    public void addChatMessage(ChatMessage msg) {
        chatLog.setText(chatLog.getText() + "\n" + msg.message);
        Gdx.app.postRunnable(() -> {
            chatScroll.layout();
            chatScroll.setScrollPercentY(1f);
        });
    }

    @Override
    public void handleNetworkMessage(NetMessage msg) {
        if (msg instanceof WorldSnapshotMessage) {
            handleWorldSnapshot((WorldSnapshotMessage) msg);
        } else if (msg instanceof ChatMessage) {
            addChatMessage((ChatMessage) msg);
        } else {
            System.out.println("Unknown message type... " + msg.getClass());
        }
    }
}
