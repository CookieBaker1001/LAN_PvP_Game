package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.net.NetworkListener;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.PhysicsSimulation;

public class LobbyScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private Stage stage;
    private Texture background;

    private boolean isHost;

    private List<String> playerListUI;
    private ScrollPane playerScrollPane;
    private Table rootTable;

    private PhysicsSimulation simulation;

    private Viewport worldViewPort;

    public LobbyScreen(LanPvpGame game, boolean isHost) {
        this.game = game;
        this.simulation = game.simulation;
        this.isHost = isHost;
        this.background = new Texture("final_frontier.jpg");
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        playerListUI = new List<>(game.uiSkin);
        playerScrollPane = new ScrollPane(playerListUI, game.uiSkin);

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        rootTable.pad(20);

        Label title = new Label("Lobby", game.uiSkin);
        title.setFontScale(1.5f);

        rootTable.add(title).colspan(2).padBottom(20);
        rootTable.row();

        rootTable.add(new Label("Players:", game.uiSkin)).left();
        rootTable.add(playerScrollPane)
            .width(300)
            .height(200)
            .left();
        rootTable.row();

        TextButton leaveButton = new TextButton("Leave game", game.uiSkin);
        rootTable.add(leaveButton).pad(20);
        leaveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!game.isHost) game.client.requestDisconnect();
                else game.client.requestShutdown();
//                game.client.disconnect(game.playerId);
//                if (isHost) {
//                    game.hostedServer.shutdown();
//                    try {
//                        game.serverThread.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                game.setScreen(new MainMenuScreen(game));
            }
        });

        if (isHost) {
            TextButton playButton = new TextButton("Start game", game.uiSkin);
            rootTable.add(playButton).padTop(20);

            playButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    startGame();
                }
            });
        }
        stage.addActor(rootTable);
    }

    @Override
    public void render(float delta) {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        game.batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        game.batch.begin();

        game.batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        game.batch.end();

        game.dispatchNetworkMessages();
        stage.act(delta);
        stage.draw();
    }

    private void startGame() {
        System.out.println("Host starting game...");
        StartGameMessage msg = new StartGameMessage();
        game.client.send(msg);
    }

    @Override public void resize(int width, int height) {
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

    private void updatePlayerList(LobbyStateMessage msg) {
        Array<String> names = new Array<>();
        for (PlayerStateDTO p : msg.players) {
            String entry = "";
            if (p.id == game.playerId) entry += "(You) ";
            entry += p.name;
            if (p.id == msg.hostId) entry += " (HOST)";
            names.add(entry);
        }
        playerListUI.setItems(names);
    }

    @Override
    public void handleNetworkMessage(NetMessage msg) {
        if (msg instanceof JoinAcceptMessage) {
            JoinAcceptMessage jam = (JoinAcceptMessage) msg;
            game.playerId = jam.clientId;
            game.isHost = jam.isHost;
            System.out.println("Assigned ID: " + game.playerId + ((jam.isHost) ? "(Host)" : ""));
        } else if (msg instanceof LobbyStateMessage) {
            LobbyStateMessage lobbyMsg = (LobbyStateMessage) msg;
            updatePlayerList(lobbyMsg);
        } else if (msg instanceof EnterLoadingMessage) {
            simulation.initPhysics();
            game.setScreen(new LoadingScreen(game));
        } else if (msg instanceof DisconnectMessage) {
            game.client.shutdown();
            game.setScreen(new MainMenuScreen(game));
        }
    }
}
