package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.springer.knakobrak.local.SoundManager;

public class OptionsScreen implements Screen {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private final Texture background;

    private Stage stage;
    private Table root;

    private TextField nameInput;
    private SkinSelector skinSelector;
    private SkinSelector ballSelector;

    private TextureAtlas skinAtlas;
    private TextureAtlas ballAtlas;

    private Viewport worldViewPort;

    public OptionsScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;

        background = new Texture("misc/great_war.png");
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


        skinAtlas = new TextureAtlas("skins/character_skins.atlas");
        Array<Drawable> skins = new Array<>();
        for (TextureAtlas.AtlasRegion region : skinAtlas.getRegions()) {
            skins.add(new TextureRegionDrawable(region));
        }
        skinSelector = new SkinSelector(uiSkin, skins, 1);

        ballAtlas = new TextureAtlas("skins/balls_skins.atlas");
        Array<Drawable> balls = new Array<>();
        for (TextureAtlas.AtlasRegion region : ballAtlas.getRegions()) {
            balls.add(new TextureRegionDrawable(region));
        }
        ballSelector = new SkinSelector(uiSkin, balls, 2);

        Label titleLabel = new Label("Options", uiSkin);

        Label nameLabel = new Label("Name:", uiSkin);
        nameInput = new TextField(game.username, uiSkin);


        titleLabel.setFontScale(3.5f);
        root.add(titleLabel).center().padBottom(30);
        root.row().padTop(10).padBottom(10);

        Table customizationArea = new Table();
        customizationArea.add(skinSelector.getRoot()).left();
        customizationArea.add(ballSelector.getRoot()).right();
        root.add(customizationArea);
        root.row().padTop(10);

        root.add(nameLabel).right().padRight(10);
        root.add(nameInput).width(300).height(40);
        root.row().padTop(10);

        TextButton backButton = new TextButton("Back", uiSkin);
        root.add(backButton).width(140).height(45).pad(25);

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveSettings();
                game.setScreen(new MainMenuScreen(game, batch, uiSkin, soundManager));
            }
        });

        //root.setDebug(true);
    }

    private void saveSettings() {
        game.setSettings(nameInput.getText(), skinSelector.getSelectedIndex(), ballSelector.getSelectedIndex());
    }

    @Override
    public void render(float delta) {
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
        skinAtlas.dispose();
    }
}
