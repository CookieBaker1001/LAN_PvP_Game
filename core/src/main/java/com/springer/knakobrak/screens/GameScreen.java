package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.local.SoundManager;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.Constants;
import com.springer.knakobrak.util.PingThresholds;
import com.springer.knakobrak.world.*;

import java.util.*;

import static com.springer.knakobrak.util.Constants.*;

public class GameScreen implements Screen, NetworkListener {

    private final LanPvpGame game;
    private final SpriteBatch batch;
    private final Skin uiSkin;
    private final SoundManager soundManager;

    private Stage stage;

    private Texture background;
    private Texture heartTexture;
    private TextureRegion deadPlayer;

    private TextureAtlas playerSkinsAttlas;

    private Viewport worldViewPort;

    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private Label timeLabel;
    private Label coordinatesLabel;

    private Table mainTable;
    private Table consoleArea;
    private Table chatTable;
    private ScrollPane chatScroll;
    private TextField chatInput;
    private Queue<String> messages;

    private Table consoleArea_History;
    private Table chatTable_History;
    private ScrollPane chatScroll_History;
    private boolean chatMode = false;

    private Table membersUiTable;
    private Table membersTable;
    private ScrollPane membersScroll;

    private Table pauseMenu;
    private boolean paused = false;
    private Drawable tintedBG;

    private Table heartTable;

    private final Texture[] ping;
    private Texture wallTexture;
    private Texture spawnTexture;

    private PhysicsSimulation simulation;
    private Player player;
    private Map<Integer, TextureRegion> playerSkins = new HashMap<>();
    private Map<Integer, Texture> ballSkins = new HashMap<>();

    private float localTime = 0f;

    OrthographicCamera cam;
    float camAngleDeg = 0f;
    float camAngleRad = 0f;
    float deltaX = 0f;
    float rotationSpeed = 0.5f;
    private final boolean rotateCameraMode = false;

    public GameScreen(LanPvpGame game, SpriteBatch batch, Skin uiSkin, SoundManager soundManager) {
        this.game = game;
        this.batch = batch;
        this.uiSkin = uiSkin;
        this.soundManager = soundManager;

        simulation = game.simulation;
        player = simulation.getPlayer(game.client.id);
        System.out.println("Got player with id " + game.client.id + ", Status: " + (player == null ? "null" : ("("+player.realX +","+player.realY +")")));
        //game.soundManager.playMusic("game", true);
        //simulation.setSimulationOwner(this);

        wallTexture = new Texture("tiles/wall.png");
        spawnTexture = new Texture("tiles/spawn.png");

        messages = new Queue<>();
        tintedBG = uiSkin.newDrawable("white", 0, 0, 0, 0.5f);
        ping = new Texture[6];
        for (int i = 0; i < 6; i++) {
            ping[i] = new Texture(Gdx.files.internal("ping/ping"+i+".png"));
        }

        for (Player ps : simulation.players.values()) {
            System.out.println(ps.body == null ? "Player body "+ps.id+" is null" : "Player body "+ps.id+" is not null");
        }

        localTime = (System.currentTimeMillis() - game.gameStartTime) / 1000f;
    }

    @Override
    public void show() {
        worldViewPort = new FitViewport(1280, 720);
        stage = new Stage(new ScreenViewport());
        cam = (OrthographicCamera) worldViewPort.getCamera();
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCursorCatched(rotateCameraMode);

        playerSkinsAttlas = new TextureAtlas("skins/character_skins.atlas");

        heartTexture = new Texture("tiles/heart.png");
        background = new Texture("misc/grass_bg.png");

        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();
        stage.addActor(mainTable);

        Table topBar = createTopBar();
        mainTable.add(topBar).expandX().fillX().pad(10);
        mainTable.row();

        Table middleBar = new Table();
        mainTable.add(middleBar).expand().fill();
        mainTable.row();

        Table bottomBar = new Table();
        Stack stack = createConsoleArea();
        bottomBar.add(stack).width(500).left().bottom();
        bottomBar.add(new Table()).expand().right();
        mainTable.add(bottomBar).expandX().fillX();

        createMembersTable();

        pauseMenu = new Table();
        pauseMenu.setFillParent(true);
        pauseMenu.center();
        stage.addActor(pauseMenu);

        Table pauseMenuFrame = new Table();
        pauseMenuFrame.setFillParent(true);
        pauseMenuFrame.center();
        stage.addActor(pauseMenuFrame);

        pauseMenu = createPauseMenu();
        pauseMenuFrame.add(pauseMenu).pad(5);

        consoleArea.setVisible(false);
        consoleArea_History.setVisible(true);
        pauseMenu.setVisible(false);
        membersUiTable.setVisible(false);

        shapeRenderer = new ShapeRenderer();
        int i = 0;
        for (Player ps : simulation.players.values()) {
            playerSkins.put(i, playerSkinsAttlas.findRegion("p" + ps.playerIcon));
            //playerSkins.put(i, new Texture("characters/p" + ps.playerIcon + ".png"));
            ballSkins.put(i, new Texture("balls/b" + ps.ballIcon + ".png"));
            i++;
        }
        deadPlayer = playerSkinsAttlas.findRegion("pDead");

        //mainTable.setDebug(true);
    }

    private Table createTopBar() {
        coordinatesLabel = new Label("x: 0.0, y: 0.0", uiSkin);
        coordinatesLabel.setFontScale(1.5f);
        timeLabel = new Label("Time: 0.0s", uiSkin);
        timeLabel.setFontScale(1.25f);
        heartTable = createHealthBar(MAX_HEALTH, heartTexture);

        Table topBar = new Table();
        Table topLeft = new Table();
        topLeft.add(coordinatesLabel).padBottom(3f);
        topLeft.row();
        topLeft.add(timeLabel);
        topBar.add(topLeft).left().expandX();
        topBar.add(heartTable).right().expandX();
        return topBar;
    }

    private void createMembersTable() {
        membersUiTable = new Table();
        membersUiTable.setFillParent(true);
        membersUiTable.center().top();
        stage.addActor(membersUiTable);

        membersTable = new Table();
        membersScroll = new ScrollPane(membersTable, uiSkin);
        membersScroll.setFadeScrollBars(false);
        membersUiTable.add(membersTable).center().top();
    }

    private Stack createConsoleArea() {
        Stack stack = new Stack();

        consoleArea_History = new Table();
        consoleArea_History.setFillParent(true);
        consoleArea_History.center();
        stage.addActor(consoleArea_History);

        Table chatArea_History = new Table();
        chatTable_History = new Table();
        chatTable_History.bottom().left();

        chatScroll_History = new ScrollPane(chatTable_History, uiSkin);
        chatScroll_History.setFadeScrollBars(false);
        chatArea_History.add(chatTable_History).expand().fill();
        consoleArea_History.add(chatArea_History).fillX().expandX().padBottom(70);

        consoleArea = new Table();
        Table chatArea = new Table();
        chatTable = new Table();
        chatTable.bottom().left();

        chatScroll = new ScrollPane(chatTable, uiSkin);
        chatScroll.setFadeScrollBars(false);
        chatArea.add(chatTable).expand().fill();
        consoleArea.add(chatArea).center();
        consoleArea.row();

        Table chatInputArea = new Table();

        chatInput = new TextField("", uiSkin);
        chatInput.setAlignment(1);
        chatInputArea.add(chatInput).fillX().expandX().height(45).left();

        consoleArea.add(chatInputArea).fillX().expandX().padBottom(25);
        stack.add(consoleArea);
        stack.add(consoleArea_History);

        return stack;
    }

    private Table createPauseMenu() {
        Table pauseTable = new Table();

        Table row1 = new Table();
        Label pausedLabel = new Label("Paused <ESC>", uiSkin);
        pausedLabel.setFontScale(5f);
        row1.add(pausedLabel).center().pad(25);
        pauseTable.add(row1);
        pauseTable.row();

        Table row2 = new Table();
        TextButton quitButton = new TextButton("Exit", uiSkin);
        row2.add(quitButton).center().width(140).height(45).pad(25);
        TextButton statusButton = new TextButton("Status (Check console)", uiSkin);
        row2.add(statusButton).center().width(140).height(45).pad(25);
        pauseTable.add(row2);
        pauseTable.setBackground(tintedBG);

        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                LeaveGameMessage lm = new LeaveGameMessage();
                lm.reason = "Client left";
                game.client.send(lm);
            }
        });

        statusButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.logAll();
            }
        });

        return pauseTable;
    }

    private Table createHealthBar(int maxHp, Texture heartRegion) {
        Table hearts = new Table();
        for (int i = 0; i < maxHp; i++) {
            Image heart = new Image(new TextureRegionDrawable(heartRegion));
            hearts.add(heart).size(24).padLeft(4);
        }
        return hearts;
    }

    public void updateHealth(int currentHp) {
        for (int i = 0; i < heartTable.getChildren().size; i++) {
            Actor heart = heartTable.getChildren().get(i);
            heart.setVisible(i < currentHp);
        }
    }

    float FIXED_DT = 1 / 60f;
    float physicsAccumulator = 0f;

    int secondsCounter = 0;
    float secondsAccumulator = 0f;

    @Override
    public void render(float delta) {
        game.dispatchNetworkMessages();
        input(delta);
        logic(delta);
        draw(delta);
    }

    private void enterChatMode() {
        Gdx.input.setCursorCatched(false);
        stage.setKeyboardFocus(chatInput);

        consoleArea.setVisible(chatMode);
        consoleArea_History.setVisible(!chatMode);
    }

    private void exitChatMode(boolean send) {
        String msg = chatInput.getText();

        if (!msg.isEmpty() && send) {
            // Send to server
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.message = msg;
            game.client.send(chatMessage);
        }

        if (rotateCameraMode) Gdx.input.setCursorCatched(true);
        chatInput.setText("");
        stage.unfocus(chatInput);
        stage.setKeyboardFocus(null);

        consoleArea.setVisible(chatMode);
        consoleArea_History.setVisible(!chatMode);
    }

    private void togglePause() {
        paused = !paused;
        pauseMenu.setVisible(paused);

        if (paused) {
            Gdx.input.setCursorCatched(false);
        } else {
            if (rotateCameraMode) {
                Gdx.input.setCursorCatched(true);
            }
        }
    }

    private void input(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (chatMode) {
                chatMode = false;
                exitChatMode(false);
            } else {
                togglePause();
            }
        }
        if (paused) return;

        membersUiTable.setVisible(Gdx.input.isKeyPressed(Input.Keys.TAB));
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) game.logAll();

        Vector2 input = Vector2.Zero;
        if (!chatMode && !player.isDead) {
            input = handleWASD();
        }

        applyMovement(input, delta);

        if (paused) return;
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            //handleFire();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            chatMode = !chatMode;
            if (chatMode) {
                enterChatMode();
            }
            else {
                exitChatMode(true);
            }
        }
    }

    private Vector2 handleWASD() {
        Vector2 input = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) input.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) input.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) input.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) input.x += 1;
        return input;
    }

    private void applyMovement(Vector2 input, float delta) {
        Vector2 desiredVelocity = new Vector2(input.x, input.y)
            .nor()
            .scl(PLAYER_SPEED_MPS * delta);
        player.realX += desiredVelocity.x;
        player.realY += desiredVelocity.y;

        player.lateX = player.realX;
        player.lateY = player.realY;

        PlayerWASDMessage pwasdm = new PlayerWASDMessage();
        pwasdm.x = player.realX;
        pwasdm.y = player.realY;
        try {
            game.client.send(pwasdm);
        } catch (Exception e) {
            System.out.println("Client is null");
        }
    }

    private void moveCameraToPlayer() {
        if (player != null) {
            float x = Constants.metersToPx(player.realX);
            float y = Constants.metersToPx(player.realY);
            cam.position.set(x, y, 0);
        }
    }

    private void logic(float delta) {
        if (rotateCameraMode && !paused && !chatMode && !player.isDead) {
            deltaX = Gdx.input.getDeltaX();
            camAngleDeg += deltaX * rotationSpeed;
            cam.rotate(deltaX * rotationSpeed);
            camAngleRad = camAngleDeg * MathUtils.degreesToRadians;
        }
        moveCameraToPlayer();
        updateCoordinateLabel();
        updateTimeLabel();
        interpolatePlayers();

        localTime += delta;

        secondsAccumulator += delta;
        if (secondsAccumulator > 1f) {
            ++secondsCounter;
            secondsAccumulator -= 1f;
            doSomethingEverySecond();
        }
    }

    private void interpolatePlayers() {
        float t = 0.1f;
        for (Player p : simulation.players.values()) {
            if (p.id == game.client.id) continue;
            p.lateX = MathUtils.lerp(p.lateX, p.realX, t);
            p.lateY = MathUtils.lerp(p.lateY, p.realY, t);
        }
    }

    private void doSomethingEverySecond() {
        //System.out.println("Time: " + secondsCounter);
        //simulation.playerStates();
    }

    private void updateCoordinateLabel() {
        coordinatesLabel.setText(String.format("x: %.1f, y: %.1f", player.realX, player.realY));
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("Time: %.1f", localTime));
    }

    private void draw(float delta) {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0.2f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewPort.apply();
        batch.setProjectionMatrix(worldViewPort.getCamera().combined);

        batch.begin();
        batch.draw(background, PIXELS_PER_METER/2, PIXELS_PER_METER/2, (game.worldWidth-1) * PIXELS_PER_METER, (game.worldHeight-1) * PIXELS_PER_METER);

        for (int i = 0; i < simulation.wallGrid.length; i++) {
            for (int j = 0; j < simulation.wallGrid[0].length; j++) {
                int w = simulation.wallGrid[i][j];
                if (w == 1) {
                    batch.draw(wallTexture, PIXELS_PER_METER * j, PIXELS_PER_METER * i, PIXELS_PER_METER, PIXELS_PER_METER);
                }
                if (w == 0) {
                    continue;
                }
                if (w == 2) {
                    batch.draw(spawnTexture, PIXELS_PER_METER * j, PIXELS_PER_METER * i, PIXELS_PER_METER, PIXELS_PER_METER);
                }
            }
        }

        for (Player p : simulation.players.values()) {
            if (p == player) continue;
            batch.draw(playerSkins.get(0),
                Constants.metersToPx(p.lateX) - PLAYER_RADIUS_PX, Constants.metersToPx(p.lateY) - PLAYER_RADIUS_PX,
                PLAYER_RADIUS_PX, PLAYER_RADIUS_PX, PLAYER_RADIUS_PX*2, PLAYER_RADIUS_PX*2,
                1f, 1f, 0);
        }

        batch.draw(playerSkins.get(0),
            Constants.metersToPx(player.realX) - PLAYER_RADIUS_PX, Constants.metersToPx(player.realY) - PLAYER_RADIUS_PX,
            PLAYER_RADIUS_PX, PLAYER_RADIUS_PX, PLAYER_RADIUS_PX*2, PLAYER_RADIUS_PX*2,
            1f, 1f, 0);

        batch.end();
        //drawGrid();

        stage.act(delta);
        stage.draw();
    }

    private void drawGrid() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLUE);
        for (int i = -20; i <= 20; i++) {
            float p = Constants.metersToPx(i);
            shapeRenderer.line(p, -1000, p, 1000); // vertical
            shapeRenderer.line(-1000, p, 1000, p); // horizontal
        }
        shapeRenderer.end();
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
        //Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        background.dispose();
    }

    int c = 0;
    @Override
    public void handleNetworkMessage(NetMessage msg) {
        c++;
        if (c >= 20) {
            System.out.println("Type of message: " + msg.getClass().getSimpleName());
            c -= 20;
        }
        switch (msg) {
            case WorldStateMessage wsm -> handleWorldStateMessage(wsm);
            case ChatMessage cm -> handleChatMessage(cm);
            case LeaveAcceptMessage lam -> handleLeaveAcceptMessage(lam);
            case PlayerListMessage plm -> handlePlayerListMessage(plm);
            case PingMessage pm -> handlePingMessage(pm);
            default -> {
                //System.out.println("Unknown message format: " + msg.getClass());
            }
        }
    }

    private void handlePingMessage(PingMessage pm) {
        PingResponseMessage prm = new PingResponseMessage();
        prm.clientId = game.client.id;
        prm.sequenceResponse = pm.secuence;
        prm.pingTimeResponse = pm.pingTime;
        game.client.send(prm);
    }

    private void handlePlayerListMessage(PlayerListMessage plm) {
        membersTable.clear();
        Label header = new Label("- Players -", uiSkin);
        membersTable.add(header).pad(10);
        membersTable.row();
        //System.out.println("Ping");
        for (int i = 0; i < plm.ids.length; i++) {
            //System.out.println(plm.ids[i] + ": " + plm.pings[i]);
            Label playerLabel = new Label("(" + plm.ids[i] + ") " + (plm.ids[i] == plm.hostId ? "(host) " : "") + plm.names[i], uiSkin);
            playerLabel.setFontScale(1f);
            Table row = new Table();
            row.add(playerLabel).fillX().left().pad(1);
            Image pingImage = getImage(plm, i);
            row.add(pingImage).width(20).height(20).right().pad(1);
            row.setBackground(tintedBG);
            membersTable.add(row).expandX().fillX().pad(1);
            membersTable.row();
        }
        //System.out.println("End of Ping");
        Gdx.app.postRunnable(() -> {
            membersScroll.layout();
            membersScroll.setScrollPercentY(1f);
        });
    }

    private Image getImage(PlayerListMessage plm, int i) {
        Image pingImage;
        if (plm.pings[i] <= PingThresholds.PING_5) pingImage = new Image(ping[5]);
        else if (plm.pings[i] <= PingThresholds.PING_4) pingImage = new Image(ping[4]);
        else if (plm.pings[i] <= PingThresholds.PING_3) pingImage = new Image(ping[3]);
        else if (plm.pings[i] <= PingThresholds.PING_2) pingImage = new Image(ping[2]);
        else if (plm.pings[i] <= PingThresholds.PING_1) pingImage = new Image(ping[1]);
        else pingImage = new Image(ping[0]);
        return pingImage;
    }

    float counter = 0f;
    private void handleWorldStateMessage(WorldStateMessage wsm) {
        for (int i = 0; i < wsm.x.length; i++) {
            Player p = simulation.getPlayer(wsm.ids[i]);
            if (p == null) {
                p = new Player();
                p.id = wsm.ids[i];
                simulation.addPlayer(p.id, p);
            }
            if (wsm.ids[i] == game.client.id) continue;
//            p.x = wsm.x[i];
//            p.y = wsm.y[i];
            p.realX = wsm.x[i];
            p.realY = wsm.y[i];
        }
        for (Player p : simulation.players.values()) {
            boolean found = false;
            for (int i = 0; i < wsm.x.length; i++) {
                if (wsm.ids[i] == p.id) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                simulation.removePlayer(p.id);
            }
        }
    }

    private void handleChatMessage(ChatMessage cm) {
        messages.addLast(cm.message);
        if (messages.size >= 11) messages.removeFirst();
        addChatMessageToTable(chatTable, chatScroll);
        addMessageToHistoryTable(cm.message);
    }

    public void addChatMessageToTable(Table table, ScrollPane scroll) {
        table.clear();
        for (String s : messages) {
            Label messageLabel = new Label(s, uiSkin);
            messageLabel.setFontScale(1.25f);
            messageLabel.setWrap(true);
            Table bubble = new Table(uiSkin);
            bubble.add(messageLabel).pad(1).width(500);
            bubble.setBackground(tintedBG);
            table.add(bubble).expandX().fillX().pad(1);
            table.row();
        }
        Gdx.app.postRunnable(() -> {
            scroll.layout();
            scroll.setScrollPercentY(1f);
        });
    }

    private void addMessageToHistoryTable(String message) {
        Label messageLabel = new Label(message, uiSkin);
        messageLabel.setFontScale(1.25f);
        messageLabel.setWrap(true);
        Table bubble = new Table(uiSkin);
        bubble.add(messageLabel).pad(1).width(500);
        bubble.setBackground(tintedBG);
        Table row = new Table();
        row.add(bubble);
        chatTable_History.add(row).expandX().fillX().pad(1);
        row.addAction(Actions.sequence(
            Actions.fadeIn(0.2f),
            Actions.delay(10f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()
        ));
        chatTable_History.row();
//            .getActor().addAction(Actions.sequence(
//            Actions.delay(10.7f),
//            Actions.removeActor()
//        ));
        Gdx.app.postRunnable(() -> {
            chatScroll_History.layout();
            chatScroll_History.setScrollPercentY(1f);
        });
    }

    private void handleLeaveAcceptMessage(LeaveAcceptMessage lam) {
        System.out.println(lam.message);
        game.client.disconnect();
        game.setScreen(new LobbyScreen(game, batch, uiSkin, soundManager));
    }
}
