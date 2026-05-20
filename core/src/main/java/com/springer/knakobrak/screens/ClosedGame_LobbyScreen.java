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
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.PlayerListItem;
import com.springer.knakobrak.world.PhysicsSimulation;

import java.util.ArrayList;

public class ClosedGame_LobbyScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private Stage stage;
    private Texture background;

    private List<String> playerListUI;
    private ScrollPane playerScrollPane;

    private Table rootTable;
    private Table playerGrid;
    private TextButton startButton;

    private PhysicsSimulation simulation;

    private Viewport worldViewPort;

    public ClosedGame_LobbyScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;

        simulation = game.simulation;
        background = new Texture("misc/final_frontier.jpg");
    }

    boolean gameIsReady = true;
    boolean ready = true;
    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        playerListUI = new List<>(uiSkin);
        playerScrollPane = new ScrollPane(playerListUI, uiSkin);

        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);

        Table middleBar = new Table();
        Label title = new Label("Lobby", uiSkin);
        title.setFontScale(1.5f);
        middleBar.add(title).center().pad(25);
        middleBar.row();
        playerGrid = new Table();
        generatePlayerGrid(null);
        middleBar.add(playerGrid);

        rootTable.add(middleBar).expandX().fillX().pad(25);
        rootTable.row();

        Table bottomBar = new Table();
        TextButton backButton = new TextButton("Back", uiSkin);
        bottomBar.add(backButton).width(140).height(45).center().pad(25);
        startButton = new TextButton("Start game", uiSkin);
        TextButton readyButton = new TextButton("Ready", uiSkin);

        if (game.client.isHost) {
            System.out.println("I am host!");
            bottomBar.add(startButton).width(140).height(45).pad(25);
            rootTable.add(bottomBar).expandX().fillX().pad(25);
        } else {
            System.out.println("I am not host!");
            bottomBar.add(readyButton).width(140).height(45).pad(25);
            rootTable.add(bottomBar).expandX().fillX().pad(25);
        }

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.client.isHost) {
                    game.server.shutdown();
                    game.cleanup();
                } else {
                    LeaveLobbyMessage lm = new LeaveLobbyMessage();
                    lm.reason = "Client left";
                    game.client.send(lm);
                }
            }
        });
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                EveryOneIsReadyMessage eirm = new EveryOneIsReadyMessage();
                game.client.send(eirm);
            }
        });
        readyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ReadyMessage rm = new ReadyMessage();
                rm.ready = ready;
                game.client.send(rm);
                System.out.println(ready ? "I'm ready" : "I'm not ready");
                ready = !ready;
                readyButton.setText(ready ? "Ready" : "Not ready");
            }
        });
    }

    @Override
    public void render(float delta) {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();

        batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        batch.end();

        game.dispatchNetworkMessages();
        stage.act(delta);
        stage.draw();
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

    private void generatePlayerGrid(ArrayList<PlayerListItem> list) {
        playerGrid.clear();
        Table table = new Table();
        if (list != null) {
            int counter = 0;
            for (PlayerListItem p : list) {
                if (counter > 2) {
                    table.row();
                    counter = 0;
                }
                counter++;
                String s = (p.id == game.id ? "(You) " : "") + (p.isHost ? "(Host) " : "") + p.name;
                Label label = new Label(s, uiSkin);
                label.setFontScale(2f);
                table.add(label).center().pad(25);
            }
        } else {
            Label label = new Label("No players have joined yet.", uiSkin);
            label.setFontScale(2f);
            table.add(label).center().pad(25);
        }
        playerGrid.add(table);
    }

    @Override
    public void handleNetworkMessage(NetMessage msg) {
        switch (msg) {
            case PlayerListMessage plm -> handlePlayerListMessage(plm);
            case GameCanStartStatusMessage gcsm -> handleGameCanStartStatusMessage(gcsm);
            case LeaveAcceptMessage lam -> handleLeaveAcceptMessage(lam);
            case StartGameMessage sgm -> handleStartGameMessage(sgm);
            default -> {}
        }
    }

    private void handleStartGameMessage(StartGameMessage sgm) {
        game.setScreen(new LoadingScreen(game, batch, uiSkin, soundManager));
    }

    private void handleGameCanStartStatusMessage(GameCanStartStatusMessage gcsm) {
        gameIsReady = gcsm.canStart;
        startButton.setText(gameIsReady ? "Start game" : "Start game\n(Waiting for players)");
        startButton.setDisabled(!gameIsReady);
    }

    private void handleLeaveAcceptMessage(LeaveAcceptMessage lam) {
        System.out.println(lam.message);
        game.client.disconnect();
        game.setScreen(new LobbyScreen(game, batch, uiSkin, soundManager));
    }

    private void handlePlayerListMessage(PlayerListMessage plm) {
        ArrayList<PlayerListItem> plil = new ArrayList<>();
        for (int i = 0; i < plm.ids.length; i++) {
            PlayerListItem pli = new PlayerListItem(plm.ids[i], plm.names[i], plm.hostId == plm.ids[i]);
            plil.add(pli);
        }
        generatePlayerGrid(plil);
    }
}
