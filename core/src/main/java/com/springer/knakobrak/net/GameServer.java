package com.springer.knakobrak.net;

import com.springer.knakobrak.world.PlayerState;

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

    @Override
    public void run() {
        try {
            //serverSocket = new ServerSocket(port);
            System.out.println("Game server started on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                new Thread(client).start();
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

    public void shutdown() {
        running = false;
        clients.values().forEach(ClientHandler::disconnect);
        clients.clear();
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
//        try {
//            if (serverSocket != null && !serverSocket.isClosed()) {
//                serverSocket.close();
//            }
//        } catch (IOException ignored) {}
//        List<ClientHandler> snapshot;
//        synchronized (clients) {
//            snapshot = new ArrayList<>(clients.values());
//        }
//        for (ClientHandler client : snapshot) {
//            client.out.println("Server is shutting down.");
//            client.disconnect();
//        }
//        host = null;
//        clients.clear();
    }

    private void broadcast(String msg) {
        clients.values().forEach(c -> c.out.println(msg));
    }

    private void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYER_LIST ");
        for (ClientHandler c : clients.values()) {
            sb.append(c.id).append(":").append(c.name).append(" ");
        }
        broadcast(sb.toString());
    }

//    private void broadcast(String msg, ClientHandler from) {
//        synchronized (clients) {
//            broadcastRaw(msg, from);
//        }
//        System.out.println(msg);
//    }
//
//    private static void broadcastRaw(String message, ClientHandler from) {
//        for (ClientHandler client : clients.values()) {
////            if (from == null) client.out.println(message);
////            else if (client != from) client.out.println(message);
//            client.out.println(message);
//        }
//    }

    private class ClientHandler implements Runnable {

        private int id;
        private String name;
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean isHost;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                handshake();
                lobbyLoop();
            } catch (IOException ignored) {
            } finally {
                disconnect();
            }
        }

        private void handshake() throws IOException {
            out.println("ENTER_NAME");
            name = in.readLine();
            id = nextId.getAndIncrement();
            if (clients.isEmpty()) host = this;
            clients.put(id, this);
            out.println("ASSIGNED_ID " + id);
            broadcastPlayerList();
        }

        private void lobbyLoop() throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("QUIT")) {
                    break;
                }
                if (line.equals("START_GAME") && this == host) {
                    broadcast("GAME_START");
                }
            }
        }

        private void disconnect() {
            clients.remove(id);
            broadcastPlayerList();
            if (this == host) {
                broadcast("HOST_LEFT");
                shutdown();
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
