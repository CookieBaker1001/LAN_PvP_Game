package com.springer.knakobrak.net.multiplayerEntities;

import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.ServerMessage;
import com.springer.knakobrak.world.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer implements Runnable {

//    private LobbyHelper lobbyHelper;
//    private LoadingHelper loadingHelper;
//    private GameHelper gameHelper;

    private ServerSocket serverSocket;
    private Thread gameLoopThread;
    private int port;
    private volatile boolean running;
    private volatile boolean shutdownRequested = false;

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private ClientHandler host;
    private final static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);

    private final PhysicsSimulation simulation;

    private float serverTime;

    public GameServer(int port) throws IOException {
        this.port = port;
        this.running = true;
        this.serverSocket = new ServerSocket(port);
        this.simulation = new PhysicsSimulation("Server");
        //this.simulation.initPhysics();
        this.serverTime = 0f;

//        this.lobbyHelper = new LobbyHelper(this, simulation, clients, host, nextId);
//        this.loadingHelper = new LoadingHelper(this, clients);
//        this.gameHelper = new GameHelper(simulation, clients);
    }

    enum ServerState {
        LOBBY,
        LOADING,
        GAME,
        SHUTDOWN
    }

    volatile ServerState serverState = ServerState.LOBBY;

    @Override
    public void run() {
        try {
            System.out.println("Game server started on port " + port);
            gameLoopThread = new Thread(this::gameLoop, "Game loop");
            gameLoopThread.start();
            //Thread.sleep(100);

            while (running) {
                Socket socket = serverSocket.accept();
                if (!running) break;

//                ClientHandler client = new ClientHandler(this, socket);
//                Thread clientThread = new Thread(client);
//                clientThread.start();
//
//                System.out.println("New client connected!");
//                if (shutdownRequested) running = false;
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }
//        catch (InterruptedException e2) {
//            e2.printStackTrace();
//        }
        finally {
            System.out.println("Shutting down!");
            shutdown();
        }
    }

    public void setState(int state) {
        switch (state) {
            case 0:
                serverState = ServerState.LOBBY;
                break;
            case 1:
                serverState = ServerState.LOADING;
                break;
            case 2:
                serverState = ServerState.GAME;
                break;
        }
    }

    public void requestShutdown() {
        shutdownRequested = true;
    }

    // Runs from the game loop
    void processServerMessages() {
        ServerMessage sm;
        while ((sm = inbox.poll()) != null) {
            dispatchMessage(sm.sender, sm.message);
        }
    }

    private void dispatchMessage(ClientHandler sender, NetMessage msg) {
//        switch (serverState) {
//            case LOBBY: {lobbyHelper.handleLobbyMessage(sender, msg);}
//            case LOADING: {loadingHelper.handleLoadingMessage(sender, msg);}
//            case GAME: {gameHelper.handleGameMessage(sender, msg);}
//            default: {}
//        }
    }

    public void enqueue(ServerMessage sm) {
        inbox.add(sm);
    }

    float secondCounter = 0f;
    final long TICK_MS = 16; // ~60Hz
    float SERVER_TICK_SPEED = 1f / 60f;

    float broadcastRefreshRate = 1 / 20f;
    float broadcastAccumulator = 0f;
    private void gameLoop() {
        while (running && !shutdownRequested && serverState != ServerState.SHUTDOWN) {
            processServerMessages();
            if (serverState == ServerState.GAME) {
                //simulation.step(SERVER_TICK_SPEED, 6, 2);
                syncBodiesToGameState();

                broadcastAccumulator += SERVER_TICK_SPEED;
                if (broadcastAccumulator >= broadcastRefreshRate) {
                    broadcastAccumulator -= broadcastRefreshRate;
                    //broadcastGameState();
                }
                secondCounter += SERVER_TICK_SPEED;
                if (secondCounter >= 1f) {
                    secondCounter -= 1f;
                    doSomethingEverySecond();
                }
                serverTime += SERVER_TICK_SPEED;
            }
            try {
                Thread.sleep(TICK_MS); // ~60 Hz
            } catch (InterruptedException ignored) {}
        }
    }

    private void doSomethingEverySecond() {
        //simulation.printProjectileList();
    }

    private void syncBodiesToGameState() {
        for (Player p : simulation.players.values()) {
            if (p.body == null) continue;
            Vector2 pos = p.body.getPosition();
            p.realX = pos.x;
            p.realY = pos.y;
        }

//        for (ProjectileState proj : simulation.projectiles.values()) {
//            if (proj.body == null) continue;
//            Vector2 pos = proj.body.getPosition();
//            proj.x = pos.x;
//            proj.y = pos.y;
//        }
    }

    private void printWallGrid(int[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                System.out.print(grid[i][j]);
            }
            System.out.println();
        }
    }

//    private void printWalls(ArrayList<Wall> walls) {
//        for (Wall w : walls) {
//            System.out.println(w);
//        }
//    }

    public void broadcast(NetMessage msg) {
        clients.values().forEach(c -> {
            try {
                c.send(msg);
            } catch (Exception e) {
                System.err.println("FAILED TO SEND TO CLIENT " + c.id);
                e.printStackTrace();
                //c.requestDisconnect();
                //c.disconnect();
            }
        });
    }

    private void send(ClientHandler c, NetMessage msg) {
        c.send(msg);
    }

//    private void broadcastGameState() {
//        WorldSnapshotMessage wsm = new WorldSnapshotMessage();
//        wsm.players = new ArrayList<>();
//        for (ClientHandler c : clients.values()) {
//            PlayerSnapshot p = new PlayerSnapshot();
//            p.id = c.id;
////            p.x = c.playerState.x;
////            p.y = c.playerState.y;
//            p.time = serverTime;
//            wsm.players.add(p);
//        }
//        wsm.projectiles = new ArrayList<>();
//        for (ProjectileState ps : simulation.projectiles.values()) {
//            ProjectileSnapshot snap = new ProjectileSnapshot();
//            snap.counter = ps.counter;
//            snap.ownerId = ps.clientId;
//            snap.x = ps.x;
//            snap.y = ps.y;
//            snap.vx = ps.body.getLinearVelocity().x;
//            snap.vy = ps.body.getLinearVelocity().y;
//            snap.lifeTime = ps.lifeTime;
//            wsm.projectiles.add(snap);
//        }
//        wsm.serverTime = this.serverTime;
//        broadcast(wsm);
//    }

    public void shutdown() {
        running = false;
        host = null;
        //clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        simulation.resetSimulation();
        serverState = ServerState.SHUTDOWN;
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onTakeDamage(int playerId, int projectileId) {
//        PlayerHealthMessage phm = new PlayerHealthMessage();
//        PlayerState p = simulation.getPlayer(playerId);
//        phm.playerId = playerId;
//        phm.hp = p.hp;
//        broadcast(phm);
//        if (p.hp <= 0) {
//            System.out.println("This guy should be dead!");
//            p.isInvincible = true;
//            p.deathTimer = 0f;
//            PlayerDeathMessage dm = new PlayerDeathMessage();
//            dm.nextSpawnPointX = p.nextSpawnPoint.x;
//            dm.nextSpawnPointY = p.nextSpawnPoint.y;
//            send(clients.get(playerId), dm);
//        }
//    }
}
