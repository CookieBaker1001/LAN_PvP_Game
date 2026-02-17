package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.util.Constants;
import com.springer.knakobrak.world.client.ClientGameState;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.Wall;

public class LoadingScreen implements Screen {

    private final LanPvpGame game;
    private Texture background;
    private ClientGameState gameState;

    private boolean initDone;
    private boolean gameStart;

    public LoadingScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("loadingBG.png");
        this.gameState = game.gameState;

        initDone = false;
        gameStart = false;
    }


    @Override
    public void show() {

    }

    @Override
    public void render(float v) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();

        game.batch.draw(background, 0, 0, worldWidth, worldHeight);
        game.batch.end();

        game.client.poll(this::handleMessage);
//        stage.act(delta);
//        stage.draw();

        if (initDone && gameStart) {

            gameState.localPlayer =
                gameState.players.get(
                    gameState.localPlayerId
                );

            game.setScreen(new GameScreen(game));
        }
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("INIT_PLAYER")) {
            System.out.println("INIT_PLAYER");
            receivePlayerData(msg);
        } else if (msg.startsWith("INIT_MAP ")) {
            System.out.println("INIT_MAP");
            String[] parts = msg.split(" ");
            System.out.println("Dimensions: " + parts[1] + "x" + parts[2]);
        } else if (msg.startsWith("INIT_MAP_WALLS")) {
            System.out.println("INIT_MAP_WALLS");
            receiveWalls(msg);
        } else if (msg.equals("INIT_DONE")) {
            System.out.println("INIT_DONE");
            initDone = true;
            game.client.send("READY");
        } else if (msg.equals("START_GAME")) {
            System.out.println("START_GAME");
            gameStart = true;
        }
    }

    private void receivePlayerData(String msg) {
        String[] parts = msg.split(" ");
        int id = Integer.parseInt(parts[1]);
        float x = Float.parseFloat(parts[2]);
        float y = Float.parseFloat(parts[3]);
        PlayerState newPlayer = new PlayerState();
        newPlayer.id = id;
        newPlayer.x = x;
        newPlayer.y = y;
        gameState.players.put(id, newPlayer);
    }

    private void receiveWalls(String msg) {
        String[] parts = msg.split(" ");
        System.out.println("Received walls: " + msg);
        game.walls.clear();
        for (int i = 1; i < parts.length; i += 4) {
            float x = Constants.metersToPx(Float.parseFloat(parts[i]));
            float y = Constants.metersToPx(Float.parseFloat(parts[i + 1]));
            float width = Constants.metersToPx(Float.parseFloat(parts[i + 2]));
            float height = Constants.metersToPx(Float.parseFloat(parts[i + 3]));
            Wall wall = new Wall();
            wall.x = x;
            wall.y = y;
            wall.width = width;
            wall.height = height;
            game.walls.add(wall);
            System.out.println("Data: " + x + ", " + y + ", " + width + ", " + height);
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
        background.dispose();
        //stage.dispose();
    }
}
