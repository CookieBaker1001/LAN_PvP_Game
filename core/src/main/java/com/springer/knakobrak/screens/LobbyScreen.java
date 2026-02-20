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
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.client.PlayerState;

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
        System.out.println("Message received!");
        if (msg instanceof JoinAcceptMessage) {
            game.playerId = ((JoinAcceptMessage) msg).clientId;
            System.out.println("Assigned ID: " + game.playerId);
        } else if (msg instanceof LobbyStateMessage) {
            LobbyStateMessage lobbyMsg = (LobbyStateMessage)msg;
            updatePlayerList(lobbyMsg);
        } else if (msg instanceof EnterLoadingMessage) {
            simulation.initPhysics();
            game.setScreen(new LoadingScreen(game));
        } else if (msg instanceof EndGameMessage) {
            System.out.println(((EndGameMessage) msg).reason);
            game.cleanupNetworking();
            game.setScreen(new MainMenuScreen(game));
        }

//        if (msg.startsWith("ASSIGNED_ID")) {
//            String[] data = msg.split(" ");
//            game.playerId = Integer.parseInt(data[1]);
//            //game.gameState.localPlayerId = Integer.parseInt(data[1]);
//            //game.playerColor = new Color(Float.parseFloat(data[2]), Float.parseFloat(data[3]), Float.parseFloat(data[4]), 1);
//            //System.out.println("Assigned ID: " + game.gameState.localPlayerId);
//            System.out.println("Assigned ID: " + game.playerId);
//        } else if (msg.startsWith("PLAYER_LIST")) {
//            updatePlayerList(msg);
//        }
////        else if (msg.startsWith("WALLS")) {
////            receiveWalls(msg);
////        }
////        else if (msg.equals("GAME_START")) {
////            game.setScreen(new GameScreen(game));
////        }
//        else if (msg.equals("ENTER_LOADING")) {
//            simulation.initPhysics();
//            game.setScreen(new LoadingScreen(game));
//        }
//        else if (msg.startsWith("GAME_START")) {
//            //receiveWalls(msg);
//            game.setScreen(new GameScreen(game));
//        } else if (msg.equals("HOST_LEFT")) {
//            game.cleanupNetworking();
//            game.setScreen(new MainMenuScreen(game));
//        }
    }

//    private void receiveWalls(String msg) {
//        String[] parts = msg.split(" ");
//        System.out.println("Received walls: " + msg);
//        game.gameState.walls.clear();
//        for (int i = 1; i < parts.length; i += 4) {
//            float x = Constants.metersToPx(Float.parseFloat(parts[i]));
//            float y = Constants.metersToPx(Float.parseFloat(parts[i + 1]));
//            float width = Constants.metersToPx(Float.parseFloat(parts[i + 2]));
//            float height = Constants.metersToPx(Float.parseFloat(parts[i + 3]));
//            Wall wall = new Wall();
//            wall.x = x;
//            wall.y = y;
//            wall.width = width;
//            wall.height = height;
//            game.gameState.walls.add(wall);
//            System.out.println("Data: " + x + ", " + y + ", " + width + ", " + height);
//        }
//    }

    private void updatePlayerList(LobbyStateMessage msg) {
        System.out.println("Updating lobby list!");
        Array<String> names = new Array<>();
        for (PlayerState p : msg.players) {
            String entry = p.name;
            if (p.id == msg.hostId) entry += " (HOST)";
            names.add(entry);
        }
        System.out.println("Updated player list: " + names);
        playerListUI.setItems(names);
    }

//    private void updatePlayerList(String msg) {
//        // Update player list UI
//        //System.out.println("Received player list: " + msg);
//        String[] parts = msg.split("_");
//        Array<String> names = new Array<>();
//        for (int i = 1; i < parts.length; i++) {
//            String entry = parts[i]; // "1:Alice"
//            String[] pair = entry.split(":");
//            if (pair.length == 2) {
//                String name = pair[1];
//                names.add(name);
//            }
//        }
//        System.out.println("Updated player list: " + names);
//        playerListUI.setItems(names);
//    }
}
