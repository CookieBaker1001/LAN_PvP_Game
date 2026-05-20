package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.local.SoundManager;
import com.springer.knakobrak.net.multiplayerEntities.ClosedGame_Server;
import com.springer.knakobrak.net.multiplayerEntities.GameClient;
import com.springer.knakobrak.net.multiplayerEntities.OpenGame_Server;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.ServerType;

import java.io.IOException;

public class LobbyScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private Stage stage;
    private Table root;

    private final Texture background;

    private Viewport worldViewPort;

    public LobbyScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        System.out.println("[LobbyScreen constructor]");
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;
        background = new Texture("misc/final_frontier.jpg");

        game.tryToShutDownClient();
        game.tryToShutDownLobbiedServer();
    }


    @Override
    public void show() {

        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(worldViewPort);
        Gdx.input.setInputProcessor(stage);

        LobbyTable();
    }

    private void initServer(boolean open) throws IOException {
        if (open) {
            game.server = new OpenGame_Server(game.port, game.getRandomHostKey());
            game.serverThread = new Thread(game.server, "Open game server");
        } else {
            game.server = new ClosedGame_Server(game.port, game.getRandomHostKey());
            game.serverThread = new Thread(game.server, "Closed game server");
        }
        game.serverThread.start();
        game.isServerRunning = true;
        System.out.println("Created open game server");
    }

    private void initLobbiedServerProcess() throws IOException {
        initServer(false);
        initClient();
        join();
    }

    private void joinGame(String host, TextField portInput) {
        game.clientIpAddress = host;
        game.port = Integer.parseInt(portInput.getText());
        try {
            initClient();
            join();
        } catch (IOException e) {
            //statusLabel.setText("Failed to connect");
        }
    }

    private void initClient() throws IOException {
        game.client = new GameClient(game, game.clientIpAddress, game.port);
        game.clientThread = new Thread(game.client, "Game client");
        game.clientThread.start();
        System.out.println("Created client");
    }

    private void join() {
        JoinMessage msg = new JoinMessage();
        msg.username = game.username;
        msg.key = game.getRandomHostKey();
        game.client.send(msg);
        System.out.println("Sent join message");
    }

    @Override
    public void render(float delta) {
        game.dispatchNetworkMessages();
        input(delta);
        logic();
        draw(delta);
    }

    private void input(float delta) {

    }

    private void logic() {

    }

    private void draw(float delta) {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //game.viewport.apply();
        //game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();

        batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
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

    }

    private void LobbyTable() {
        root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Table centerArea = new Table();

        Table createSection = new Table();
        Table row1_c = new Table();
        Label portLabel = new Label("Port ", uiSkin);
        portLabel.setFontScale(2f);
        row1_c.add(portLabel).left().padRight(25);
        TextField portInput = new TextField("5000", uiSkin);
        portInput.setAlignment(1);
        row1_c.add(portInput).width(100).height(45).right().pad(25);
        createSection.add(row1_c);
        createSection.row();

        Table row2_c = new Table();
        TextButton joinButton = new TextButton("Join game", uiSkin);
        row2_c.add(joinButton).width(140).height(45).center().pad(25);
        TextButton shutDownButton = new TextButton("Shut down server", uiSkin);
        row2_c.add(shutDownButton).width(140).height(45).center().pad(25);
        createSection.add(row2_c);
        createSection.row();

        Table row3 = new Table();
        TextButton createOpenGameButton = new TextButton("Create open game", uiSkin);
        row3.add(createOpenGameButton).width(140).height(45).center().pad(25);
        TextButton createLobbiedGameButton = new TextButton("Create lobby game", uiSkin);
        row3.add(createLobbiedGameButton).width(140).height(45).center().pad(25);
        createSection.add(row3);
        createSection.row();

        centerArea.add(createSection).left();

        Table joinSection = new Table();
        Table row1_j = new Table();

        Label portLabel2 = new Label("Port", uiSkin);
        portLabel2.setFontScale(2f);
        row1_j.add(portLabel2).left().pad(25);
        TextField portInput2 = new TextField("5000", uiSkin);
        portInput2.setAlignment(1);
        row1_j.add(portInput2).width(100).height(45).right().pad(25);
        joinSection.add(row1_j);
        joinSection.row();

        Table row2_j = new Table();
        Label ipAddressLabel = new Label("IP-address", uiSkin);
        ipAddressLabel.setFontScale(2f);
        row2_j.add(ipAddressLabel).left().pad(25);
        TextField hostInput = new TextField("localhost", uiSkin);
        hostInput.setAlignment(1);
        row2_j.add(hostInput).width(200).height(45).right().pad(25);
        joinSection.add(row2_j);
        joinSection.row();

        TextButton joinButton2 = new TextButton("Join game", uiSkin);
        joinSection.add(joinButton2).width(140).height(45).center().pad(25);

        centerArea.add(joinSection).right();
        root.add(centerArea).center();
        root.row();

        TextButton backButton = new TextButton("Back", uiSkin);
        root.add(backButton).width(140).height(45).pad(25);

        TextButton statusButton = new TextButton("Status (Check console)", uiSkin);
        root.add(statusButton).width(140).height(45).pad(25);

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                joinGame("localhost", portInput);
            }
        });

        joinButton.setDisabled(!game.isServerRunning);
        shutDownButton.setDisabled(!game.isServerRunning);

        shutDownButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.server.shutdown();
                joinButton.setDisabled(true);
                shutDownButton.setDisabled(true);
                createOpenGameButton.setDisabled(false);
                createLobbiedGameButton.setDisabled(false);
                game.cleanup();
            }
        });

        createOpenGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                try {
                    game.generateRandomHostKey();
                    game.serverType = ServerType.OPEN;
                    initServer(true);
                } catch (IOException e) {
                    //statusLabel.setText("Port is busy");
                    e.printStackTrace();
                } finally {
                    joinButton.setDisabled(false);
                    shutDownButton.setDisabled(false);
                    createOpenGameButton.setDisabled(true);
                    createLobbiedGameButton.setDisabled(true);
                }
            }
        });

        createLobbiedGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                try {
                    game.generateRandomHostKey();
                    game.serverType = ServerType.LOBBIED;
                    initLobbiedServerProcess();
                    //game.setScreen(new ClosedGame_LobbyScreen(game, batch, uiSkin, soundManager, true));
                } catch (IOException e) {

                }
            }
        });

        joinButton2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                joinGame(hostInput.getText(), portInput2);
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.clearNetwork();
                game.setScreen(new MainMenuScreen(game, batch, uiSkin, soundManager));
            }
        });

        statusButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.logAll();
            }
        });
    }

    @Override
    public void handleNetworkMessage(NetMessage msg) {
        switch (msg) {
            case JoinAcceptMessage jam -> handleJoinAcceptMessage(jam);
            case LeaveAcceptMessage lam -> handleLeaveAcceptMessage(lam);
            case JoinRejectedMessage jdm -> handleJoinRejectedMessage(jdm);

            default -> {}
        }
    }

    private void handleGameCanStartMessage(GameCanStartStatusMessage gcsm) {

    }

    private void handleJoinAcceptMessage(JoinAcceptMessage jam) {
        game.id = jam.id;
        game.client.id = jam.id;
        game.client.isHost = jam.isHost;
        game.client.serverType = ServerType.getType(jam.serverType);
        System.out.println("Assigned ID: " + game.id + ((jam.isHost) ? " (Host)" : ""));
        System.out.println("Type is " + ServerType.getType(jam.serverType));
        if (game.client.serverType == ServerType.OPEN) {
            game.setScreen(new LoadingScreen(game, batch, uiSkin, soundManager));
        }
        else {
            game.setScreen(new ClosedGame_LobbyScreen(game, batch, uiSkin, soundManager));
        }
    }

    private void handleLeaveAcceptMessage(LeaveAcceptMessage lam) {
        System.out.println("I'm leaving, serverType:" + game.client.serverType);
        if (game.client.serverType == ServerType.LOBBIED) {
            //game.setScreen(new MainLobbyScreen(game, batch, uiSkin));
            System.out.println("Changing table!");
        }
        game.client.disconnect();
    }

    private void handleJoinRejectedMessage(JoinRejectedMessage jrm) {
        System.out.println("Could not join the room. Reason: " + jrm.reason);
    }

    private void handleWorldSnapshotMessage(WorldSnapshotMessage wsm) {

    }
}
