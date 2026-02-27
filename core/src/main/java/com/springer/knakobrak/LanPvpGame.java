package com.springer.knakobrak;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.springer.knakobrak.net.GameClient;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.NetworkListener;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.screens.MainMenuScreen;
import com.springer.knakobrak.world.PlayerState;


public class LanPvpGame extends Game {
    public SpriteBatch batch;
    public Skin uiSkin;

    public GameClient client;
    public Thread clientThread;
    public GameServer hostedServer;
    public Thread serverThread;
    public int port = 5000;

    public PlayerState localPlayer;
    public String username;
    public int playerIcon;
    public int ballIcon;
    public int playerId;
    public boolean isHost = false;

    public float worldWidth;
    public float worldHeight;

    public PhysicsSimulation simulation;

    @Override
    public void create() {
        username = "UNNAMED-" + (int)(Math.random() * 100000);
        //playerIcon = (int) (Math.random() * skinCount);
        worldWidth = 0;
        worldHeight = 0;
        batch = new SpriteBatch();

        uiSkin = new Skin(Gdx.files.internal("ui/clean-crispy/clean-crispy-ui.json"));

        this.setScreen(new MainMenuScreen(this));
    }

    public void dispatchNetworkMessages() {
        NetMessage msg;
        while((msg = client.pollOne()) != null) {
            Screen screen = getScreen();
            if (screen instanceof NetworkListener) {
                NetworkListener listener = (NetworkListener) screen;
                listener.handleNetworkMessage(msg);
            } else {
                System.out.println("No networkListener for " + screen);
            }
        }
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
        super.dispose();
    }

    public void cleanupNetworking() {
        if (client != null) {
//            DisconnectMessage dcm = new DisconnectMessage();
//            dcm.playerId = playerId;
//            client.send(dcm);
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
