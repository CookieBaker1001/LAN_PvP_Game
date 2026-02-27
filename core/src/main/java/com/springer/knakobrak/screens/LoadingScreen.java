package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.NetworkListener;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.Wall;

public class LoadingScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private Stage stage;
    private Texture background;

    private boolean initDone;
    private boolean gameStart;

    private Viewport worldViewPort;

    public LoadingScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("loadingBG.png");
        initDone = false;
        gameStart = false;
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
    }

    private boolean sentReady = false;
    @Override
    public void render(float v) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        game.batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        game.batch.begin();

        game.batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        game.batch.end();

        game.dispatchNetworkMessages();
//        stage.act(delta);
//        stage.draw();

        //System.out.println("LS: " + playersDataReceived + ", " + worldDataReceived + ", " + (!gameStart) + ", " + (!sentReady));
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

    private boolean playersDataReceived = false;
    private void receivePlayerData(InitPlayersMessage msg) {
        System.out.println("11111");
        for (PlayerStateDTO p : msg.players) {
            System.out.println("22222");
            PlayerState ps = new PlayerState();
            ps.id = p.id;
            ps.name = p.name;
            ps.playerIcon = p.playerIcon;
            ps.ballIcon = p.ballIcon;
            ps.x = p.x;
            ps.y = p.y;
            ps.body = LoadUtillities.createPlayerBody(game.simulation.getWorld(),  p.x, p.y, p.id);
            game.simulation.addPlayer(ps);
            //game.simulation.players.put(ps.id, ps);
            if (ps.id == game.playerId){
                game.localPlayer = ps;
            }
            System.out.println("Added player with id " + ps.id + ", and skin " + ps.playerIcon);
        }
        playersDataReceived = true;
    }

    private boolean worldDataReceived = false;
    private void receiveWorldData(InitWorldMessage msg) {
        game.simulation.setPlayerSpawnPoints(msg.spawnPoints);
        for (WallDTO wDTO : msg.walls) {
            Wall w = new Wall();
            w.x = wDTO.x;
            w.y = wDTO.y;
            w.width = wDTO.width;
            w.height = wDTO.height;
            w.body = LoadUtillities.createWall(game.simulation.getWorld(), w.x, w.y, (int)w.height, (int)w.width);
            game.simulation.addWall(w);
        }
        worldDataReceived = true;
        System.out.println("Received wall bits!");
        game.simulation.setWallGrid(msg.wallBits);
        for (int[] row : game.simulation.getWallGrid()) {
            for (int w : row) {
                System.out.print(w);
            }
            System.out.println();
        }
        game.worldHeight = game.simulation.getWallGrid().length;
        game.worldWidth = game.simulation.getWallGrid()[0].length;
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
        background.dispose();
        stage.dispose();
    }

    @Override
    public void handleNetworkMessage(NetMessage msg) {
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
    }
}
