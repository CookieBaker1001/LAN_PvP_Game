package com.springer.knakobrak.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.ProjectileState;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer implements Runnable {

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<Integer, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private List<ProjectileState> projectiles = new ArrayList<>();
    private int nextProjectileId = 1;

    private int port;

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
        PlayerState playerState;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.playerState = new PlayerState();
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
            this.playerState.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
            clients.put(id, this);
            playerStates.put(id, this.playerState);
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
            playerStates.remove(id);
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
        while (serverState != ServerState.SHUTDOWN) {
            now = System.nanoTime();
            delta = (now - last) / 1_000_000_000f;
            last = now;
            if (serverState == ServerState.GAME) {
                processGameInputs();
                updateProjectiles(delta);
                broadcastGameState();
            } else if (serverState == ServerState.LOBBY) {
                processMessagesInLobby();
            }
            try {
                Thread.sleep(TICK_MS); // ~60 Hz
            } catch (InterruptedException ignored) {}
        }
    }

    private void processMessagesInLobby() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                if (line.equals("START_GAME") && c == host) {
                    serverState = ServerState.GAME;
                    spawnPlayers();
                    //broadcastStartGame();
                    broadcast("GAME_START");
                    System.out.println("Game started by host.");
                }
            }
        }
    }

    private void broadcastStartGame() {
        StringBuilder sb = new StringBuilder("GAME_START");
        for (PlayerState p : playerStates.values()) {
            System.out.println("Player " + p.id + " has color: " + p.color);
            sb.append(" ").append(p.id).append(":")
                .append(p.color.r).append(":")
                .append(p.color.g).append(":")
                .append(p.color.b);
        }
        System.out.println(sb);
        broadcast(sb.toString());
    }

    private void processGameInputs() {
        for (ClientHandler c : clients.values()) {
            String line;
            while ((line = c.messageQueue.poll()) != null) {
                if (line.startsWith("MOVE")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);
                    float speed = Float.parseFloat(p[3]);
                    c.playerState.x += dx * speed;
                    c.playerState.y += dy * speed;
                }
                if (line.startsWith("SHOOT")) {
                    String[] p = line.split(" ");
                    float dx = Float.parseFloat(p[1]);
                    float dy = Float.parseFloat(p[2]);
                    ProjectileState proj = new ProjectileState();
                    proj.id = nextProjectileId++;
                    //proj.ownerId = c.id;
                    proj.x = c.playerState.x + dx * 20;
                    proj.y = c.playerState.y + dy * 20;
                    proj.vx = dx * 400;
                    proj.vy = dy * 400;
                    projectiles.add(proj);
                }
            }
        }
    }

    private void updateProjectiles(float delta) {
        Iterator<ProjectileState> it = projectiles.iterator();
        while (it.hasNext()) {
            ProjectileState p = it.next();
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            if (Math.abs(p.x) > 3000 || Math.abs(p.y) > 3000) {
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
            PlayerState s = c.playerState;
            sb.append(" ")
                .append(s.id).append(":")
                .append(s.x).append(":")
                .append(s.y).append(":")
                .append(s.color.r).append(":")
                .append(s.color.g).append(":")
                .append(s.color.b);
        }
        sb.append(" PR");
        for (ProjectileState p : projectiles) {
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
            c.playerState = new PlayerState();
            c.playerState.id = c.id;
            c.playerState.x = 100 + i * 80;
            c.playerState.y = 200;
            c.playerState.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
            i++;
        }
    }

    public void shutdown() {
        running = false;
        clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        playerStates.clear();
        host = null;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
}
