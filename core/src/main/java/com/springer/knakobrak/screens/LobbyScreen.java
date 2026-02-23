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
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.PhysicsSimulation;

public class LobbyScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;
    private Texture background;

    private boolean isHost;

    private List<String> playerListUI;
    private ScrollPane playerScrollPane;
    private Table rootTable;

    private PhysicsSimulation simulation;

    public LobbyScreen(LanPvpGame game, boolean isHost) {
        this.game = game;
        //game.gameState = new ClientGameState();
        this.simulation = game.simulation;
        //this.gameState = game.gameState;
        //game.players.clear();
        this.isHost = isHost;
        this.background = new Texture("final_frontier.jpg");
        //this.background = new Texture("libgdx.png");
    }

    @Override
    public void show() {
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
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
                game.client.disconnect(game.playerId);
                if (isHost) {
                    game.hostedServer.shutdown();
                    try {
                        game.serverThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
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

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();

        game.batch.draw(background, 0, 0, worldWidth, worldHeight);
        game.batch.end();

        game.client.poll(this::handleMessage);
        stage.act(delta);
        stage.draw();
    }

    private void startGame() {
        System.out.println("Host starting game...");
        StartGameMessage msg = new StartGameMessage();
        game.client.send(msg);
    }

    @Override public void resize(int width, int height) {
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
        stage.dispose();
    }

    private void handleMessage(NetMessage msg) {
        if (msg instanceof JoinAcceptMessage) {
            game.playerId = ((JoinAcceptMessage) msg).clientId;
            System.out.println("Assigned ID: " + game.playerId);
        } else if (msg instanceof LobbyStateMessage) {
            LobbyStateMessage lobbyMsg = (LobbyStateMessage) msg;
            updatePlayerList(lobbyMsg);
        } else if (msg instanceof EnterLoadingMessage) {
            simulation.initPhysics();
            game.setScreen(new LoadingScreen(game));
        } else if (msg instanceof EndGameMessage) {
            System.out.println(((EndGameMessage) msg).reason);
            game.cleanupNetworking();
            game.setScreen(new MainMenuScreen(game));
        }
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
}
