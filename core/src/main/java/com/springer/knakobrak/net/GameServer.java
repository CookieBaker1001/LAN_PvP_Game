package com.springer.knakobrak.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer implements Runnable {

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private ClientHandler host;
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

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

    public void shutdown() {
        running = false;
        clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        host = null;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
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

    private class ClientHandler implements Runnable {

        private int id;
        private String name;
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean isHost;

        PlayerState playerState;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                handshake();
                while (serverState != ServerState.SHUTDOWN) {
                    if (serverState == ServerState.LOBBY) {
                        lobbyLoop();
                    } else if (serverState == ServerState.GAME) {
                        gameLoop();
                    }
                }
            } catch (IOException ignored) {
            } finally {
                disconnect();
            }
        }

        private void handshake() throws IOException {
            //out.println("ENTER_NAME");
            name = in.readLine();
            id = nextId.getAndIncrement();
            if (clients.isEmpty()) {
                host = this;
                host.isHost = true;
            }
            clients.put(id, this);
            out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
        }

        private void lobbyLoop() throws IOException {
            String line;
            while (serverState == ServerState.LOBBY && (line = in.readLine()) != null) {
                if (line.equals("QUIT")) {
                    break;
                }
                if (line.equals("START_GAME") && this == host) {
                    serverState = ServerState.GAME;
                    spawnPlayers();
                    broadcastGameState();
                    broadcast("GAME_START");
                }
            }
        }

        private void gameLoop() throws IOException {
            String line;
            while (serverState == ServerState.GAME && (line = in.readLine()) != null) {
                if (line.equals("QUIT")) {
                    return;
                }
                if (line.startsWith("MOVE")) {
                    String[] parts = line.split(" ");
                    float dx = Float.parseFloat(parts[1]);
                    float dy = Float.parseFloat(parts[2]);
                    float SPEED = Float.parseFloat(parts[3]);

//                    Player p = players.get(this);
//                    p.x += dx * SPEED;
//                    p.y += dy * SPEED;
                    playerState.x += dx * SPEED;
                    playerState.y += dy * SPEED;
                    broadcastGameState();
                }
            }
        }

        private void disconnect() {
            clients.remove(id);
            broadcastPlayerList();
            if (this == host) {
                broadcast("HOST_LEFT");
                serverState = ServerState.SHUTDOWN;
                shutdown();
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void broadcastGameState() {
        StringBuilder sb = new StringBuilder("STATE");
        for (ClientHandler c : clients.values()) {
            PlayerState s = c.playerState;
            sb.append(" ")
                .append(s.id).append(":")
                .append(s.x).append(":")
                .append(s.y);
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
            i++;
        }
    }

    static class PlayerState {
        int id;
        float x, y;
    }
}
