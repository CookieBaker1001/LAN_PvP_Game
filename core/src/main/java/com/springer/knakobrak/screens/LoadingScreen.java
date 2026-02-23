package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.Wall;

public class LoadingScreen implements Screen {

    private final LanPvpGame game;
    private Texture background;
    //private ClientGameState gameState;

    private boolean initDone;
    private boolean gameStart;

    public LoadingScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("loadingBG.png");
        //this.gameState = game.gameState;

        initDone = false;
        gameStart = false;
    }


    @Override
    public void show() {

    }

    private boolean sentReady = false;
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

        System.out.println("LoadingScreen: " + playersDataReceived + ", " + worldDataReceived + ", " + (!gameStart) + ", " + (!sentReady));
        if (playersDataReceived && worldDataReceived && !gameStart && !sentReady) {
            ReadyMessage rm = new ReadyMessage();
            rm.ready = true;
            sentReady = true;
            game.client.send(rm);
        }

        if (initDone && gameStart) {
            game.localPlayer =
                game.simulation.getPlayer(
                    game.playerId
                );
            game.setScreen(new GameScreen(game));
        }
    }

    private void handleMessage(NetMessage msg) {

        System.out.println("Received message: " + msg.getClass().getSimpleName());
        if (msg instanceof InitPlayersMessage) {
            System.out.println("[C]: INIT_PLAYER");
            receivePlayerData((InitPlayersMessage) msg);
        } else if (msg instanceof InitWorldMessage) {
            System.out.println("[C]: INIT_WORLD");
            receiveWorldData((InitWorldMessage) msg);
        } else if (msg instanceof LoadingCompleteMessage) {
            System.out.println("[C]: INIT_COMPLETE");
            initDone = true;
        } else if (msg instanceof StartSimulationMessage) {
            System.out.println("[C]: START_SIMULATION");
            gameStart = true;
        }

//        if (msg.startsWith("INIT_PLAYER")) {
//            //System.out.println("INIT_PLAYER");
//            receivePlayerData(msg);
//        } else if (msg.startsWith("INIT_MAP ")) {
//            //System.out.println("INIT_MAP");
//            String[] parts = msg.split(" ");
//            //System.out.println("Dimensions: " + parts[1] + "x" + parts[2]);
//        } else if (msg.startsWith("INIT_MAP_WALLS")) {
//            //System.out.println("INIT_MAP_WALLS");
//            receiveWalls(msg);
//        } else if (msg.equals("INIT_DONE")) {
//            //System.out.println("INIT_DONE");
//            initDone = true;
//            try {
//                ReadyMessage readyMessage = new ReadyMessage();
//                readyMessage.ready = true;
//                game.client.send(readyMessage);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            //game.client.send("READY");
//        } else if (msg.equals("START_GAME")) {
//            //System.out.println("START_GAME");
//            gameStart = true;
//        }
    }

    private boolean playersDataReceived = false;
    private void receivePlayerData(InitPlayersMessage msg) {
        for (PlayerStateDTO p : msg.players) {
            PlayerState ps = new PlayerState();
            ps.id = p.id;
            ps.name = p.name;
            ps.x = p.x;
            ps.y = p.y;
            ps.color = new Color(p.r, p.g, p.b, 1.0f);
            ps.body = LoadUtillities.createPlayerBody(game.simulation.world,  p.x, p.y, p.id);
            game.simulation.players.put(ps.id, ps);
            if (ps.id == game.playerId){
                game.localPlayer = ps;
            }
            System.out.println("Added player with id " + ps.id);
        }
        System.out.println("set to true!!!!");
        playersDataReceived = true;
    }

    private boolean worldDataReceived = false;
    private void receiveWorldData(InitWorldMessage msg) {
        game.simulation.clearWalls();
        game.simulation.playerSpawnPoints = msg.spawnPoints;
        for (WallDTO wDTO : msg.walls) {
            Wall w = new Wall();
            w.x = wDTO.x;
            w.y = wDTO.y;
            w.width = wDTO.width;
            w.height = wDTO.height;
            w.body = LoadUtillities.createWall(game.simulation.world, w.x, w.y, (int)w.height, (int)w.width);
            game.simulation.addWall(w);
        }
        worldDataReceived = true;
    }

    private void receiveWalls(String msg) {
        //System.out.println("Walls: " + msg);
        String[] parts = msg.split(" ");
        game.simulation.clearWalls();
        //gameState.walls.clear();
        for (int i = 1; i < parts.length; i += 4) {
            float x = Float.parseFloat(parts[i]);
            float y = Float.parseFloat(parts[i + 1]);
            float width = Float.parseFloat(parts[i + 2]);
            float height = Float.parseFloat(parts[i + 3]);
            Wall wall = new Wall();
            wall.x = x;
            wall.y = y;
            wall.width = width;
            wall.height = height;
            wall.body = LoadUtillities.createWall(game.simulation.world, x, y, (int)height, (int)width);
            game.simulation.addWall(wall);
            //System.out.println("Data: " + x + ", " + y + ", " + width + ", " + height);
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
