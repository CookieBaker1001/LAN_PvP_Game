package com.springer.knakobrak;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.springer.knakobrak.net.GameClient;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.screens.LobbyScreen;
import com.springer.knakobrak.screens.MainMenuScreen;


public class LanPvpGame extends Game {
    public SpriteBatch batch;
    public Skin uiSkin;
    public FitViewport viewport;

    public GameClient client; // <-- lives here
    public GameServer hostedServer;
    public Thread serverThread;

    public boolean inChat;
    public String username = "UNNAMED";
    public int clientId;
    public int port = 5000;

    @Override
    public void create() {
        batch = new SpriteBatch();
//        image = new Texture("libgdx.png");

        //font = new BitmapFont();
        //uiSkin = new Skin(Gdx.files.internal("ui/metal/metal-ui.json"));
        uiSkin = new Skin(Gdx.files.internal("ui/clean-crispy/clean-crispy-ui.json"));

        viewport = new FitViewport(8, 5);

        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        //font has 15pt, but we need to scale it to our viewport by ratio of viewport height to screen height
        //font.setUseIntegerPositions(false);
        //font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
//        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
//        batch.begin();
//        batch.draw(image, 140, 210);
//        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
//        image.dispose();
        super.dispose();
    }

    public void cleanupNetworking() {
        if (client != null) {
            client.send("QUIT");
            client.disconnect();
            client = null;
        }
        if (hostedServer != null) {
            hostedServer.shutdown();
            hostedServer = null;
            serverThread = null;
        }
    }
}
