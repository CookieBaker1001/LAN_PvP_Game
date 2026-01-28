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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.world.PlayerState;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private final LanPvpGame game;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture background;

    Map<Integer, PlayerState> players = new HashMap<>();
    ShapeRenderer shapeRenderer = new ShapeRenderer();

    private Stage stage;


    int movementSpeed = 200;
    float positionX = 100;
    float positionY = 100;

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

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label label = new Label("GAME STARTED!", game.uiSkin);
        label.setFontScale(2f);
        table.add(label);
    }

    @Override
    public void render(float delta) {
        // 1. Poll input (WASD + mouse)
        // 2. Send InputMessage to server
        // 3. Receive WorldStateMessage
        // 4. Draw circles

        handleInput(delta);

        game.client.poll(this::handleMessage);

        moveCameraToPlayer();

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
        for (PlayerState p : players.values()) {
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(p.x, p.y, 20);
        }
        shapeRenderer.end();

        stage.act(delta);
        stage.draw();
    }

    private void handleInput(float delta) {
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;

        if (dx != 0 || dy != 0) {
            game.client.send("MOVE " + dx + " " + dy + " " + (movementSpeed * delta));
        }
    }

    private void moveCameraToPlayer() {
//        game.viewport.getCamera().position.set(positionX, positionY, 0);
//        game.viewport.getCamera().update();

        PlayerState me = players.get(game.clientId);
        if (me != null) {
            camera.position.set(me.x, me.y, 0);
            camera.update();
        }
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
//        if (msg.startsWith("MOVE")) {
//            String[] p = msg.split(" ");
//            float dx = Float.parseFloat(p[1]);
//            float dy = Float.parseFloat(p[2]);
//
//            state.x += dx;
//            state.y += dy;
//
//            broadcastGameState();
//        }
    }

    private void updateGameState(String msg) {
        String[] parts = msg.split(" ");
        for (int i = 1; i < parts.length; i++) {
            String[] data = parts[i].split(":");
            int id = Integer.parseInt(data[0]);
            float x = Float.parseFloat(data[1]);
            float y = Float.parseFloat(data[2]);
            PlayerState p = players.get(id);
            if (p == null) {
                p = new PlayerState();
                p.id = id;
                players.put(id, p);
            }
            if (id == game.clientId) {
                positionX = x;
                positionY = y;
            }
            p.x = x;
            p.y = y;
        }
//        players.computeIfAbsent(id, k -> {
//            GameServer.PlayerState p = new GameServer.PlayerState();
//            p.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
//            return p;
//        });
    }
}
