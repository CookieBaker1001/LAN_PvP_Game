package com.springer.knakobrak.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.ProjectileState;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private final LanPvpGame game;
    private Stage stage;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture background;

    Map<Integer, PlayerState> players = new HashMap<>();
    ShapeRenderer shapeRenderer = new ShapeRenderer();

    Map<Integer, ProjectileState> projectiles = new HashMap<>();

    int playerSize = 20;
    int projectileSize = 10;

    int movementSpeed = 200;
    float positionX = 100;
    float positionY = 100;
    Vector2 touchPos = new Vector2();

    public GameScreen(LanPvpGame game) {
        this.game = game;
        this.batch = game.batch;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.viewport.getScreenWidth(), game.viewport.getScreenHeight());

        shapeRenderer = new ShapeRenderer();
        background = new Texture("grass_bg.png");

        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label label = new Label("GAME STARTED!", game.uiSkin);
        label.setFontScale(2f);
        table.add(label);
    }

    @Override
    public void render(float delta) {
        // 1. Poll input (WASD + mouse)
        // 2. Send InputMessage to server
        // 3. Receive WorldStateMessage
        // 4. Draw circles

        input(delta);
        game.client.poll(this::handleMessage);
        logic(delta);
        draw();

        stage.act(delta);
        stage.draw();
    }

    private void input(float delta) {
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;

//        if (Gdx.input.isTouched()) {
//            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
//            game.viewport.unproject(touchPos);
//            String angle = touchPos.y + " " + touchPos.x;
//            game.client.send("SHOOT " + angle);
//        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 mouse = new Vector3(
                Gdx.input.getX(),
                Gdx.input.getY(),
                0
            );

            camera.unproject(mouse);

            PlayerState me = players.get(game.clientId);
            if (me == null) return;

            float mouseDx = mouse.x - me.x;
            float mouseDy = mouse.y - me.y;

            float len = (float)Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
            if (len == 0) return;

            mouseDx /= len;
            mouseDy /= len;

            game.client.send("SHOOT " + mouseDx + " " + mouseDy);
        }

        if (dx != 0 || dy != 0) {
            game.client.send("MOVE " + dx + " " + dy + " " + (movementSpeed * delta));
        }
    }

    private void moveCameraToPlayer() {
        PlayerState me = players.get(game.clientId);
        if (me != null) {
            camera.position.set(me.x, me.y, 0);
            camera.update();
        }
    }

    private void logic(float delta) {
        moveCameraToPlayer();

    }

    private void draw() {
        //ScreenUtils.clear(Color.BLACK);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        game.worldWidth = game.viewport.getWorldWidth();
        game.worldHeight = game.viewport.getWorldHeight();
        batch.draw(background, 0, 0, game.worldWidth, game.worldHeight);

        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (PlayerState p : players.values()) {
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(p.x, p.y, playerSize);
        }

        shapeRenderer.setColor(Color.YELLOW);
        for (ProjectileState p : projectiles.values()) {
            shapeRenderer.circle(p.x, p.y, projectileSize);
        }
        shapeRenderer.end();
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
        stage.dispose();
        background.dispose();
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("STATE")) {
            updateGameState(msg);
        }
        else if (msg.startsWith("SHOOT")) {
            String[] parts = msg.split(" ");
            System.out.println("Player " + parts[1] + " fired a shot at angle " + parts[2] + "!");
        }
    }

    private void updateGameState(String msg) {
        String[] tokens = msg.split(" ");
        int i = 0;

        if (!tokens[i].equals("STATE")) return;
        i++;

        if (!tokens[i].equals("P")) return;
        i++;

        while (i < tokens.length && !tokens[i].equals("PR")) {
            String[] data = tokens[i].split(":");
            int id = Integer.parseInt(data[0]);
            float x = Float.parseFloat(data[1]);
            float y = Float.parseFloat(data[2]);
            PlayerState p = players.get(id);
            if (p == null) {
                p = new PlayerState();
                p.id = id;
                players.put(id, p);
            }
            p.x = x;
            p.y = y;
            i++;
        }

        projectiles.clear();

        if (i < tokens.length && tokens[i].equals("PR")) {
            i++;
            while (i < tokens.length) {
                String[] data = tokens[i].split(":");
                int id = Integer.parseInt(data[0]);
                float x = Float.parseFloat(data[1]);
                float y = Float.parseFloat(data[2]);

                ProjectileState p = new ProjectileState();
                p.ownerId = id;
                p.x = x;
                p.y = y;

                projectiles.put(id, p);
                i++;
            }
        }

//        System.out.println("Received game state: " + msg);
//        String[] parts = msg.split(" ");
//        for (int i = 1; i < parts.length; i++) {
//            System.out.println("Processing part: " + parts[i]);
//            String[] data = parts[i].split(":");
//            int id = Integer.parseInt(data[0]);
//            float x = Float.parseFloat(data[1]);
//            float y = Float.parseFloat(data[2]);
//
//            PlayerState p = players.get(id);
//            if (p == null) {
//                p = new PlayerState();
//                p.id = id;
//                players.put(id, p);
//            }
//            if (id == game.clientId) {
//                positionX = x;
//                positionY = y;
//            }
//            p.x = x;
//            p.y = y;
//        }
//
//        projectiles.clear();
//
//        for (...) {
//            ProjectileState p = new ProjectileState();
//            p.ownerId = id;
//            p.x = x;
//            p.y = y;
//            projectiles.put(id, p);
//        }
    }
}
