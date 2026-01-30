package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.GameClient;
import com.springer.knakobrak.net.GameServer;

import java.io.IOException;

public class MainMenuScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;
    private Texture background;

    public MainMenuScreen(LanPvpGame game) {
        this.game = game;
        this.background = new Texture("great_war.png");
        //this.background = new Texture("libgdx.png");
    }

    @Override
    public void show() {
        // Create UI buttons:
        // Host -> start GameServer, then game.setScreen(new GameScreen(...))
        // Join -> input IP, create GameClient
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.pad(20);

        Label title = new Label("Pvp Game", game.uiSkin);
        title.setFontScale(1.5f);

        table.add(title).colspan(2).padBottom(20);
        table.row();

        Label nameLabel = new Label("Name:", game.uiSkin);
        table.add(nameLabel).pad(20);
        TextField nameInput = new TextField(game.username, game.uiSkin);
        table.add(nameInput).pad(20);
        table.row();

        Label portLabel = new Label("Port:", game.uiSkin);
        table.add(portLabel).pad(20);
        TextField portInput = new TextField("" + game.port, game.uiSkin);
        table.add(portInput).pad(20);
        table.row();

        TextButton hostButton = new TextButton("Host game", game.uiSkin);
        table.add(hostButton).pad(20);
        Label statusLabel = new Label("", game.uiSkin);
        table.add(statusLabel).pad(20);
        table.row();

        TextButton joinButton = new TextButton("Join Game", game.uiSkin);
        table.add(joinButton).pad(20);
        table.row();

        TextButton optionsButton = new TextButton("Options", game.uiSkin);
        table.add(optionsButton).pad(20);

//        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
//        Skin skin = new Skin(uiAtlas);

        hostButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                game.username = nameInput.getText();
                try {
                    // 1. Start server
                    game.hostedServer = new GameServer(game.port);
                    game.serverThread = new Thread(game.hostedServer);
                    game.serverThread.start();
                    // 2. Create client IMMEDIATELY
                    game.client = new GameClient("localhost", game.port);
                    //game.client.connect();
                    // 3. Send name
                    game.client.send(game.username);
                    // 4. Enter lobby
                    game.setScreen(new LobbyScreen(game, true));
                } catch (IOException e) {
                    statusLabel.setText("Port is busy");
                }
            }

//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                try {
//                    GameServer server = new GameServer(Integer.parseInt(portInput.getText()));
//                    server.run();
//                    GameClient client = new GameClient("localhost", 54555);
//                    game.setScreen(new GameScreen(client));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
        });

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.port = Integer.parseInt(portInput.getText());
                game.username = nameInput.getText();
                try {
                    game.client = new GameClient("localhost", game.port);
                    //game.client.connect("localhost", port);
                    game.client.send(game.username);
                    game.setScreen(new LobbyScreen(game, false));
                } catch (IOException e) {
                    statusLabel.setText("Failed to connect");
                }
            }
        });

//        joinButton.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                try {
//                    GameClient client = new GameClient("192.168.1.100", 54555);
//                    game.setScreen(new GameScreen(client));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });

//        Label test = new Label("UI OK", skin);
//        test.setPosition(20, 20);
//        stage.addActor(test);

//        stage.addActor(hostButton);
//        stage.addActor(joinButton);
//        stage.addActor(optionsButton);
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
        stage.dispose();
    }
}
