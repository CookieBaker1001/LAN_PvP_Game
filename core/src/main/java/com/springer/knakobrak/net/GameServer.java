package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;
import com.springer.knakobrak.world.client.Wall;
import com.badlogic.gdx.physics.box2d.*;
//import com.sun.security.ntlm.Server.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        private BufferedReader in;
        private PrintWriter out;
        private boolean isHost;

        private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
        public PlayerState playerState;

        public int lastProcessedInput = 0;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                handshake();
                String line;
                while ((line = in.readLine()) != null) {
                    //System.out.println("Hi");
                    messageQueue.add(line); // thread-safe queue
                }
                //System.out.println("Done...");
            } catch (IOException e) {
                //System.out.println("Something went wrong!");
                e.printStackTrace();
            } finally {
                //System.out.println("Finally...");
                disconnect();
            }
        }

        public void send(String msg) {
            out.println(msg);
        }

        private void handshake() throws IOException {
            name = in.readLine();
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
            out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
            //broadcastWalls();
        }

        private void disconnect() {
            System.out.println("Disconnecting!");
            clients.remove(id);
            simulation.removePlayer(id);
            //simulation.players.remove(id);
            if (this == host) {
                broadcast("HOST_LEFT");
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
            while ((line = c.messageQueue.poll()) != null) {
                //if (line.equals("START_GAME") && c == host) {
                if (line.startsWith("START_GAME") && c == host) {
                    broadcast("ENTER_LOADING");
                    serverState = ServerState.LOADING;
                    simulation.initPhysics();
                    loadData();
                    spawnPlayers();
                    Thread.sleep(500);
                    sendInitialDataToAllClients();
                }
            }
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
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                //if (line.equals("START_GAME") && c == host) {
                if (line.equals("READY")) {
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
        broadcast("START_GAME");
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
            c.send("INIT_PLAYER " + p.id + " " + p.x + " " + p.y);
        }

        // Send wall map
        sendWallLayout(c);

        c.send("INIT_DONE");
    }

    void sendWallLayout(ClientHandler c) {

        int mapWidth = (int) (worldWidth * PIXELS_PER_METER);
        int mapHeight = (int) (worldHeight * PIXELS_PER_METER);

        c.send("INIT_MAP " + mapWidth + " " + mapHeight);

        StringBuilder sb = new StringBuilder("INIT_MAP_WALLS");
        for (Wall w : simulation.walls) {
            sb.append(" ").append(w.x)
                .append(" ").append(w.y)
                .append(" ").append(w.width)
                .append(" ").append(w.height);
        }
        c.send(sb.toString());

//        for (int y = 0; y < mapHeight; y++) {
//            StringBuilder row = new StringBuilder("INIT_MAP_ROW ");
//            row.append(y);
//
//            for (int x = 0; x < mapWidth; x++) {
//                row.append(" ").append(map[y][x]);
//            }
//
//            c.send(row.toString());
//        }
    }

    private void processGameInputs() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                //System.out.println("Processing input from player " + c.id + ": " + line);
                if (line.startsWith("MOVE")) {
                    String[] p = line.split(" ");
                    int sequence = Integer.parseInt(p[1]);
                    float dx = Float.parseFloat(p[2]);
                    float dy = Float.parseFloat(p[3]);

                    Body body = c.playerState.body;
                    if (body == null) continue;

                    Vector2 desiredVelocity = new Vector2(dx, dy)
                        .nor()
                        .scl(PLAYER_SPEED_MPS);
                    body.setLinearVelocity(desiredVelocity);

                    c.lastProcessedInput = sequence;
                }
                else if (line.equals("STOP")) {
                    Body body = c.playerState.body;
                    if (body != null) {
                        body.setLinearVelocity(0, 0);
                    }
                }
                else if (line.startsWith("SHOOT")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);
                    ProjectileState proj = new ProjectileState();
                    proj.id = nextProjectileId++;
                    //proj.ownerId = c.id;
                    Vector2 dir = new Vector2(dx, dy).nor();

                    Vector2 spawnPos = c.playerState.body.getPosition()
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
                else if (line.startsWith("MSG")) {
                    broadcast(line);
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

    private void broadcast(String msg) {
        clients.values().forEach(c -> c.out.println(msg));
    }

    private void broadcastWalls() {
        StringBuilder sb = new StringBuilder("GAME_START");
        for (Wall w : simulation.walls) {
            sb.append(" ").append(w.x)
                .append(" ").append(w.y)
                .append(" ").append(w.width)
                .append(" ").append(w.height);
        }
        broadcast(sb.toString());
    }

    private void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYER_LIST ");
        for (ClientHandler c : clients.values()) {
            sb.append(c.id).append(":").append(c.name);
            if (c.isHost) sb.append(" (HOST)");
            sb.append("_");
        }
        broadcast(sb.toString());
    }

    private void broadcastGameState() {
        StringBuilder sb = new StringBuilder("STATE P");
        for (ClientHandler c : clients.values()) {
            //PlayerStateDTO s = PlayerStateDTO.FromServerToDTO(c.playerState);
            sb.append(" ")
                .append(c.id).append(":")
                .append(c.playerState.x).append(":")
                .append(c.playerState.y);
//                .append(":")
//                .append(c.playerState.color.r).append(":")
//                .append(c.playerState.color.g).append(":")
//                .append(c.playerState.color.b);
        }
        sb.append(" PR");
        for (ProjectileState p : simulation.projectiles.values()) {
            sb.append(" ");
            sb.append(p.id).append(":")
                .append(p.x).append(":")
                .append(p.y);//.append(":")
                //.append(p.vx).append(":")
                //.append(p.vy);
        }
        broadcast(sb.toString());
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
