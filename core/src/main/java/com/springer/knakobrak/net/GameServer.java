package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.dto.PlayerStateDTO;
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
    private Thread gameLoopThread;
    private int port;
    private volatile boolean running;
    private volatile boolean shutdownRequested = false;

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private ClientHandler host;
    private final static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);

    private final PhysicsSimulation simulation;
    private int nextProjectileId = 0;

    private float serverTime;

    public GameServer(int port) throws IOException {
        this.port = port;
        this.running = true;
        this.serverSocket = new ServerSocket(port);
        this.simulation = new PhysicsSimulation();
        this.simulation.initPhysics();
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
            gameLoopThread = new Thread(this::gameLoop, "Game loop");
            gameLoopThread.start();
            //Thread.sleep(100);

            while (running) {
                Socket socket = serverSocket.accept();
                if (!running) break;

                ClientHandler client = new ClientHandler(this, socket);
                Thread clientThread = new Thread(client);
                clientThread.start();

                System.out.println("New client connected!");
                if (shutdownRequested) running = false;
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

    void handleLobbyMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof StartGameMessage && sender == host) {
            transitionToLoading();
        } else if (msg instanceof JoinMessage) {
            JoinMessage jm = (JoinMessage) msg;
            handleJoin(sender, jm);
        } else if (msg instanceof DisconnectMessage) {
            DisconnectMessage dcm = (DisconnectMessage) msg;
            handleDisconnect(sender, dcm);
        } else if (msg instanceof EndGameMessage) {
            EndGameMessage egm = (EndGameMessage) msg;
            handleEndGame(egm);
        }
    }

    private void handleEndGame(EndGameMessage egm) {
        shutdownRequested = true;
        DisconnectMessage dcm = new DisconnectMessage();
        dcm.reason = egm.reason;
        clients.values().forEach(c -> {
            //c.requestDisconnect();
            dcm.playerId = c.id;
            removeClient(c, dcm);
        });
        shutdown();
    }

    private void handleDisconnect(ClientHandler sender, DisconnectMessage dcm) {
        removeClient(sender, dcm);
        broadcastPlayerList();
    }

    private void removeClient(ClientHandler sender, DisconnectMessage dcm) {
        System.out.println("Player " + dcm.playerId + " left. Reason: " + dcm.reason);
        clients.remove(sender.id);
        sender.requestDisconnect();
    }

    private void transitionToLoading() {
        broadcast(new EnterLoadingMessage());
        serverState = ServerState.LOADING;
        loadData();
        spawnPlayers();
        sendInitialDataToAllClients();
        broadcast(new LoadingCompleteMessage());
    }

    private void handleJoin(ClientHandler sender, JoinMessage jm) {
        System.out.println(jm.playerName + " (v." + jm.protocolVersion + ") just joined!");
        int id = nextId.getAndIncrement();
        sender.id = id;
        sender.name = jm.playerName;
        if (clients.isEmpty()) {
            host = sender;
            sender.isHost = true;
        }
        PlayerState ps = new PlayerState();
        ps.id = id;
        ps.name = jm.playerName;
        ps.playerIcon = jm.playerIcon;
        ps.ballIcon = jm.ballIcon;
        simulation.addPlayer(ps);
        //simulation.players.put(id, ps);
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
            PlayerStateDTO p = new PlayerStateDTO();
            p.name = c.playerState.name;
            p.id = c.playerState.id;
            lsm.players.add(p);
        }
        System.out.println("Broadcasting players!");
        broadcast(lsm);
    }

    // Runs from the game loop
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
        } else if (msg instanceof ChatMessage) {
            ChatMessage cm = (ChatMessage) msg;
            handleChatMessage(cm);
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
        float dx = spm.dx;
        float dy = spm.dy;
        ProjectileState proj = new ProjectileState();
        proj.id = nextProjectileId++;
        proj.ownerId = sender.id;
        proj.localPlayerFireSequence = spm.fireSequence;
        proj.isAlive = true;
        Vector2 dir = new Vector2(dx, dy).nor();
        Vector2 spawnPos = sender.playerState.body.getPosition()
            .cpy()
            .add(dir.scl(BULLET_SPAWN_OFFSET_M));
        proj.body = LoadUtillities.createProjectile(
            simulation.getWorld(),
            spawnPos.x,
            spawnPos.y,
            proj.id
        );
        proj.body.setLinearVelocity(
            dir.scl(BULLET_SPEED_MPS)
        );
        simulation.addProjectile(proj);
        //simulation.projectiles.put(proj.id, proj);
    }

    void handleChatMessage(ChatMessage cm) {
        cm.message = "<" + clients.get(cm.playerId).name + ">" + cm.message;
        broadcast(cm);
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
        inbox.add(sm);
    }

    final long TICK_MS = 16; // ~60Hz
    float SERVER_TICK_SPEED = 1f / 60f;

    float broadcastRefreshRate = 1 / 20f;
    float broadcastAccumulator = 0f;
    private void gameLoop() {
        while (running && !shutdownRequested && serverState != ServerState.SHUTDOWN) {
            processServerMessages();
            if (serverState == ServerState.GAME) {
                simulation.step(SERVER_TICK_SPEED, 6, 2);
                syncBodiesToGameState();

                broadcastAccumulator += SERVER_TICK_SPEED;
                if (broadcastAccumulator >= broadcastRefreshRate) {
                    broadcastAccumulator -= broadcastRefreshRate;
                    broadcastGameState();
                }
                serverTime += SERVER_TICK_SPEED;
            }
            try {
                Thread.sleep(TICK_MS); // ~60 Hz
            } catch (InterruptedException ignored) {}
        }
    }

    private void syncBodiesToGameState() {
        for (PlayerState p : simulation.getPlayers().values()) {
            if (p.body == null) continue;
            Vector2 pos = p.body.getPosition();
            p.x = pos.x;
            p.y = pos.y;
        }

        for (ProjectileState proj : simulation.getProjectiles().values()) {
            if (proj.body == null) continue;
            Vector2 pos = proj.body.getPosition();
            proj.x = pos.x;
            proj.y = pos.y;
        }
    }

    private void loadData() {
        try {
            simulation.setWallGrid(LoadUtillities.loadLevel("levels/level1.txt"));
            //printWallGrid(grid);
            simulation.setWalls(LoadUtillities.generateWallsFromGrid(simulation.getWorld(), simulation.getWallGrid()));
            //printWalls(simulation.walls);
            simulation.setPlayerSpawnPoints(LoadUtillities.getPlayerSpawnPoints(simulation.getWallGrid()));
        } catch (IOException e) {
            System.out.println("Error loading level: " + e.getMessage());
            e.printStackTrace();
            serverState = ServerState.LOBBY;
        }
    }

    private void printWallGrid(int[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                System.out.print(grid[i][j]);
            }
            System.out.println();
        }
    }

    private void printWalls(ArrayList<Wall> walls) {
        for (Wall w : walls) {
            System.out.println(w);
        }
    }

    private void startGame() {
        System.out.println("[Server]: Ladies and gentlemen; we are starting the GAME!!!");
        serverState = ServerState.GAME;
        broadcast(new StartSimulationMessage());
        //broadcast("START_GAME");
    }

    private void sendInitialDataToAllClients() {
        InitPlayersMessage ipm = new InitPlayersMessage();
        ArrayList<PlayerStateDTO> playersDTO = new ArrayList<>();
        System.out.println("Lets check for players here.");
        for (PlayerState p : simulation.getPlayers().values()) {
            System.out.println("Found one player here....");
            PlayerStateDTO pDTO = new PlayerStateDTO();
            pDTO.name = p.name;
            pDTO.id = p.id;
            pDTO.playerIcon = p.playerIcon;
            pDTO.ballIcon = p.ballIcon;
            pDTO.x = p.x;
            pDTO.y = p.y;
            playersDTO.add(pDTO);
        }
        ipm.players = playersDTO;
        InitWorldMessage iwm = new InitWorldMessage();
        ArrayList<WallDTO> walls = new ArrayList<>();
        for (Wall w : simulation.getWalls()) {
            WallDTO wDTO = new WallDTO();
            wDTO.x = w.x;
            wDTO.y = w.y;
            wDTO.width = w.width;
            wDTO.height = w.height;
            walls.add(wDTO);
        }
        iwm.walls = walls;
        iwm.spawnPoints = simulation.getPlayerSpawnPoints();
        iwm.wallBits = simulation.getWallGrid();
        for (ClientHandler c : clients.values()) {
            c.send(ipm);
            c.send(iwm);
        }
    }

    private void broadcast(NetMessage msg) {
        clients.values().forEach(c -> {
            try {
                c.send(msg);
            } catch (Exception e) {
                System.err.println("FAILED TO SEND TO CLIENT " + c.id);
                e.printStackTrace();
                c.requestDisconnect();
                //c.disconnect();
            }
        });
    }

    private void broadcastGameState() {
        WorldSnapshotMessage wsm = new WorldSnapshotMessage();
        wsm.players = new ArrayList<>();
        for (ClientHandler c : clients.values()) {
            PlayerSnapshot p = new PlayerSnapshot();
            p.id = c.id;
            p.x = c.playerState.x;
            p.y = c.playerState.y;
            p.time = serverTime;
            wsm.players.add(p);
        }
        wsm.projectiles = new ArrayList<>();
        for (ProjectileState ps : simulation.getProjectiles().values()) {
            ProjectileSnapshot snap = new ProjectileSnapshot();
            snap.id = ps.id;
            snap.ownerId = ps.ownerId;
            snap.fireSequence = ps.localPlayerFireSequence;
            snap.x = ps.x;
            snap.y = ps.y;
            snap.vx = ps.body.getLinearVelocity().x;
            snap.vy = ps.body.getLinearVelocity().y;
            snap.alive = ps.isAlive;
            wsm.projectiles.add(snap);
        }
        wsm.serverTime = this.serverTime;
        broadcast(wsm);
    }

    private void spawnPlayers() {
        int i = 0;
        ArrayList<Vector2> points = simulation.getPlayerSpawnPoints();
        for (ClientHandler c : clients.values()) {
            c.playerState.id = c.id;
            c.playerState.x = points.get(i).x + 0.5f;
            c.playerState.y = points.get(i).y + 0.5f;
            c.playerState.body = LoadUtillities.createPlayerBody(simulation.getWorld(), c.playerState.x, c.playerState.y, c.id);
            i++;
        }
    }

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
}
