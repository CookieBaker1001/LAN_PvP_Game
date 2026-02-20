package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;
import com.springer.knakobrak.world.client.Wall;
import com.badlogic.gdx.physics.box2d.*;
import com.springer.knakobrak.world.server.ServerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.springer.knakobrak.util.Constants.*;

public class GameServer implements Runnable {

    private ServerSocket serverSocket;
    private Thread gameThread;
    private int port;
    private volatile boolean running = true;

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public int getNextId() {
        return nextId.getAndIncrement();
    }

    public boolean isRoomEmpty() {
        return clients.isEmpty();
    }

    public void setHost(ClientHandler handler) {
        host = handler;
    }

    public void addClient(int id, ClientHandler handler) {
        clients.put(id, handler);
        try {
            simulation.players.put(id, handler.playerState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PhysicsSimulation simulation;
    private int nextProjectileId = 1;

    static int worldHeight = 0;
    static int worldWidth = 0;

    public GameServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
        simulation = new PhysicsSimulation();
    }

    enum ServerState {
        LOBBY,
        LOADING,
        GAME,
        SHUTDOWN
    }

    volatile ServerState serverState = ServerState.LOBBY;



    void handleLobbyMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof StartGameMessage && sender == host) {
            transitionToLoading();
        } else if (msg instanceof JoinMessage) {
            JoinMessage jm = (JoinMessage) msg;
            handleJoin(sender, jm);
        } else if (msg instanceof DisconnectMessage) {
            DisconnectMessage dcm = (DisconnectMessage) msg;
            removeClient(sender, dcm);
        }
    }

    private void transitionToLoading() {
        broadcast(new EnterLoadingMessage());
        serverState = ServerState.LOADING;
        simulation.initPhysics();
        loadData();
        spawnPlayers();
        //Thread.sleep(500);
        sendInitialDataToAllClients();
    }

    private void handleJoin(ClientHandler sender, JoinMessage jm) {
        System.out.println(jm.playerName + "(" + jm.protocolVersion + ") just joined!");

        int id = nextId.getAndIncrement();

        sender.id = id;
        sender.isHost = clients.isEmpty();
        if (clients.isEmpty()) {
            host = sender;
        }

        PlayerState ps = new PlayerState();
        ps.id = id;
        ps.name = jm.playerName;
        ps.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

        simulation.players.put(id, ps);
        clients.put(id, sender);

        JoinAcceptMessage accept = new JoinAcceptMessage();
        accept.clientId = id;
        accept.isHost = sender.isHost;

        sender.send(accept);

        broadcastPlayerList();
    }

    private void removeClient(ClientHandler sender, DisconnectMessage dcm) {
        System.out.println("Player " + dcm.playerId + " left. Reason: " + dcm.reason);
        sender.disconnect();
    }

    // Runs from the gameloop
    void processServerMessages() {
        ServerMessage sm;
        while ((sm = inbox.poll()) != null) {
            dispatchMessage(sm.sender, sm.message);
        }
    }

    private void dispatchMessage(ClientHandler sender, NetMessage msg) {
        switch (serverState) {
            case LOBBY: {handleLobbyMessage(sender, msg);}
            case LOADING: {handleLoadingMessage(sender, msg);}
                case GAME: {handleGameMessage(sender, msg);}
            default: {}
        }
    }

    void handleGameMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof PlayerInputMessage) {
            PlayerInputMessage pim = (PlayerInputMessage) msg;
            handlePlayerInput(sender, pim);
        } else if (msg instanceof SpawnProjectileMessage) {
            SpawnProjectileMessage spm = (SpawnProjectileMessage) msg;
            handleSpawnProjectile(sender, spm);
        }
    }

    void handlePlayerInput(ClientHandler sender, PlayerInputMessage pim) {
        int sequence = pim.sequence;
        float dx = pim.dx;
        float dy = pim.dy;
        Body body = sender.playerState.body;
        if (body == null) return;
        Vector2 desiredVelocity = new Vector2(dx, dy)
            .nor()
            .scl(PLAYER_SPEED_MPS);
        body.setLinearVelocity(desiredVelocity);
        sender.lastProcessedInput = sequence;
    }

    void handleSpawnProjectile(ClientHandler sender, SpawnProjectileMessage spm) {
        float dx = spm.x;
        float dy = spm.y;
        ProjectileState proj = new ProjectileState();
        proj.id = nextProjectileId++;
        proj.ownerId = sender.id;
        Vector2 dir = new Vector2(dx, dy).nor();
        Vector2 spawnPos = sender.playerState.body.getPosition()
            .cpy()
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));
        proj.body = LoadUtillities.createProjectile(
            simulation.world,
            spawnPos.x,
            spawnPos.y,
            proj.id
        );
        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );
        simulation.projectiles.put(proj.id, proj);
    }

    void handleLoadingMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof ReadyMessage) {
            ReadyMessage rm = (ReadyMessage) msg;
            handleReadyMessage(sender, rm);
        }
    }

    void handleReadyMessage(ClientHandler sender, ReadyMessage rm) {
        if (!rm.ready) {
            readyClients.remove(sender.id);
            return;
        }
        readyClients.add(sender.id);
        if (readyClients.size() == clients.size()) {
            startGame();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Game server started on port " + port);
            gameThread = new Thread(this::gameLoop, "Game loop");
            gameThread.start();

            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(this, socket);

                Thread clientThread = new Thread(client);
                clientThread.start();

                System.out.println("New client connected!");
//                if (host == null) {
//                    host = client;
//                    System.out.println("Host connected.");
//                } else System.out.println("New client connected");
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            shutdown();
        }
    }

    public void enqueue(ServerMessage sm) {
        //inbox.offer(sm);
        inbox.add(sm);
    }

    public synchronized void handleJoinRequest(ClientHandler client, JoinMessage msg) {
        if (serverState != ServerState.LOBBY) {
            JoinRejectedMessage jrm = new JoinRejectedMessage();
            jrm.reason = "Game already started";
            client.send(jrm);
            client.disconnect();
            return;
        }
//        if (nameTaken(msg.playerName)) {
//            client.send(new JoinRejectedMessage("Name already in use"));
//            client.disconnect();
//            return;
//        }
        // Accept
        client.name = msg.playerName;
        clients.put(client.id, client);
        if (host == null) {
            host = client;
        }
        JoinAcceptMessage jam = new JoinAcceptMessage();
        jam.clientId = client.id;
        jam.isHost = (client == host);
        client.send(jam);
        broadcastPlayerList();
        //broadcastLobbyUpdate();
    }

    private void gameLoop() {
        final long TICK_MS = 16; // ~60Hz
        long last = System.nanoTime();
        long now;
        float delta;
        float dt = 1f / 60f;

        int counter = 0;
        while (serverState != ServerState.SHUTDOWN) {
            now = System.nanoTime();
            delta = (now - last) / 1_000_000_000f;
            last = now;

            processServerMessages();

            if (serverState == ServerState.GAME) {
                //System.out.println("Tick (" + (counter++) + ")");
                //processGameInputs();
                //updateProjectiles(delta);

                simulation.step(dt, 6, 2);

                syncBodiesToGameState();
                broadcastGameState();

            }
            try {
                Thread.sleep(TICK_MS); // ~60 Hz
            } catch (InterruptedException ignored) {}
        }
    }

    private void syncBodiesToGameState() {
        for (PlayerState p : simulation.players.values()) {
            if (p.body == null) continue;

            Vector2 pos = p.body.getPosition();
            p.x = pos.x;
            p.y = pos.y;
        }

        for (ProjectileState proj : simulation.projectiles.values()) {
            if (proj.body == null) continue;

            Vector2 pos = proj.body.getPosition();
            proj.x = pos.x;
            proj.y = pos.y;
        }
    }

//    private void processMessagesInLobby() throws InterruptedException {
//        for (ClientHandler c : clients.values()) {
//            NetMessage msg;
//            while ((msg = c.incoming.poll()) != null) {
//                if (msg instanceof StartGameMessage) {
//                    broadcast(new EnterLoadingMessage());
//                    serverState = ServerState.LOADING;
//                    simulation.initPhysics();
//                    loadData();
//                    spawnPlayers();
//                    Thread.sleep(500);
//                    sendInitialDataToAllClients();
//                } else if (msg instanceof JoinMessage) {
//                    JoinMessage jm = (JoinMessage) msg;
//                } else if (msg instanceof DisconnectMessage) {
//                    DisconnectMessage dcm = (DisconnectMessage) msg;
//                    System.out.println("Player " + dcm.playerId + " left. Reason: " + dcm.reason);
//                    c.disconnect();
//                }
//            }
//        }
//    }

    private void loadData() {
//        simulation.clearWalls();
//        simulation.clearPlayerSpawnPoints();
        try {
            int[][] grid = LoadUtillities.loadLevel("levels/level1.txt");
            System.out.println();
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[0].length; j++) {
                    System.out.print(grid[i][j]);
                }
                System.out.println();
            }
            simulation.walls = LoadUtillities.generateWallsFromGrid(simulation.world, grid);
            printWalls(simulation.walls);
            simulation.playerSpawnPoints = LoadUtillities.getPlayerSpawnPoints(grid);
        } catch (IOException e) {
            System.out.println("Error loading level: " + e.getMessage());
            e.printStackTrace();
            serverState = ServerState.LOBBY;
            //return;
        }
    }

    private void printWalls(ArrayList<Wall> walls) {
        for (Wall w : walls) {
            System.out.println(w);
        }
    }

    Set<Integer> readyClients = new HashSet<>();
//    private void processMessagesInLoadingScreen() {
//        for (ClientHandler c : clients.values()) {
////            String line;
////            while ((line = c.incoming.poll()) != null) {
////                //if (line.equals("START_GAME") && c == host) {
////                if (line.equals("READY")) {
////                    readyClients.add(c.id);
////                    if (readyClients.size() == clients.size()) {
////                        startGame();
////                    }
////                }
////            }
//
//            NetMessage msg;
//            while ((msg = c.incoming.poll()) != null) {
//                if (msg instanceof ReadyMessage) {
//                    if (!((ReadyMessage) msg).ready) {
//                        readyClients.remove(c);
//                        continue;
//                    }
//                    readyClients.add(c.id);
//                    if (readyClients.size() == clients.size()) {
//                        startGame();
//                    }
//                }
//            }
//        }
//    }

    private void startGame() {
        serverState = ServerState.GAME;
        broadcast(new StartSimulationMessage());
        //broadcast("START_GAME");
    }

    private void sendInitialDataToAllClients() {
        for (ClientHandler c : clients.values()) {
            sendInitialData(c);
        }
    }

    void sendInitialData(ClientHandler c) {

        // Tell client which player it owns
        //c.send("WELCOME " + c.id);

        // Send player list
        for (PlayerState p : simulation.players.values()) {
            InitPlayerMessage ipm = new InitPlayerMessage();
            ipm.player.id = p.id;
            ipm.player.x = p.x;
            ipm.player.y = p.y;
            c.send(ipm);
            //c.send("INIT_PLAYER " + p.id + " " + p.x + " " + p.y);
        }

        // Send wall map
        sendWallLayout(c);

        //c.send("INIT_DONE");

        c.send(new StartSimulationMessage());
    }

    void sendWallLayout(ClientHandler c) {

//        int mapWidth = (int) (worldWidth * PIXELS_PER_METER);
//        int mapHeight = (int) (worldHeight * PIXELS_PER_METER);
//
//        c.send("INIT_MAP " + mapWidth + " " + mapHeight);

        InitWorldMessage iwm = new InitWorldMessage();

        iwm.walls = simulation.walls;
        iwm.spawnPoints = simulation.playerSpawnPoints;

        c.send(iwm);
    }

//    private void processGameInputs() {
//        for (ClientHandler c : clients.values()) {
////            String line;
////            while ((line = c.incoming.poll()) != null) {
////                //System.out.println("Processing input from player " + c.id + ": " + line);
////                if (line.startsWith("MOVE")) {
////                    String[] p = line.split(" ");
////                    int sequence = Integer.parseInt(p[1]);
////                    float dx = Float.parseFloat(p[2]);
////                    float dy = Float.parseFloat(p[3]);
////
////                    Body body = c.playerState.body;
////                    if (body == null) continue;
////
////                    Vector2 desiredVelocity = new Vector2(dx, dy)
////                        .nor()
////                        .scl(PLAYER_SPEED_MPS);
////                    body.setLinearVelocity(desiredVelocity);
////
////                    c.lastProcessedInput = sequence;
////                }
////                else if (line.equals("STOP")) {
////                    Body body = c.playerState.body;
////                    if (body != null) {
////                        body.setLinearVelocity(0, 0);
////                    }
////                }
////                else if (line.startsWith("SHOOT")) {
////                    String[] p = line.split(" ");
////                    float dx = Float.parseFloat(p[1]);
////                    float dy = Float.parseFloat(p[2]);
////                    ProjectileState proj = new ProjectileState();
////                    proj.id = nextProjectileId++;
////                    //proj.ownerId = c.id;
////                    Vector2 dir = new Vector2(dx, dy).nor();
////
////                    Vector2 spawnPos = c.playerState.body.getPosition()
////                        .cpy()
////                        .add(dir.scl(BULLET_SPAWN_OFFSET_M));
////
////                    proj.body = LoadUtillities.createProjectile(
////                        simulation.world,
////                        spawnPos.x,
////                        spawnPos.y,
////                        proj.id
////                    );
////
////                    proj.body.setLinearVelocity(
////                        dir.scl(BULLET_SPEED_MPS)
////                    );
////
////                    simulation.projectiles.put(proj.id, proj);
////                }
////                else if (line.startsWith("MSG")) {
////                    broadcast(line);
////                }
////            }
//
//            NetMessage msg;
//            while((msg = c.incoming.poll()) != null) {
//                if (msg instanceof PlayerInputMessage) {
//                    PlayerInputMessage pim = (PlayerInputMessage) msg;
//                    int sequence = pim.sequence;
//                    float dx = pim.dx;
//                    float dy = pim.dy;
//
//                    Body body = c.playerState.body;
//                    if (body == null) continue;
//
//                    Vector2 desiredVelocity = new Vector2(dx, dy)
//                        .nor()
//                        .scl(PLAYER_SPEED_MPS);
//                    body.setLinearVelocity(desiredVelocity);
//
//                    c.lastProcessedInput = sequence;
//                } else if (msg instanceof SpawnProjectileMessage) {
//                    SpawnProjectileMessage spm = (SpawnProjectileMessage) msg;
//                    float dx = spm.x;
//                    float dy = spm.y;
//                    ProjectileState proj = new ProjectileState();
//                    proj.id = nextProjectileId++;
//                    proj.ownerId = c.id;
//                    Vector2 dir = new Vector2(dx, dy).nor();
//
//                    Vector2 spawnPos = c.playerState.body.getPosition()
//                        .cpy()
//                        .add(dir.scl(BULLET_SPAWN_OFFSET_M));
//
//                    proj.body = LoadUtillities.createProjectile(
//                        simulation.world,
//                        spawnPos.x,
//                        spawnPos.y,
//                        proj.id
//                    );
//
//                    proj.body.setLinearVelocity(
//                        dir.scl(BULLET_SPEED_MPS)
//                    );
//
//                    simulation.projectiles.put(proj.id, proj);
//                }
//            }
//        }
//    }

    private void broadcast(NetMessage msg) {
        if (msg instanceof EnterLoadingMessage) {
            clients.values().forEach(c -> c.send(msg));
        }
    }

//    private void broadcastWalls() {
//        StringBuilder sb = new StringBuilder("GAME_START");
//        for (Wall w : simulation.walls) {
//            sb.append(" ").append(w.x)
//                .append(" ").append(w.y)
//                .append(" ").append(w.width)
//                .append(" ").append(w.height);
//        }
//        broadcast(sb.toString());
//    }

    private void broadcastPlayerList() {
        LobbyStateMessage lsm = new LobbyStateMessage();
        lsm.hostId = host.id;
        lsm.players = new ArrayList<>();
        for (ClientHandler c : clients.values()) {
            PlayerState p = new PlayerState();
            p.id = c.id;
            p.name = c.name;
            lsm.players.add(p);
        }
        broadcast(lsm);
    }

    private void broadcastGameState() {
        WorldSnapshotMessage wsm = new WorldSnapshotMessage();
        wsm.players = new ArrayList<>();

        for (ClientHandler c : clients.values()) {
            PlayerState p = new PlayerState();
            p.id = c.id;
            p.x = c.playerState.x;
            p.y = c.playerState.y;
            wsm.players.add(p);
        }
        wsm.projectiles = new ArrayList<>();
        for (ProjectileState p : simulation.projectiles.values()) {
            ProjectileState p2 = new ProjectileState();
            p2.id = p.id;
            p2.x = p.x;
            p2.y = p.y;
            wsm.projectiles.add(p2);
        }
        broadcast(wsm);
    }

    private void spawnPlayers() {
        int i = 0;
        for (ClientHandler c : clients.values()) {
            //c.serverPlayerState = new ServerPlayerState();
            c.playerState.id = c.id;
            c.playerState.x = simulation.playerSpawnPoints.get(i).x + 0.5f;
            c.playerState.y = simulation.playerSpawnPoints.get(i).y + 0.5f;
            c.playerState.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
            c.playerState.body = LoadUtillities.createPlayerBody(simulation.world, c.playerState.x, c.playerState.y, c.id);
            i++;
        }
    }

    public void shutdown() {
        running = false;
        clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        simulation.players.clear();
        host = null;
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (IOException ignored) {}
    }
}
