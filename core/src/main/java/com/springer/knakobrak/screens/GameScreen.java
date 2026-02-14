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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.util.Constants;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;
import com.springer.knakobrak.world.client.Wall;
import com.springer.knakobrak.world.server.ServerProjectileState;
import com.sun.org.apache.bcel.internal.Const;
import jdk.javadoc.internal.doclets.toolkit.builders.ConstantsSummaryBuilder;

import static com.springer.knakobrak.util.Constants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture background;

    public Map<Integer, PlayerState> players = new HashMap<>();
    ShapeRenderer shapeRenderer = new ShapeRenderer();

    Map<Integer, ProjectileState> projectiles = new HashMap<>();
    Label coordinatesLabel;

    private List<String> consoleMessages;
    private Label chatLog;
    private ScrollPane chatScroll;
    private TextField chatInput;
    private Table rootTable;

    private boolean chatMode = false;

    public GameScreen(LanPvpGame game) {
        this.game = game;
        this.batch = game.batch;
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

    @Override
    public void render(float delta) {
        input(delta);
        game.client.poll(this::handleMessage);
        logic(delta);
        draw();

        stage.act(delta);
        stage.draw();
    }

    private void enterChatMode() {
        chatInput.setVisible(true);
        stage.setKeyboardFocus(chatInput);
    }

    private void exitChatMode() {
        String msg = chatInput.getText();

        if (!msg.isEmpty()) {
            // Send to server
            game.client.send("MSG <" + game.username + ">" + msg);
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

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 mouse = new Vector3(
                Gdx.input.getX(),
                Gdx.input.getY(),
                0
            );

            camera.unproject(mouse);

            PlayerState me = players.get(game.clientId);
            if (me == null) return;

            float mouseDx = mouse.x - me.x;
            float mouseDy = mouse.y - me.y;

            float len = (float)Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
            if (len == 0) return;

            mouseDx /= len;
            mouseDy /= len;

            game.client.send("SHOOT " + mouseDx + " " + mouseDy);
        }

        if (!chatMode && (dx != 0 || dy != 0)) {
            game.client.send("MOVE " + dx + " " + dy);
//            PlayerState me = players.get(game.clientId);
//            System.out.println(me.x + " " + me.y);
        } else {
            game.client.send("STOP");
        }
    }

    private void moveCameraToPlayer() {
        PlayerState me = players.get(game.clientId);
        if (me != null) {
            camera.position.set(me.x, me.y, 0);
            camera.update();
        }
    }

    private void logic(float delta) {
        moveCameraToPlayer();
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
        for (Wall w : game.walls) {

//            xPx = Constants.metersToPx(w.x);
//            yPx = Constants.metersToPx(w.y);
//
//            sizePx = Constants.metersToPx(1f); // 1 meter → pixels
//            half = sizePx / 2f;
//
//            shapeRenderer.rect(
//                xPx - half,   // bottom-left X
//                yPx - half,   // bottom-left Y
//                sizePx,       // width
//                sizePx        // height
//            );

            //System.out.println("Drawing wall at " + w.x + " " + w.y + " size " + w.width + " " + w.height);
            shapeRenderer.setColor(Color.BROWN);
            //shapeRenderer.rect(w.x, w.y, w.width, w.height);

            shapeRenderer.rect(
                w.x - w.width / 2f,
                w.y - w.height / 2f,
                w.width,
                w.height
            );
        }
        for (PlayerState p : players.values()) {
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(p.x, p.y, PLAYER_RADIUS_PX);
        }
        shapeRenderer.setColor(Color.YELLOW);
        for (ProjectileState p : projectiles.values()) {
            shapeRenderer.circle(p.x, p.y, BULLET_RADIUS_PX);
        }
        shapeRenderer.end();

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

    private void handleMessage(String msg) {
        if (msg.startsWith("STATE")) {
            updateGameState(msg);
        }
        else if (msg.startsWith("SHOOT")) {
            String[] parts = msg.split(" ");
            System.out.println("Player " + parts[1] + " fired a shot at angle " + parts[2] + "!");
        }
        else if (msg.startsWith("DAMAGE")) {
            String[] parts = msg.split(" ");
            System.out.println("Player " + parts[1] + " took damage! HP left: " + parts[3]);
        }
        else if (msg.startsWith("MSG")) {
            String[] parts = msg.split(" ");
            addChatMessage(parts[1]);
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

    private void updateGameState(String msg) {
        String[] tokens = msg.split(" ");
        int i = 0;

        if (!tokens[i].equals("STATE")) return;
        i++;

        if (!tokens[i].equals("P")) return;
        i++;

        while (i < tokens.length && !tokens[i].equals("PR")) {
            String[] data = tokens[i].split(":");
            int id = Integer.parseInt(data[0]);
            float x = Constants.metersToPx(Float.parseFloat(data[1]));
            float y = Constants.metersToPx(Float.parseFloat(data[2]));
            float r = Float.parseFloat(data[3]);
            float g = Float.parseFloat(data[4]);
            float b = Float.parseFloat(data[5]);
            PlayerState p = players.get(id);
            if (p == null) {
                p = new PlayerState();
                p.id = id;
                players.put(id, p);
            }
            p.x = x;
            p.y = y;
            p.color.r = r;
            p.color.g = g;
            p.color.b = b;
            i++;

            //System.out.println("game.clientId: " + game.clientId + ", received player id: " + id);
            if (id == game.clientId) {
                //System.out.println("My position: " + x + " " + y);
                coordinatesLabel.setText(String.format("x: %.1f, y: %.1f", Constants.pxToMeters(x), Constants.pxToMeters(y)));
            }
        }

        projectiles.clear();

        if (i < tokens.length && tokens[i].equals("PR")) {
            i++;
            while (i < tokens.length) {
                String[] data = tokens[i].split(":");
                int id = Integer.parseInt(data[0]);
                float x = Constants.metersToPx(Float.parseFloat(data[1]));
                float y = Constants.metersToPx(Float.parseFloat(data[2]));

                ProjectileState p = new ProjectileState();
                p.id = id;
                p.x = x;
                p.y = y;

                projectiles.put(id, p);
                i++;
            }
        }
    }
}
