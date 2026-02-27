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

    private Viewport worldViewPort;

    private ShapeRenderer shapeRenderer = new ShapeRenderer();

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

    static final float INTERPOLATION_DELAY = 0.15f; // 100 ms

    private float localTime;

    Map<Integer, PredictedProjectile> predictedProjectiles = new HashMap<>();


    public GameScreen(LanPvpGame game) {
        this.game = game;
        this.batch = game.batch;
        this.simulation = game.simulation;
        this.localPlayer = game.localPlayer;
        this.localTime = 0f;
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);


        //camera = new OrthographicCamera();
        //camera.setToOrtho(false, game.viewport.getScreenWidth(), game.viewport.getScreenHeight());

        shapeRenderer = new ShapeRenderer();
        background = new Texture("grass_bg.png");

        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        rootTable.top().left().pad(10);

        coordinatesLabel = new Label("x: 0.0, y: 0.0", game.uiSkin);
        coordinatesLabel.setFontScale(1.5f);
        rootTable.add(coordinatesLabel).top().left();

        chatLog = new Label("", game.uiSkin);
        chatLog.setWrap(true);

        chatScroll = new ScrollPane(chatLog, game.uiSkin);
        chatScroll.setFadeScrollBars(false);

        chatInput = new TextField("", game.uiSkin);
        chatInput.setVisible(false);

        rootTable.bottom().left().pad(10);
        rootTable.add(chatScroll).width(400).height(150).row();
        rootTable.add(chatInput).width(400).height(30);

        int i = 0;
        for (PlayerState ps : simulation.players.values()) {
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

//        if (!latestSnapshot.projectiles.isEmpty()) {
//            System.out.println("Proj: " + latestSnapshot.projectiles.get(0).x);
//        }

        for (ProjectileSnapshot pss : latestSnapshot.projectiles) {
            if (pss.ownerId == localPlayer.id) {

                PredictedProjectile predicted = predictedProjectiles.remove(pss.fireSequence);

                if (predicted != null) {
                    destroy(predicted.projectile);

                    ProjectileState proj = spawnProjectileFromSnapshot(pss);
                    simulation.projectiles.put(pss.id, proj);
                    continue;
                }
            }

            if (!simulation.projectiles.containsKey(pss.id)) {
                simulation.projectiles.put(pss.id, spawnProjectileFromSnapshot(pss));
            }
        }

//        for (ProjectileSnapshot ps : latestSnapshot.projectiles) {
//
//            ProjectileState local = simulation.projectiles.get(ps.id);
//            //System.out.println("ps.id: " + ps.id);
//
//            if (local == null) {
//                local = spawnProjectile(simulation.getPlayer(ps.ownerId), ps.vx, ps.vy);
//
////                simulation.projectiles.put(p.id, p);
////                SpawnProjectileMessage spm = new SpawnProjectileMessage();
////                spm.ownerId = localPlayer.id;
////                spm.fireSequence = nextProjectileId++;
////                spm.dx = p.body.getLinearVelocity().x;
////                spm.dy = p.body.getLinearVelocity().y;
////                game.client.send(spm);
//
//                simulation.projectiles.put(ps.id, local);
//            }
//
//            // Soft-correct position
//            Vector2 pos = local.body.getPosition();
//            float lerp = 0.25f;
//
//            local.body.setTransform(
//                MathUtils.lerp(pos.x, ps.x, lerp),
//                MathUtils.lerp(pos.y, ps.y, lerp),
//                local.body.getAngle()
//            );
//        }
    }

    private void destroy(ProjectileState p) {
        if (p == null) return;

        System.out.println("Destroyed");
        // 1. Remove Box2D body
        if (p.body != null) {
            simulation.world.destroyBody(p.body);
            p.body = null;
        }

        // 2. Remove from simulation collections
        predictedProjectiles.remove(p.id);
        //simulation.projectiles.remove(p.id);

        // 3. (Optional) clear other references
        // e.g. predictedProjectiles map is handled elsewhere
    }

    private ProjectileState spawnProjectileFromSnapshot(ProjectileSnapshot ps) {
        System.out.println("Spawning official bullet!");
        ProjectileState proj = new ProjectileState();
        //System.out.println("Spawn official bullet: x,y: (" + ps.x + "," + ps.y + ")");
        proj.id = ps.id;
        proj.ownerId = ps.ownerId;
        //System.out.println("[2]: Firing sequence: " + ps.fireSequence);
        Vector2 dir = new Vector2(ps.vx, ps.vy).nor();
        Vector2 spawnPos = new Vector2(ps.x, ps.y)
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));
        proj.body = LoadUtillities.createProjectile(
            simulation.world,
            spawnPos.x,
            spawnPos.y,
            proj.id
        );
        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );
        //System.out.println("Spawned authoritative bullet at (" + spawnPos.x + "," + spawnPos.y + ")");

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
        for (ProjectileState ps : simulation.projectiles.values()) {
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
            System.out.println("Bodies in world: " + simulation.world.getBodyCount());
            //System.out.println("[1]: Firing sequence: " + pp.fireSequence);
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
            simulation.world,
            spawnPos.x,
            spawnPos.y,
            p.id
        );

        p.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );

        //System.out.println("Spawned local bullet at (" + spawnPos.x + "," + spawnPos.y + ")");

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
            simulation.world,
            spawnPos.x,
            spawnPos.y,
            proj.id
        );

        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );

        System.out.println("Spawned bullet at (" + spawnPos.x + "," + spawnPos.y + ")");

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
            //camera.position.set(x, y, 0);
            //camera.update();
            OrthographicCamera cam = (OrthographicCamera) worldViewPort.getCamera();

            cam.position.set(x, y, 0);
        }
    }

    private void localLogic() {
        moveCameraToPlayer();
        updateCoordinateLabel();
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

        for (PlayerState ps : simulation.players.values()) {
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

    float xPx, yPx, sizePx, half;
    private void draw() {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();
        batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        for (PlayerState p : simulation.players.values()) {
            batch.draw(playerSkins.get(p.id), Constants.metersToPx(p.x) - PLAYER_RADIUS_PX, Constants.metersToPx(p.y) - PLAYER_RADIUS_PX, PLAYER_RADIUS_PX*2, PLAYER_RADIUS_PX*2);
        }
        for (ProjectileState p : simulation.projectiles.values()) {
//            float x = Constants.metersToPx(p.x) - BULLET_RADIUS_PX;
//            float y = Constants.metersToPx(p.y) - BULLET_RADIUS_PX;
//            System.out.println("x,y: (" + x + "," + y + ")");
//            batch.draw(ballSkins.get(p.ownerId), x, y, BULLET_RADIUS_PX*2, BULLET_RADIUS_PX*2);
            batch.draw(ballSkins.get(p.ownerId), Constants.metersToPx(p.x) - BULLET_RADIUS_PX, Constants.metersToPx(p.y) - BULLET_RADIUS_PX, BULLET_RADIUS_PX*2, BULLET_RADIUS_PX*2);
        }
        batch.end();

        shapeRenderer.setProjectionMatrix(worldViewPort.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BROWN);
        for (Wall w : simulation.walls) {
            float wx = Constants.metersToPx(w.x);
            float wy = Constants.metersToPx(w.y);
            float ww = Constants.metersToPx(w.width);
            float wh = Constants.metersToPx(w.height);
            shapeRenderer.rect(
                wx - ww / 2f,
                wy - wh / 2f,
                ww,
                wh
            );
        }
//        for (PlayerState p : simulation.players.values()) {
//            shapeRenderer.setColor(p.color);
//            shapeRenderer.circle(Constants.metersToPx(p.x), Constants.metersToPx(p.y), PLAYER_RADIUS_PX);
//        }
//        shapeRenderer.setColor(Color.YELLOW);
//        for (ProjectileState p : simulation.projectiles.values()) {
//            shapeRenderer.circle(Constants.metersToPx(p.x), Constants.metersToPx(p.y), BULLET_RADIUS_PX);
//        }
        shapeRenderer.end();
        drawGrid();
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
        //game.viewport.update(width, height);
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
