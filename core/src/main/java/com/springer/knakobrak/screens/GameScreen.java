package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private final LanPvpGame game;

    private Texture background;
    private Stage stage;

    Map<Integer, Player> players = new HashMap<>();
    ShapeRenderer shapeRenderer = new ShapeRenderer();

    int movementSpeed = 200;

    public GameScreen(LanPvpGame game) {
        this.game = game;
        background = new Texture("grass_bg.png");
    }

    @Override
    public void show() {
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

        float speed = movementSpeed * delta;
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed;

        if (dx != 0 || dy != 0) {
            game.client.send("MOVE " + dx + " " + dy);
        }

        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();
        game.batch.draw(background, 0, 0, worldWidth, worldHeight);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Player p : players.values()) {
            shapeRenderer.circle(p.x, p.y, 20);
        }
        shapeRenderer.end();

        game.batch.end();

        game.client.poll(this::handleMessage);

        stage.act(delta);
        stage.draw();
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
            Player p = players.get(id);
            if (p == null) {
                p = new Player();
                p.id = id;
                players.put(id, p);
            }
            p.x = x;
            p.y = y;
        }
    }

    static class Player {
        int id;
        float x, y;
    }
}
