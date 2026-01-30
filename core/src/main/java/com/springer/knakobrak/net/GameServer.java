package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.world.PlayerStateDTO;
import com.springer.knakobrak.world.ProjectileState;
import com.springer.knakobrak.world.ServerPlayerState;
import com.badlogic.gdx.physics.box2d.*;

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
    private volatile boolean running = true;

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<Integer, ServerPlayerState> players = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    //private List<ProjectileState> projectiles = new ArrayList<>();
    private static Map<Integer, ProjectileState> projectiles = new ConcurrentHashMap<>();
    private int nextProjectileId = 1;

    private int port;

    World world;

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
        projectiles.get(projectileId).isDead = true;
        //damagePlayer(playerId);
        players.values().forEach(player -> {
            if (player.id == playerId) {
                player.hp--;
                System.out.println("Player " + playerId + " was damaged! Remaining HP: " + player.hp);
            }
        });
    }

    Body createPlayerBody(float x, float y, int playerId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(PLAYER_RADIUS_M); // meters

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0f;
        fd.restitution = 0f;

        Fixture f = body.createFixture(fd);
        f.setUserData(playerId);

        shape.dispose();

        return body;
    }

    Body createProjectile(float x, float y, float vx, float vy, int projId) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.bullet = true;
        bd.position.set(x, y);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(BULLET_RADIUS_M);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true; // IMPORTANT for bullets

        Fixture f = body.createFixture(fd);
        f.setUserData(projId);

        body.setLinearVelocity(vx, vy);

        shape.dispose();
        return body;
    }

    @Override
    public void run() {
        try {
            //serverSocket = new ServerSocket(port);
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
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean isHost;

        private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
        public ServerPlayerState serverPlayerState;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            //this.serverPlayerState = new ServerPlayerState();
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
            System.out.println("OI! " + clients.get(id).serverPlayerState + " , " + players.get(id));
            //out.println("ASSIGNED_ID " + id + " " + color.r + " " + color.g + " " + color.b);
            out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
        }

//        private void broadcastPlayerFire(String angle) {
//            String[] parts = angle.split(" ");
//            broadcast("SHOOT " + id + " " + parts[1] + " " + parts[2]);
//        }

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

//            world = new World(new Vector2(0, 0), true);
//            final float TIME_STEP = 1f / 60f;
//            float accumulator = 0f;

            if (serverState == ServerState.GAME) {
                System.out.println("Tick (" + (counter++) + ")");
                processGameInputs();
                updateProjectiles();

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
//        for (ServerPlayerState p : players.values()) {
//            //System.out.println(p.toString());
//            if (p.body == null) {
//                System.out.println("Warning: Player body is null for player ID " + p.id);
//                continue;
//            }
//            Vector2 pos = p.body.getPosition();
//            p.x = pos.x;
//            p.y = pos.y;
//        }

        for (ServerPlayerState p : players.values()) {
            if (p.body == null) continue;

            Vector2 pos = p.body.getPosition();
            p.x = pos.x;
            p.y = pos.y;
        }

        for (ProjectileState proj : projectiles.values()) {
            if (proj.body == null) continue;

            Vector2 pos = proj.body.getPosition();
            proj.x = pos.x;
            proj.y = pos.y;
        }
//        // Sync player bodies
//        for (ClientHandler c : clients.values()) {
//            Body body = c.playerState.body;
//            if (body != null) {
//                Vector2 pos = body.getPosition();
//                c.playerState.x = pos.x;
//                c.playerState.y = pos.y;
//            }
//        }
//
//        // Sync projectile bodies
//        for (ProjectileState p : projectiles) {
//            Body body = p.body;
//            if (body != null) {
//                Vector2 pos = body.getPosition();
//                p.x = pos.x;
//                p.y = pos.y;
//            }
//        }
    }

    private void processMessagesInLobby() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                if (line.equals("START_GAME") && c == host) {
                    serverState = ServerState.GAME;
                    initPhysics();
                    spawnPlayers();
                    broadcast("GAME_START");
                    System.out.println("Game started by host.");
                }
            }
        }
    }

    private void processGameInputs() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                System.out.println("Processing input from player " + c.id + ": " + line);
                if (line.startsWith("MOVE")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);

                    Body body = c.serverPlayerState.body;
                    if (body == null) continue;

                    Vector2 desiredVelocity = new Vector2(dx, dy).scl(PLAYER_SPEED_MPS);
                    body.setLinearVelocity(desiredVelocity);

//                    c.serverPlayerState.x += dx * speed;
//                    c.serverPlayerState.y += dy * speed;
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
                    ProjectileState proj = new ProjectileState();
                    proj.id = nextProjectileId++;
                    //proj.ownerId = c.id;

                    Vector2 spawnPos = c.serverPlayerState.body.getPosition()
                        .cpy()
                        .add(dx * (COLLISION_DISTANCE_M + 1.3f), dy * (COLLISION_DISTANCE_M + 1.3f));

                    proj.body = createProjectile(
                        spawnPos.x,
                        spawnPos.y,
                        dx * BULLET_SPEED_MPS,
                        dy * BULLET_SPEED_MPS,
                        proj.id
                    );

//                    proj.x = c.serverPlayerState.x + dx * 20;
//                    proj.y = c.serverPlayerState.y + dy * 20;
//                    proj.vx = dx * 400;
//                    proj.vy = dy * 400;
//                    proj.body = createProjectile(proj.x, proj.y, proj.vx, proj.vy, proj.id);
                    projectiles.put(proj.id, proj);
                    //projectiles.add(proj);
                }
            }
        }
    }

    private void updateProjectiles() {
        Iterator<ProjectileState> it = projectiles.values().iterator();
        while (it.hasNext()) {
            ProjectileState ps = it.next();
            Body body = ps.body;
            if (body == null) continue;

//            Vector2 pos = body.getPosition();
//            ps.x = pos.x;
//            ps.y = pos.y;

//            Vector2 desiredVelocity = new Vector2(ps.vx, ps.vy).scl(BULLET_SPEED);
//            body.setLinearVelocity(desiredVelocity);

            if (ps.isDead || Math.abs(ps.x) > 3000 || Math.abs(ps.y) > 3000) {
                world.destroyBody(body);
                it.remove();
            }
        }
    }

    private void broadcast(String msg) {
        clients.values().forEach(c -> c.out.println(msg));
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
        for (ProjectileState p : projectiles.values()) {
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
            c.serverPlayerState.x = 100 + i * 80;
            c.serverPlayerState.y = 200;
            c.serverPlayerState.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
            c.serverPlayerState.body = createPlayerBody(c.serverPlayerState.x, c.serverPlayerState.y, c.id);
            System.out.println(c.serverPlayerState.body);
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
        } catch (IOException ignored) {}
    }
}
