package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.ProjectileStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.*;
import com.badlogic.gdx.physics.box2d.*;

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
    private Thread sevrerThread;
    private int port;
    private volatile boolean running = true;

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private PhysicsSimulation simulation;
    private int nextProjectileId = 1;

    private float serverTime;

    public GameServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
        this.simulation = new PhysicsSimulation();
        this.serverTime = 0f;
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
            sevrerThread = new Thread(this::gameLoop, "Game loop");
            sevrerThread.start();

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

    void handleLobbyMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof StartGameMessage && sender == host) {
            System.out.println("Start game");
            transitionToLoading();
        } else if (msg instanceof JoinMessage) {
            System.out.println("Join game");
            JoinMessage jm = (JoinMessage) msg;
            handleJoin(sender, jm);
        } else if (msg instanceof DisconnectMessage) {
            System.out.println("Disconnecting from game");
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
        broadcast(new LoadingCompleteMessage());
    }

    private void handleJoin(ClientHandler sender, JoinMessage jm) {
        System.out.println(jm.playerName + " (v." + jm.protocolVersion + ") just joined!");

        int id = nextId.getAndIncrement();

        sender.id = id;
        sender.name = jm.playerName;
        sender.isHost = clients.isEmpty();
        if (clients.isEmpty()) {
            host = sender;
        }

        PlayerState ps = new PlayerState();
        ps.id = id;
        ps.name = jm.playerName;
        ps.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

        simulation.players.put(id, ps);
        sender.playerState = ps;
        clients.put(id, sender);

        JoinAcceptMessage accept = new JoinAcceptMessage();
        accept.clientId = id;
        accept.isHost = sender.isHost;

        sender.send(accept);

        broadcastPlayerList();
    }

    private void broadcastPlayerList() {
        LobbyStateMessage lsm = new LobbyStateMessage();
        lsm.hostId = host.id;
        lsm.players = new ArrayList<>();
        for (ClientHandler c : clients.values()) {
            PlayerStateDTO p = PlayerStateDTO.toDTO(c.playerState);
            lsm.players.add(p);
        }
        System.out.println("Broadcasting players!");
        broadcast(lsm);
    }

    private void removeClient(ClientHandler sender, DisconnectMessage dcm) {
        System.out.println("Player " + dcm.playerId + " left. Reason: " + dcm.reason);
        sender.disconnect();
    }

    // Runs from the gameloop
    void processServerMessages() {
        ServerMessage sm;
        while ((sm = inbox.poll()) != null) {
            //System.out.println("[GS]: Handling: " + sm);
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

    Set<Integer> readyClients = new HashSet<>();
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

    public void enqueue(ServerMessage sm) {
        //inbox.offer(sm);
        //System.out.println("Received something!");
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
    }

    private void gameLoop() {
        //long last = System.nanoTime();

        while (serverState != ServerState.SHUTDOWN) {

            final long TICK_MS = 16; // ~60Hz
//            long now = System.nanoTime();
//            float delta = (now-last) / 1_000_000_000f;
//            last = now;

            float dt = 1f / 60f;

            processServerMessages();
            if (serverState == ServerState.GAME) {
                simulation.step(dt, 6, 2);
                syncBodiesToGameState();
                broadcastGameState();
                serverTime += dt;
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

    private void loadData() {
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
        }
    }

    private void printWalls(ArrayList<Wall> walls) {
        for (Wall w : walls) {
            System.out.println(w);
        }
    }

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
        for (PlayerState p : simulation.players.values()) {
            InitPlayerMessage ipm = new InitPlayerMessage();
            ipm.player = PlayerStateDTO.toDTO(p);
            c.send(ipm);
            //c.send("INIT_PLAYER " + p.id + " " + p.x + " " + p.y);
        }
        sendWallLayout(c);
    }

    void sendWallLayout(ClientHandler c) {
        InitWorldMessage iwm = new InitWorldMessage();
        iwm.walls = WallDTO.toDTO(simulation.walls);
        iwm.spawnPoints = simulation.playerSpawnPoints;
        c.send(iwm);
    }

    private void broadcast(NetMessage msg) {
        clients.values().forEach(c -> c.send(msg));
    }

    private void broadcastGameState() {
        WorldSnapshotMessage wsm = new WorldSnapshotMessage();
        wsm.players = new ArrayList<>();

        for (ClientHandler c : clients.values()) {
//            PlayerStateDTO p = PlayerStateDTO.toDTO(c.playerState);
//            PlayerStateDTO p = new PlayerStateDTO();
//            p.id = c.id;
//            p.x = c.playerState.x;
//            p.y = c.playerState.y;
//            wsm.players.add(p);
            PlayerSnapshot p = new PlayerSnapshot();
            p.id = c.id;
            p.x = c.playerState.x;
            p.y = c.playerState.y;
            p.time = serverTime;
            wsm.players.add(p);
        }
        wsm.projectiles = new ArrayList<>();
        for (ProjectileState p : simulation.projectiles.values()) {
            //ProjectileStateDTO p2 = ProjectileStateDTO.toDTO(p);
            ProjectileSnapshot p2 = new ProjectileSnapshot();
            p2.id = p.id;
            p2.ownerId = p.ownerId;
            p2.x = p.x;
            p2.y = p.y;
//            ProjectileStateDTO p2 = new ProjectileStateDTO();
//            p2.id = p.id;
//            p2.x = p.x;
//            p2.y = p.y;
            wsm.projectiles.add(p2);
        }
        wsm.serverTime = this.serverTime;
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
