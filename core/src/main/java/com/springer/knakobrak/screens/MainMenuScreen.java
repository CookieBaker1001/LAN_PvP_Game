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
import com.springer.knakobrak.world.PhysicsSimulation;

public class MainMenuScreen implements Screen {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private Stage stage;
    private Table root;

    private final Texture background;

    private Viewport worldViewPort;

    public MainMenuScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        System.out.println("[MainMenuScreen constructor]");
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;

        background = new Texture("misc/great_war.png");
        if (game.simulation != null) {
            game.simulation.resetSimulation();
        }
        else game.simulation = new PhysicsSimulation("Client");
    }

    @Override
    public void show() {

        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(worldViewPort);
        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Label titleLabel = new Label("LAN Pvp Game", uiSkin);
        titleLabel.setFontScale(5f);
        TextButton startButton = new TextButton("Start", uiSkin);
        TextButton optionsButton = new TextButton("Options", uiSkin);
        TextButton quitButton = new TextButton("Quit", uiSkin);

        Table topBar = new Table();
        topBar.add(titleLabel).center().expandX();

        root.add(topBar).expandX().fillX().pad(25);
        root.row();

        root.add(startButton).width(140).height(45).pad(25);
        root.row();

        root.add(optionsButton).width(140).height(45).pad(25);
        root.row();

        root.add(quitButton).width(140).height(45).pad(25);
        root.row();

        Table bottomBar = new Table();

        TextButton statusButton = new TextButton("Status (Check console)", uiSkin);

        Label versionLabel = new Label("v 1.0.0", uiSkin);
        versionLabel.setFontScale(1.5f);
        bottomBar.add(versionLabel).left().expandX();
        Label companyLabel = new Label("KnaKoBraK AB", uiSkin);
        companyLabel.setFontScale(1.5f);
        bottomBar.add(companyLabel).right().expandX();
        bottomBar.add(statusButton).right().expandX();

        root.add(bottomBar).expandX().fillX().pad(30);

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LobbyScreen(game, batch, uiSkin, soundManager));
            }
        });

        optionsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new OptionsScreen(game, batch, uiSkin, soundManager));
            }
        });

        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.exit(0);
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
        background.dispose();
        stage.dispose();
    }
}
