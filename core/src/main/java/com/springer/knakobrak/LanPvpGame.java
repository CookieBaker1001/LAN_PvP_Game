package com.springer.knakobrak;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.springer.knakobrak.local.SoundManager;
import com.springer.knakobrak.net.multiplayerEntities.GameClient;
import com.springer.knakobrak.screens.NetworkListener;
import com.springer.knakobrak.net.multiplayerEntities.Server;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.util.ServerType;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.screens.MainMenuScreen;

import java.util.Random;


public class LanPvpGame extends Game {

    private SoundManager soundManager;
    private SpriteBatch batch;
    private Skin uiSkin;

    public Server server;
    public Thread serverThread;
    public GameClient client;
    public Thread clientThread;
    public String clientIpAddress = "localhost";
    public int port = 5000;

    public ServerType serverType = ServerType.UNDEFINED;

    public int id;
    public String username;

    public int playerIcon;
    public int ballIcon;

    private long randomHostKey = -1;

    public boolean isServerRunning = false;
    public boolean isClientShuttingDown = false;

    public PhysicsSimulation simulation;

    public int worldHeight;
    public int worldWidth;

    @Override
    public void create() {
        soundManager = new SoundManager();
        soundManager.loadMusic("game", "sounds/action1.mp3");
        batch = new SpriteBatch();
        uiSkin = new Skin(Gdx.files.internal("ui/default/uiSkin.json"));
        username = "UNNAMED-" + (int)(Math.random() * 100000);
        this.setScreen(new MainMenuScreen(this, batch, uiSkin, soundManager));
    }

    public void startGameWorld(boolean start) {
        if (start) {
            simulation = new PhysicsSimulation("Client");
        } else if (simulation != null) {
            simulation.closeSimulation();
            simulation = null;
        }
    }

    public void generateRandomHostKey() {
        Random rd = new Random();
        randomHostKey = rd.nextLong(0, 1_000_000_000);
    }

    public long getRandomHostKey() {
        return randomHostKey;
    }

    public boolean tryToMatchHostKey(long otherKey) {
        return randomHostKey == otherKey;
    }

    public void dispatchNetworkMessages() {
        if (client == null) return;
        NetMessage msg;
        try {
            while ((msg = client.pollOne()) != null) {
                Screen screen = getScreen();
                if (screen instanceof NetworkListener listener) {
                    listener.handleNetworkMessage(msg);
                } else {
                    System.out.println("No networkListener for " + screen);
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected!");
            //e.printStackTrace();
        }
    }

    public void tryToShutDownClient() {
        System.out.println("Trying to shut down client!");
        if (!isClientShuttingDown) return;
        try {
            clientThread.join();
            client = null;
            System.out.println("Client is now null!");
        } catch (Exception e) {
            System.out.println("Error: Count not close the client thread.");
        } finally {
            clientThread = null;
            client = null;
        }
    }

    public void tryToShutDownLobbiedServer() {
        if (serverType == ServerType.LOBBIED) {
            clearNetwork();
        }
    }

    public void clearNetwork() {
        tryToShutDownClient();
        try {
            server.shutdown();
            cleanup();
        }
        catch (Exception ignored) {}
        id = 0;
        port = 0;
        randomHostKey = -1;
        serverType = ServerType.UNDEFINED;
    }

    public void cleanup() {
        try {
            serverThread.join();
            server = null;
        } catch (InterruptedException e) {
            System.err.println("Error: Count not close the server thread.");
        } finally {
            serverThread = null;
            isServerRunning = false;
        }
        randomHostKey = -1;
        serverType = ServerType.UNDEFINED;
    }

    public void setSettings(String name, int skinIndex, int ballIndex) {
        username = name;
        playerIcon = skinIndex;
        ballIcon = ballIndex;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        uiSkin.dispose();
        soundManager.dispose();
    }

    public void logAll() {
        String s = "[START_OF_STATUS]\nID: " + id + ", Name: " + username + ", Port: " + port + "\nServer: " + serverType
            + "\nServer thread: " + serverThread + "\nClient: " + client + "\nClient thread: " + clientThread
            + "\nClient IP-address: " + clientIpAddress + "\nServer type: " + serverType
            + "\nServer running: " + isServerRunning + "\nClient shutting down: " + isClientShuttingDown
            + "\nRandom server key: " + getRandomHostKey() + "\n[END_OF_STATUS]";
        System.out.println(s);
    }
}
