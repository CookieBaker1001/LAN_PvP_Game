package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.local.SoundManager;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.Player;

public class LoadingScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private Stage stage;
    private Texture background;

    private Viewport worldViewPort;

    private PhysicsSimulation simulation;

    public LoadingScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        System.out.println("[LoadingScreen constructor]");
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;

        background = new Texture("misc/loadingBG.png");
        game.startGameWorld(true);
        simulation = game.simulation;

        System.out.println("ID: " + game.client.id);
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(worldViewPort);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        game.dispatchNetworkMessages();
        input(delta);
        logic(delta);
        draw(delta);
    }

    private void input(float delta) {

    }

    private float mandatoryWaitTimer = 0f;
    private final float mandatoryWaitTimerMax = 3f;
    private boolean mandatoryWaitTimeDone = false;

    private float requestResourceWaitTimer = 1f;
    private final float requestResourceWaitTimer_Max = 1f;

    private float sendAccumulator = 1f;
    private final float sendFrequency = 1f;
    private boolean canSend = true;
    private void logic(float delta) {
        if (!mandatoryWaitTimeDone) {
            mandatoryWaitTimer += delta;
            if (mandatoryWaitTimer >= mandatoryWaitTimerMax) {
                mandatoryWaitTimeDone = true;
            }
        }

        sendAccumulator += delta;
        if (!canSend && sendAccumulator >= sendFrequency) {
            sendAccumulator = 0f;
            canSend = true;
        }

        requestResourceWaitTimer += delta;
        if (canSend && requestResourceWaitTimer >= requestResourceWaitTimer_Max) {
            requestResourceWaitTimer = 0f;
            requestResources();
            canSend = false;
        }

        if (canSend && mandatoryWaitTimeDone) {
            if (receivedPlayerData && receivedWorldData) {
                System.out.println("Everything loaded. Trying to switch to game screen!");
                game.client.send(new AllResourcesLoadedMessage());
                canSend = false;
            }
        }
        //System.out.println("canSend: " + canSend + ", accumulator: " + sendAccumulator);
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();
        batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        batch.end();

//        stage.act(delta);
//        stage.draw();
    }

    private boolean receivedPlayerData = false;
    private boolean receivedWorldData = true;
    private void requestResources() {
        if (!receivedPlayerData) {
            game.client.send(new GetPlayerDataMessage());
        }
        if (!receivedWorldData) {
            game.client.send(new GetMapDataMessage());
        }
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

        switch (msg) {
            case PlayerStateMessage psm -> handlePlayerStateMessage(psm);
            //case MapDataMessage wdm -> handleMapDataMessage(wdm);
            case AllResourcesLoadedAcknowledgedMessage arlam -> handleAllResourcesLoadedAcknowledgedMessage(arlam);
            default -> {
                //System.out.println("Unknown message format: " + msg.getClass());
            }
        }
    }

    private void handlePlayerStateMessage(PlayerStateMessage psm) {
        for (int i = 0; i < psm.x.length; i++) {
            Player p = simulation.getPlayer(i);
            if (p == null) {
                p = new Player();
                simulation.addPlayer(i, p);
            }
            p.x = psm.x[i];
            p.y = psm.y[i];
        }
        receivedPlayerData = true;
    }

//    private void handleMapDataMessage(MapDataMessage wdm) {
//        simulation.playerSpawnPoints = wdm.spawnPoints;
//        for (WallDTO wDTO : wdm.walls) {
//            Wall w = new Wall();
//            w.x = wDTO.x;
//            w.y = wDTO.y;
//            w.width = wDTO.width;
//            w.height = wDTO.height;
//            w.body = LoadUtillities.createWall(simulation.world, w.x, w.y, (int)w.height, (int)w.width);
//            game.simulation.addWall(w);
//        }
//        System.out.println("Received wall bits!");
//        simulation.wallGrid = wdm.wallBits;
//        for (int[] row : simulation.wallGrid) {
//            for (int w : row) {
//                System.out.print(w);
//            }
//            System.out.println();
//        }
//        game.worldHeight = simulation.wallGrid.length;
//        game.worldWidth = simulation.wallGrid[0].length;
//
//        receivedWorldData = true;
//    }

    private void handleAllResourcesLoadedAcknowledgedMessage(AllResourcesLoadedAcknowledgedMessage arlam) {
        game.setScreen(new GameScreen(game, batch, uiSkin, soundManager));
    }
}
