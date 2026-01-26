package com.springer.knakobrak.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.springer.knakobrak.net.GameClient;

public class GameScreen implements Screen {

    private ShapeRenderer renderer;
    private GameClient client;

    public GameScreen(GameClient client) {
        this.client = client;
        renderer = new ShapeRenderer();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // 1. Poll input (WASD + mouse)
        // 2. Send InputMessage to server
        // 3. Receive WorldStateMessage
        // 4. Draw circles
    }

    @Override
    public void resize(int i, int i1) {

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

    }
}
