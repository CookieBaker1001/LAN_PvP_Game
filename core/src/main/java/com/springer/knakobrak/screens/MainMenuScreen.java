package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.local.SkinSelector;
import com.springer.knakobrak.net.GameClient;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.messages.JoinMessage;

import java.io.IOException;

public class MainMenuScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;
    private Table root;
    private TextureAtlas skinAtlas;
    private Texture background;
    private Sprite[] playerSkins;

    public MainMenuScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("great_war.png");
    }

    @Override
    public void show() {
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.pad(20);

        Label title = new Label("Pvp Game", game.uiSkin);
        title.setFontScale(1.5f);

        root.add(title).colspan(2).padBottom(20);
        root.row();

        skinAtlas = new TextureAtlas("skins/character_skins.atlas");
        Array<Drawable> skins = new Array<>();
        for (TextureAtlas.AtlasRegion region : skinAtlas.getRegions()) {
            skins.add(new TextureRegionDrawable(region));
        }

        SkinSelector skinSelector = new SkinSelector(game.uiSkin, skins);
        root.add(skinSelector.getRoot());

        Label nameLabel = new Label("Name:", game.uiSkin);
        root.add(nameLabel).pad(20);
        TextField nameInput = new TextField(game.username, game.uiSkin);
        root.add(nameInput).pad(20);
        root.row();

        Label portLabel = new Label("Port:", game.uiSkin);
        root.add(portLabel).pad(20);
        TextField portInput = new TextField("" + game.port, game.uiSkin);
        root.add(portInput).pad(20);
        root.row();

        TextButton hostButton = new TextButton("Host game", game.uiSkin);
        root.add(hostButton).pad(20);
        Label statusLabel = new Label("", game.uiSkin);
        root.add(statusLabel).pad(20);
        root.row();

        TextButton joinButton = new TextButton("Join Game", game.uiSkin);
        root.add(joinButton).pad(20);
        root.row();

        TextButton optionsButton = new TextButton("Options", game.uiSkin);
        root.add(optionsButton).pad(20);

        hostButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                game.username = nameInput.getText();
                game.playerIcon = skinSelector.getSelectedIndex();
                try {
                    game.hostedServer = new GameServer(game.port);
                    game.serverThread = new Thread(game.hostedServer, "GameServer");
                    game.serverThread.start();

                    game.client = new GameClient(game, "localhost", game.port);
                    game.clientThread = new Thread(game.client, "GameClient");
                    game.clientThread.start();

                    JoinMessage msg = new JoinMessage();
                    msg.playerName = game.username;
                    msg.playerIcon = game.playerIcon;
                    game.client.send(msg);

                    game.setScreen(new LobbyScreen(game, true));
                } catch (IOException e) {
                    statusLabel.setText("Port is busy");
                }
            }
        });

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                game.username = nameInput.getText();
                game.playerIcon = skinSelector.getSelectedIndex();
                try {
                    game.client = new GameClient(game, "localhost", game.port);
                    game.clientThread = new Thread(game.client, "GameClient");
                    game.clientThread.start();

                    JoinMessage msg = new JoinMessage();
                    msg.playerName = game.username;
                    msg.playerIcon = game.playerIcon;
                    game.client.send(msg);

                    game.setScreen(new LobbyScreen(game, false));
                } catch (IOException e) {
                    statusLabel.setText("Failed to connect");
                }
            }
        });
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
        background.dispose();
        skinAtlas.dispose();
        stage.dispose();
    }
}
