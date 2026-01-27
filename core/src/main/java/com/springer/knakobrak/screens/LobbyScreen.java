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

public class LobbyScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;
    private Texture background;

    private boolean isHost;

    private List<String> playerListUI;
    private ScrollPane playerScrollPane;
    private Table rootTable;

    public LobbyScreen(LanPvpGame game, boolean isHost) {
        this.game = game;
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
                // Disconnect client
                game.client.disconnect();
                // If host, stop server
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
                    game.client.send("START_GAME");
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

    private void handleMessage(String msg) {
        if (msg.startsWith("ASSIGNED_ID")) {
            game.clientId = Integer.parseInt(msg.split(" ")[1]);
        } else if (msg.startsWith("PLAYER_LIST")) {
            updatePlayerList(msg);
        } else if (msg.equals("GAME_START")) {
            game.setScreen(new GameScreen(game));
        } else if (msg.equals("HOST_LEFT")) {
            game.cleanupNetworking();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void updatePlayerList(String msg) {
        // Update player list UI
        System.out.println("Received player list: " + msg);
        String[] parts = msg.split("_");
        Array<String> names = new Array<>();
        for (int i = 1; i < parts.length; i++) {
            String entry = parts[i]; // "1:Alice"
            String[] pair = entry.split(":");
            if (pair.length == 2) {
                String name = pair[1];
                names.add(name);
            }
        }
        System.out.println("Updated player list: " + names);
        playerListUI.setItems(names);
    }
}
