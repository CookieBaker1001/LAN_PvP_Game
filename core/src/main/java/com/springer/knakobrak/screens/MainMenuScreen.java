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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.local.SkinSelector;
import com.springer.knakobrak.net.GameClient;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.messages.JoinMessage;
import com.springer.knakobrak.world.PhysicsSimulation;

import java.io.IOException;

public class MainMenuScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;
    private Table root;
    private TextureAtlas skinAtlas;
    private TextureAtlas ballAtlas;
    private final Texture background;

    private Viewport worldViewPort;

    public MainMenuScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("great_war.png");
        if (game.simulation != null) {
            game.simulation.resetSimulation();
        }
        else game.simulation = new PhysicsSimulation();
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);


        skinAtlas = new TextureAtlas("skins/character_skins.atlas");
        Array<Drawable> skins = new Array<>();
        for (TextureAtlas.AtlasRegion region : skinAtlas.getRegions()) {
            skins.add(new TextureRegionDrawable(region));
        }
        SkinSelector skinSelector = new SkinSelector(game.uiSkin, skins, 1);

        ballAtlas = new TextureAtlas("skins/balls_skins.atlas");
        Array<Drawable> balls = new Array<>();
        for (TextureAtlas.AtlasRegion region : ballAtlas.getRegions()) {
            balls.add(new TextureRegionDrawable(region));
        }
        SkinSelector ballSelector = new SkinSelector(game.uiSkin, balls, 2);

        Label titleLabel = new Label("Pvp Game", game.uiSkin);

        Label nameLabel = new Label("Name:", game.uiSkin);
        TextField nameInput = new TextField(game.username, game.uiSkin);

        Label portLabel = new Label("Port:", game.uiSkin);
        TextField portInput = new TextField("" + game.port, game.uiSkin);

        Label statusLabel = new Label("", game.uiSkin);

        TextButton hostButton = new TextButton("Host game", game.uiSkin);
        TextButton joinButton = new TextButton("Join Game", game.uiSkin);
        TextButton optionsButton = new TextButton("Options", game.uiSkin);



        titleLabel.setFontScale(3.5f);
        root.add(titleLabel).center().padBottom(30);
        root.row().padTop(10).padBottom(10);

        root.add(skinSelector.getRoot()).left();
        root.add(ballSelector.getRoot()).right();
        root.row().padTop(10);

        root.add(nameLabel).right().padRight(10);
        root.add(nameInput).width(300).height(40);
        root.row().padTop(10);

        root.add(portLabel).right().padRight(10);
        root.add(portInput).width(300).height(40);
        root.row().padTop(30);

        root.add(statusLabel).width(140).height(45);
        root.row().padTop(30);

        root.add(hostButton).width(140).height(45).padRight(20);
        root.add(joinButton).width(140).height(45).padRight(20);
        root.add(optionsButton).width(140).height(45);

        //root.setDebug(true);



        hostButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                game.username = nameInput.getText();
                game.playerIcon = skinSelector.getSelectedIndex();
                game.ballIcon = ballSelector.getSelectedIndex();
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
                    msg.ballIcon = game.ballIcon;
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
                game.ballIcon = ballSelector.getSelectedIndex();
                try {
                    game.client = new GameClient(game, "localhost", game.port);
                    game.clientThread = new Thread(game.client, "GameClient");
                    game.clientThread.start();

                    JoinMessage msg = new JoinMessage();
                    msg.playerName = game.username;
                    msg.playerIcon = game.playerIcon;
                    msg.ballIcon = game.ballIcon;
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

        //game.viewport.apply();
        //game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        worldViewPort.apply();
        game.batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        game.batch.begin();

        game.batch.draw(background, 0, 0, worldViewPort.getWorldWidth(), worldViewPort.getWorldHeight());
        game.batch.end();

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
        background.dispose();
        skinAtlas.dispose();
        stage.dispose();
    }
}
