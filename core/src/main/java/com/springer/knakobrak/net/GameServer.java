package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.serialization.NetworkRegistry;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;
import com.springer.knakobrak.world.client.Wall;
import com.badlogic.gdx.physics.box2d.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.springer.knakobrak.util.Constants.*;

public class GameServer implements Runnable {

    private ServerSocket serverSocket;
    private int port;
    private volatile boolean running = true;

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

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



    @Override
    public void run() {
        try {
            System.out.println("Game server started on port " + port);
            new Thread(this::gameLoop).start();
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                new Thread(client).start();
                if (host == null) {
                    host = client;
                    System.out.println("Host connected.");
                } else System.out.println("New client connected");
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

    private class ClientHandler implements Runnable {

        private int id;
        private String name;

        private Socket socket;
        private Kryo kryo;
        Input in;
        Output out;

        private boolean isHost;

        private final Queue<NetMessage> incoming = new ConcurrentLinkedQueue<>();
        public PlayerState playerState;

        public int lastProcessedInput = 0;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            kryo = new Kryo();
            NetworkRegistry.register(kryo);
            in = new Input(socket.getInputStream());
            out = new Output(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                handshake();
                readLoop();
            } catch (IOException e) {
                //System.out.println("Something went wrong!");
                e.printStackTrace();
            } finally {
                //System.out.println("Finally...");
                disconnect();
            }
        }

        private void readLoop() throws IOException {
            NetMessage msg;
            while (!socket.isClosed()) {
                //msg = (NetMessage) kryo.readClassAndObject(in);
                msg = kryo.readObject(in, NetMessage.class);
                incoming.add(msg);
            }
        }

        public synchronized void send(NetMessage msg) {
            kryo.writeClassAndObject(out, msg);
            out.flush();
        }

        private void handshake() throws IOException {
            //name = in.readLine();
            id = nextId.getAndIncrement();
            if (clients.isEmpty()) {
                host = this;
                this.isHost = true;
            }
            PlayerState p = new PlayerState();
            p.id = id;
            p.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

            this.playerState = p;

            clients.put(id, this);
            try {
                simulation.players.put(id, p);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
            //broadcastWalls();
        }

        public void disconnect() {
            System.out.println("Disconnecting!");
            clients.remove(id);
            simulation.removePlayer(id);
            //simulation.players.remove(id);
            if (this == host) {
                DisconnectMessage dm = new DisconnectMessage();
                dm.playerId = id;
                dm.reason = "Not specified";
                broadcast(dm);
                serverState = ServerState.SHUTDOWN;
                shutdown();
            } else {
                broadcastPlayerList();
            }
            try {
                socket.close();
                socket = null;
            } catch (IOException ignored) {}
        }
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

            if (serverState == ServerState.GAME) {
                //System.out.println("Tick (" + (counter++) + ")");
                processGameInputs();
                //updateProjectiles(delta);

                simulation.step(dt, 6, 2);

                syncBodiesToGameState();
                broadcastGameState();

            } else if (serverState == ServerState.LOBBY) {
                try {
                    processMessagesInLobby();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    serverState = ServerState.SHUTDOWN;
                }
            } else if (serverState == ServerState.LOADING) {
                //loadingScreen();
                processMessagesInLoadingScreen();
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

    private void processMessagesInLobby() throws InterruptedException {
        for (ClientHandler c : clients.values()) {
            String line;
            NetMessage msg;
            while ((msg = c.incoming.poll()) != null) {
                if (msg instanceof StartGameMessage) {
                    broadcast(new EnterLoadingMessage());
                    serverState = ServerState.LOADING;
                    simulation.initPhysics();
                    loadData();
                    spawnPlayers();
                    Thread.sleep(500);
                    sendInitialDataToAllClients();
                }
            }
//            while ((line = c.incoming.poll()) != null) {
//                //if (line.equals("START_GAME") && c == host) {
//                if (line.startsWith("START_GAME") && c == host) {
//                    broadcast("ENTER_LOADING");
//                    serverState = ServerState.LOADING;
//                    simulation.initPhysics();
//                    loadData();
//                    spawnPlayers();
//                    Thread.sleep(500);
//                    sendInitialDataToAllClients();
//                }
//            }
        }
    }

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
    private void processMessagesInLoadingScreen() {
        for (ClientHandler c : clients.values()) {
//            String line;
//            while ((line = c.incoming.poll()) != null) {
//                //if (line.equals("START_GAME") && c == host) {
//                if (line.equals("READY")) {
//                    readyClients.add(c.id);
//                    if (readyClients.size() == clients.size()) {
//                        startGame();
//                    }
//                }
//            }

            NetMessage msg;
            while ((msg = c.incoming.poll()) != null) {
                if (msg instanceof ReadyMessage) {
                    if (!((ReadyMessage) msg).ready) {
                        readyClients.remove(c);
                        continue;
                    }
                    readyClients.add(c.id);
                    if (readyClients.size() == clients.size()) {
                        startGame();
                    }
                }
            }
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

    private void processGameInputs() {
        for (ClientHandler c : clients.values()) {
//            String line;
//            while ((line = c.incoming.poll()) != null) {
//                //System.out.println("Processing input from player " + c.id + ": " + line);
//                if (line.startsWith("MOVE")) {
//                    String[] p = line.split(" ");
//                    int sequence = Integer.parseInt(p[1]);
//                    float dx = Float.parseFloat(p[2]);
//                    float dy = Float.parseFloat(p[3]);
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
//                }
//                else if (line.equals("STOP")) {
//                    Body body = c.playerState.body;
//                    if (body != null) {
//                        body.setLinearVelocity(0, 0);
//                    }
//                }
//                else if (line.startsWith("SHOOT")) {
//                    String[] p = line.split(" ");
//                    float dx = Float.parseFloat(p[1]);
//                    float dy = Float.parseFloat(p[2]);
//                    ProjectileState proj = new ProjectileState();
//                    proj.id = nextProjectileId++;
//                    //proj.ownerId = c.id;
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
//                else if (line.startsWith("MSG")) {
//                    broadcast(line);
//                }
//            }

            NetMessage msg;
            while((msg = c.incoming.poll()) != null) {
                if (msg instanceof PlayerInputMessage) {
                    PlayerInputMessage pim = (PlayerInputMessage) msg;
                    int sequence = pim.sequence;
                    float dx = pim.dx;
                    float dy = pim.dy;

                    Body body = c.playerState.body;
                    if (body == null) continue;

                    Vector2 desiredVelocity = new Vector2(dx, dy)
                        .nor()
                        .scl(PLAYER_SPEED_MPS);
                    body.setLinearVelocity(desiredVelocity);

                    c.lastProcessedInput = sequence;
                }
            }
        }
    }

    private void updateProjectiles(float delta) {
        Iterator<ProjectileState> it = simulation.projectiles.values().iterator();
        while (it.hasNext()) {
            ProjectileState ps = it.next();
            Body body = ps.body;
            if (body == null) continue;

            ps.lifeTime += delta;

//            Vector2 pos = body.getPosition();
//            ps.x = pos.x;
//            ps.y = pos.y;

//            Vector2 desiredVelocity = new Vector2(ps.vx, ps.vy).scl(BULLET_SPEED);
//            body.setLinearVelocity(desiredVelocity);

            if (ps.lifeTime >= ps.lifeTimeLimit || Math.abs(ps.x) > 100 || Math.abs(ps.y) > 100) {
                simulation.world.destroyBody(body);
                it.remove();
            }
        }
    }

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
