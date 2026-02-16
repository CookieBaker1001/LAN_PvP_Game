package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.springer.knakobrak.util.Constants;
import com.springer.knakobrak.world.PlayerStateDTO;
import com.springer.knakobrak.world.server.ServerProjectileState;
import com.springer.knakobrak.world.server.ServerPlayerState;
import com.badlogic.gdx.physics.box2d.*;
import com.springer.knakobrak.world.server.ServerWall;
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
    private static Map<Integer, ServerPlayerState> players = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private static Map<Integer, ServerProjectileState> projectiles = new ConcurrentHashMap<>();
    private int nextProjectileId = 1;

    World world;
    private static Array<ServerWall> walls = new Array<>();

    ArrayList<Vector2> playerSpawnPoints = new ArrayList<>();

    public GameServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    enum ServerState {
        LOBBY,
        GAME,
        SHUTDOWN
    }

    volatile ServerState serverState = ServerState.LOBBY;

    void initPhysics() {
        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                System.out.println("Collision detected");
                Object a = contact.getFixtureA().getUserData();
                Object b = contact.getFixtureB().getUserData();

                handleCollision(a, b);
            }

            public void endContact(Contact contact) {}
            public void preSolve(Contact contact, Manifold oldManifold) {}
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    void handleCollision(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            int idA = (int) a;
            int idB = (int) b;

            if (isProjectile(idA) && isPlayer(idB)) {
                hitPlayer(idB, idA);
            } else if (isProjectile(idB) && isPlayer(idA)) {
                hitPlayer(idA, idB);
            }
        }
    }

    boolean isPlayer(int id) {
        return players.containsKey(id);
    }

    boolean isProjectile(int id) {
        return projectiles.containsKey(id);
    }

    void hitPlayer(int playerId, int projectileId) {
        //removeProjectile(projectileId);
        //projectiles.get(projectileId).isDead = true;
        //damagePlayer(playerId);
        players.values().forEach(player -> {
            if (player.id == playerId) {
                player.hp--;
                broadcast("DAMAGE " + playerId + " _now_has " + player.hp + " HP.");
                //System.out.println("Player " + playerId + " was damaged! Remaining HP: " + player.hp);
            }
        });
    }

    Body createPlayerBody(float x, float y, int playerId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(PLAYER_RADIUS_M);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0f;
        fd.restitution = 0.4f;

        Fixture f = body.createFixture(fd);
        f.setUserData(playerId);

        shape.dispose();

        return body;
    }

    Body createProjectile(float x, float y/*, float vx, float vy*/, int projId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.bullet = true;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(BULLET_RADIUS_M);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0f;
        fd.restitution = 1f;

        //fd.isSensor = true; // IMPORTANT for bullets

        Fixture f = body.createFixture(fd);
        f.setUserData(projId);

        //body.setLinearVelocity(vx, vy);

        shape.dispose();
        return body;
    }

    Body createWall(float x, float y, int height, int width) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        //bd.position.set(x - width/2f, y - height/2f);
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);
        //shape.setAsBox(width, height);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 0f;
        fd.friction = 0f;
        fd.restitution = 0f;

        body.createFixture(fd);
        body.setFixedRotation(true);
        shape.dispose();
        return body;
    }

    Body createTestBox() {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;

        // position is CENTER of the box, in meters
        bd.position.set(2f + 0.5f, 2f + 0.5f);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();

        // setAsBox takes HALF-width and HALF-height (meters)
        shape.setAsBox(0.5f, 0.5f); // 1m × 1m total

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 0f;
        fd.friction = 0f;
        fd.restitution = 0f;

        body.createFixture(fd);
        shape.dispose();

        return body;
    }

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
        public ServerPlayerState serverPlayerState;

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
                    messageQueue.add(line); // thread-safe queue
                }
            } catch (IOException ignored) {
            } finally {
                disconnect();
            }
        }

        private void handshake() throws IOException {
            name = in.readLine();
            id = nextId.getAndIncrement();
            if (clients.isEmpty()) {
                host = this;
                host.isHost = true;
            }
            ServerPlayerState p = new ServerPlayerState();
            p.id = id;
            p.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);

            this.serverPlayerState = p;

            clients.put(id, this);
            players.put(id, p);
            out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
            //broadcastWalls();
        }

        private void disconnect() {
            clients.remove(id);
            players.remove(id);
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
                updateProjectiles(delta);

                world.step(dt, 6, 2);

                syncBodiesToGameState();
                broadcastGameState();


            } else if (serverState == ServerState.LOBBY) {
                processMessagesInLobby();
            }
            try {
                Thread.sleep(TICK_MS); // ~60 Hz
            } catch (InterruptedException ignored) {}
        }
    }

    private void syncBodiesToGameState() {
        for (ServerPlayerState p : players.values()) {
            if (p.body == null) continue;

            Vector2 pos = p.body.getPosition();
            p.x = pos.x;
            p.y = pos.y;
        }

        for (ServerProjectileState proj : projectiles.values()) {
            if (proj.body == null) continue;

            Vector2 pos = proj.body.getPosition();
            proj.x = pos.x;
            proj.y = pos.y;
        }
    }

    private void processMessagesInLobby() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                //if (line.equals("START_GAME") && c == host) {
                if (line.startsWith("START_GAME") && c == host) {
                    walls.clear();
                    playerSpawnPoints.clear();
                    serverState = ServerState.GAME;
                    initPhysics();
                    try {
                        int[][] grid = loadLevel("levels/level2.txt");
                        System.out.println();
                        for (int i = 0; i < grid.length; i++) {
                            for (int j = 0; j < grid[0].length; j++) {
                                System.out.print(grid[i][j]);
                            }
                            System.out.println();
                        }
                        generateWallsFromGrid(grid);
                    } catch (IOException e) {
                        System.out.println("Error loading level: " + e.getMessage());
                        e.printStackTrace();
                        continue;
                    }

                    //generateWorldWalls();

                    spawnPlayers();
                    broadcastWalls();
                    //broadcast("GAME_START");
                    System.out.println("Game started by host.");
                }
            }
        }
    }

    private void generateWorldWalls() {
        // Create walls around the play area
//        createWall(0, -(WORLD_HEIGHT * PIXELS_PER_METER) / 2, WORLD_HEIGHT * PIXELS_PER_METER, 10); // Bottom
//        createWall(0, (WORLD_HEIGHT * PIXELS_PER_METER) / 2, WORLD_HEIGHT * PIXELS_PER_METER, 10);  // Top
//        createWall(-(WORLD_WIDTH * PIXELS_PER_METER) / 2, 0, 10, WORLD_WIDTH * PIXELS_PER_METER);   // Left
//        createWall(WORLD_WIDTH * PIXELS_PER_METER / 2, 0, 10, WORLD_WIDTH * PIXELS_PER_METER);    // Right

        ServerWall testWall = new ServerWall();
        //testWall.body = createTestBox();
        testWall.body = createWall(1, 1, 1, 1);
        testWall.x = testWall.body.getPosition().x;
        testWall.y = testWall.body.getPosition().y;
        testWall.height = 1f;
        testWall.width = 1f;
        walls.add(testWall);

//        ServerWall wall = new ServerWall();
//        wall.body = createWall(1, 1, 1, 2); // Bottom
//        wall.x = wall.body.getPosition().x;
//        wall.y = wall.body.getPosition().y;
//        wall.height = 1f;
//        wall.width = 2f;
//        walls.add(wall);
        //createWall((int)WORLD_HEIGHT, 0, 20, 10); // Left

        // Additional internal walls can be created here if desired
    }

    static int[][] loadLevel(String path) throws IOException {
        List<int[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                int[] row = new int[parts.length];
                //for (int i = parts.length-1; i >= 0; i--) {
                for (int i = 0; i < parts.length; i++) {
                    row[i] = Integer.parseInt(parts[i]);
                }
                rows.add(row);
            }
        }

        int tmp[];
        for (int i = 0; i < rows.size()/2; i++) {
            tmp = rows.get(i);
            rows.set(i, rows.get(rows.size()-i-1));
            rows.set(rows.size()-i-1, tmp);
        }

        return rows.toArray(new int[0][]);
    }

    private void generateWallsFromGrid(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        boolean[][] used = new boolean[rows][cols];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                if (grid[y][x] == 0 || used[y][x]) continue;
                if (grid[y][x] == 2) {
                    playerSpawnPoints.add(new Vector2(x, y));
                    continue;
                }

                int width = 1;
                while (x + width < cols &&
                    grid[y][x + width] == 1 &&
                    !used[y][x + width]) {
                    width++;
                }

                // ---- Expand vertically ----
                int height = 1;
                boolean canExpand = true;
                while (y + height < rows && canExpand) {

                    for (int i = 0; i < width; i++) {
                        if (grid[y + height][x + i] == 0 ||
                            used[y + height][x + i]) {
                            canExpand = false;
                            break;
                        }
                    }

                    if (canExpand) height++;
                }

                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        used[y + dy][x + dx] = true;
                    }
                }

                createMergedWall(x, y, width, height);

//                if (grid[y][x] == 1) {
////                    float centerX = (x + 0.5f);
////                    float centerY = (y + 0.5f);
//
//                    Body b = createWall(
//                        x,
//                        y,
//                        1,
//                        1
//                    );
//
//                    ServerWall wall = new ServerWall();
//                    wall.body = b;
//                    wall.x = wall.body.getPosition().x;
//                    wall.y = wall.body.getPosition().y;
//                    wall.height = 1f;
//                    wall.width = 1f;
//                    walls.add(wall);
//                }
            }
        }
    }

    private void createMergedWall(int x, int y, int w, int h) {

        float centerX = (x + w / 2f);
        float centerY = (y + h / 2f);

        ServerWall wall = new ServerWall();
        wall.body = createWall(centerX, centerY, h, w);
        wall.x = wall.body.getPosition().x;
        wall.y = wall.body.getPosition().y;
        wall.height = h;
        wall.width = w;
        walls.add(wall);
    }

    private void processGameInputs() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                //System.out.println("Processing input from player " + c.id + ": " + line);
                if (line.startsWith("MOVE")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);

                    Body body = c.serverPlayerState.body;
                    if (body == null) continue;

                    Vector2 desiredVelocity = new Vector2(dx, dy)
                        .nor()
                        .scl(PLAYER_SPEED_MPS);
                    body.setLinearVelocity(desiredVelocity);
                }
                if (line.equals("STOP")) {
                    Body body = c.serverPlayerState.body;
                    if (body != null) {
                        body.setLinearVelocity(0, 0);
                    }
                }
                if (line.startsWith("SHOOT")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);
                    ServerProjectileState proj = new ServerProjectileState();
                    proj.id = nextProjectileId++;
                    //proj.ownerId = c.id;
                    Vector2 dir = new Vector2(dx, dy).nor();

                    Vector2 spawnPos = c.serverPlayerState.body.getPosition()
                        .cpy()
                        .add(dir.scl(BULLET_SPAWN_OFFSET_M));

                    proj.body = createProjectile(
                        spawnPos.x,
                        spawnPos.y,
                        proj.id
                    );

                    proj.body.setLinearVelocity(
                        dir.scl(BULLET_SPEED_MPS)
                    );

                    projectiles.put(proj.id, proj);
                }
                if (line.startsWith("MSG")) {
                    broadcast(line);
                }
            }
        }
    }

    private void updateProjectiles(float delta) {
        Iterator<ServerProjectileState> it = projectiles.values().iterator();
        while (it.hasNext()) {
            ServerProjectileState ps = it.next();
            Body body = ps.body;
            if (body == null) continue;

            ps.lifeTime += delta;

//            Vector2 pos = body.getPosition();
//            ps.x = pos.x;
//            ps.y = pos.y;

//            Vector2 desiredVelocity = new Vector2(ps.vx, ps.vy).scl(BULLET_SPEED);
//            body.setLinearVelocity(desiredVelocity);

            if (ps.lifeTime >= ps.lifeTimeLimit || Math.abs(ps.x) > 100 || Math.abs(ps.y) > 100) {
                world.destroyBody(body);
                it.remove();
            }
        }
    }

    private void broadcast(String msg) {
        clients.values().forEach(c -> c.out.println(msg));
    }

    private void broadcastWalls() {
        StringBuilder sb = new StringBuilder("GAME_START");
        for (ServerWall w : walls) {
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
            PlayerStateDTO s = PlayerStateDTO.FromServerToDTO(c.serverPlayerState);
            sb.append(" ")
                .append(s.id).append(":")
                .append(s.x).append(":")
                .append(s.y).append(":")
                .append(s.color.r).append(":")
                .append(s.color.g).append(":")
                .append(s.color.b);
        }
        sb.append(" PR");
        for (ServerProjectileState p : projectiles.values()) {
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
            c.serverPlayerState.id = c.id;
            c.serverPlayerState.x = playerSpawnPoints.get(i).x + 0.5f;
            c.serverPlayerState.y = playerSpawnPoints.get(i).y + 0.5f;
            c.serverPlayerState.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
            c.serverPlayerState.body = createPlayerBody(c.serverPlayerState.x, c.serverPlayerState.y, c.id);
            i++;
        }
    }

    public void shutdown() {
        running = false;
        clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        players.clear();
        host = null;
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (IOException ignored) {}
    }
}
