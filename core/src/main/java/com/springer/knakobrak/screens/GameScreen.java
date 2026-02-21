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
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.*;
import com.springer.knakobrak.util.Constants;

import static com.springer.knakobrak.util.Constants.*;

import java.util.*;

public class GameScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture background;

    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private Label coordinatesLabel;
    private List<String> consoleMessages;
    private Label chatLog;
    private ScrollPane chatScroll;
    private TextField chatInput;
    private Table rootTable;

    private boolean chatMode = false;

    private PhysicsSimulation simulation;
    private PlayerState localPlayer;

    int nextInputId = 0;
    Queue<PlayerInputMessage> pendingInputs = new ArrayDeque<>();

    Deque<WorldSnapshotMessage> snapshotBuffer = new ArrayDeque<>();
    static final float INTERPOLATION_DELAY = 0.1f; // 100 ms

    private float localTime;


    public GameScreen(LanPvpGame game) {
        this.game = game;
        this.batch = game.batch;
        this.simulation = game.simulation;
        this.localPlayer = game.localPlayer;
        this.localTime = 0f;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.viewport.getScreenWidth(), game.viewport.getScreenHeight());

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

//        consoleMessages = new List<>(game.uiSkin);
//        chatScroll = new ScrollPane(consoleMessages, game.uiSkin);
//
//        rootTable.add(chatScroll)
//            .width(150)
//            .height(100)
//            .left()
//            .bottom();
//        rootTable.row();
    }

    float physicsAccumulator = 0f;
    float FIXED_DT = 1 / 60f;
    @Override
    public void render(float delta) {
        game.client.poll(this::handleMessage); // Check for incoming data to be put into the queue
        localTime += delta; // age localtime
        input(delta); // detect local input, move, and tell the server

        physicsAccumulator += delta;
        while (physicsAccumulator >= FIXED_DT) {
            simulation.step(FIXED_DT);
            physicsAccumulator -= FIXED_DT;
        }

        syncBody(); // Write the local players Box2D coordinates to the pixel coordinates

        logic(delta); // handle world snapshots
        draw(); // render everything

        stage.act(delta); // UI
        stage.draw(); // UI
    }

    private void syncBody() {
//        for (PlayerState p : simulation.players.values()) {
//            Vector2 pos = p.body.getPosition();
//            p.x = pos.x;
//            p.y = pos.y;
//        }

        Vector2 pos = localPlayer.body.getPosition();
        localPlayer.x = pos.x;
        localPlayer.y = pos.y;

//        for (ProjectileState p : simulation.projectiles.values()) {
//            Vector2 pos = p.body.getPosition();
//            p.x = pos.x;
//            p.y = pos.y;
//        }
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
            chatMessage.message = "<" + game.username + ">" + msg;
            game.client.send(chatMessage);


            //game.client.send("MSG <" + game.username + ">" + msg);
            //sendMessageToServer(msg);

            // Optional: show locally
            //addChatMessage("You: " + msg);
        }

        chatInput.setText("");
        chatInput.setVisible(false);
        stage.unfocusAll();
    }

    private void input(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            chatMode = !chatMode;
            if (chatMode) {
                enterChatMode();
            }
            else {
                exitChatMode();
            }
        }

        float dx = 0, dy = 0;

        if (!chatMode) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        }

        PlayerInputMessage pim = new PlayerInputMessage();
        pim.playerId = localPlayer.id;
        pim.sequence = nextInputId++;
        pim.dx = dx;
        pim.dy = dy;
        //input.dt = delta;

        pendingInputs.add(pim);

        applyMovement(pim);

        game.client.send(pim);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 mouse = new Vector3(
                Gdx.input.getX(),
                Gdx.input.getY(),
                0
            );

            camera.unproject(mouse);

            float mouseDx = mouse.x - Constants.metersToPx(localPlayer.x);
            float mouseDy = mouse.y - Constants.metersToPx(localPlayer.y);

            float len = (float)Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
            if (len == 0) return;

            mouseDx /= len;
            mouseDy /= len;

            applyFire(mouseDx, mouseDy);
        }
    }

    private int nextProjectileId = 1;
    private void applyFire(float dx, float dy) {
        ProjectileState proj = new ProjectileState();
        proj.id = nextProjectileId++;
        proj.ownerId = localPlayer.id;
        Vector2 dir = new Vector2(dx, dy).nor();

        Vector2 spawnPos = localPlayer.body.getPosition()
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

        simulation.projectiles.put(proj.id, proj);
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
            camera.position.set(x, y, 0);
            camera.update();
        }
    }

    private void logic(float delta) {
        handleWorldSnapshotBuffer(delta);
        moveCameraToPlayer();
        updateCoordinateLabel();
    }

    private void handleWorldSnapshotBuffer(float delta) {
        float renderTime = localTime - INTERPOLATION_DELAY;

        WorldSnapshotMessage older = null;
        WorldSnapshotMessage newer = null;

        for (WorldSnapshotMessage snap : snapshotBuffer) {
            if (snap.serverTime <= renderTime) {
                older = snap;
            } else {
                newer = snap;
                break;
            }
        }
        if (older == null || newer == null) return;

        float alpha;
        for (PlayerState ps : simulation.players.values()) {
            if (ps == localPlayer) continue;

            alpha =
                (renderTime - older.serverTime) /
                    (newer.serverTime - older.serverTime);

            alpha = MathUtils.clamp(alpha, 0f, 1f);

            PlayerSnapshot p0 = findPlayer(older, ps.id);
            PlayerSnapshot p1 = findPlayer(newer, ps.id);

            if (p0 == null || p1 == null) continue;

            float t = (localTime - p0.time) / (p1.time - p0.time);
            t = MathUtils.clamp(t, 0f, 1f);

            ps.x = MathUtils.lerp(p0.x, p1.x, t);
            ps.y = MathUtils.lerp(p0.y, p1.y, t);

            System.out.println(localTime);
        }
    }

    private PlayerSnapshot findPlayer(WorldSnapshotMessage snap, int id) {
        for (PlayerSnapshot p : snap.players) {
            if (p.id == id) return p;
        }
        return null;
    }

    private void updateCoordinateLabel() {
        coordinatesLabel.setText(String.format("x: %.1f, y: %.1f", localPlayer.x, localPlayer.y));
    }

    float xPx, yPx, sizePx, half;
    private void draw() {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        game.worldWidth = game.viewport.getWorldWidth();
        game.worldHeight = game.viewport.getWorldHeight();
        batch.draw(background, 0, 0, game.worldWidth, game.worldHeight);

        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
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
        for (PlayerState p : simulation.players.values()) {
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(Constants.metersToPx(p.x), Constants.metersToPx(p.y), PLAYER_RADIUS_PX);
        }
        shapeRenderer.setColor(Color.YELLOW);
        for (ProjectileState p : simulation.projectiles.values()) {
            shapeRenderer.circle(Constants.metersToPx(p.x), Constants.metersToPx(p.y), BULLET_RADIUS_PX);
        }
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
        game.viewport.update(width, height);
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

    private void handleMessage(NetMessage msg) {
        if (msg instanceof WorldSnapshotMessage) {
            handleWorldSnapshot((WorldSnapshotMessage) msg);
        }
        else {
            System.out.println("Unknown message type... " + msg.getClass());
        }
    }

    private void handleWorldSnapshot(WorldSnapshotMessage msg) {
        snapshotBuffer.addLast(msg);
        // Prevent runaway memory
        while (snapshotBuffer.size() > 20) {
            snapshotBuffer.removeFirst();
        }
    }

    public void addChatMessage(String message) {
        chatLog.setText(chatLog.getText() + "\n" + message);

        // Scroll to bottom
        Gdx.app.postRunnable(() -> {
            chatScroll.layout();
            chatScroll.setScrollPercentY(1f);
        });
    }

    private void onServerSnapshot(PlayerSnapshotMessage psm) {
        if (psm.id != localPlayer.id) return;

        // 1️⃣ Correct position
        Body body = localPlayer.body;
        body.setTransform(psm.position, body.getAngle());
        body.setLinearVelocity(psm.velocity);

        // 2️⃣ Drop acknowledged inputs
        pendingInputs.removeIf(
            input -> input.sequence <= psm.lastProcessedInput
        );

        // 3️⃣ Re-apply remaining inputs
        for (PlayerInputMessage input : pendingInputs) {
            //applyInputToBody(body, input);
        }
    }
}
