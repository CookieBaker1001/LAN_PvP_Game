package com.springer.knakobrak.net.multiplayerEntities;

import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.*;
import com.springer.knakobrak.world.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class OpenGame_Server implements Server {

    private ServerSocket serverSocket;
    private Thread gameLoopThread;
    private ServerType serverType;
    private ServerState serverState;
    private int port;
    private long hostKey;
    private volatile boolean running;
    private volatile boolean shutdownRequested;

    private IdPool idPool;
    private ClientHandler host;
    private final static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private PhysicsSimulation simulation;

    private float serverTime;
    private long serverStartTime;

    public OpenGame_Server(int port, long hostKey) throws IOException {
        this.port = port;
        this.hostKey = hostKey;

        serverType = ServerType.OPEN;
        serverSocket = new ServerSocket(port);
        running = true;
        shutdownRequested = false;
        idPool = new IdPool();
        simulation = new PhysicsSimulation("Server");
        simulation.wallGrid = LoadUtillities.loadLevel("levels/level1.txt");

        serverState = ServerState.GAME;
        serverStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            gameLoopThread = new Thread(this::gameLoop, "Open game loop");
            gameLoopThread.start();
            while(running) {
                Socket socket = serverSocket.accept();
                if (!running) break;

                ClientHandler client = new ClientHandler(this, socket);
                Thread clientThread = new Thread(client);
                clientThread.start();

                System.out.println("New client connected!");
                if (shutdownRequested) running = false;
            }
        } catch (IOException e) {
            System.out.println("SERVER ERROR: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private int secondCounter = 0;
    private float accumulator_1 = 0f;
    private float accumulator_5 = 0f;

    private final long TICK_MS = 16;
    private float SERVER_TICK_SPEED = 1f / 60f;

    private float broadCastAccumulator = 0f;
    private final float broadCastRate = 1 / 20f;

    private void gameLoop() {
        while (running) {
            processServerMessages();
            accumulator_1 += SERVER_TICK_SPEED;
            accumulator_5 += SERVER_TICK_SPEED;
            broadCastAccumulator += SERVER_TICK_SPEED;
            if (accumulator_1 >= 1f) {
                accumulator_1 -= 1f;
                secondCounter++;
                doSomethingEverySecond();
            }
            if (accumulator_5 >= 5f) {
                accumulator_5 -= 5f;
                doSomethingEvery5Seconds();
            }
            if (broadCastAccumulator >= broadCastRate) {
                broadcastGameState();
                broadCastAccumulator -= broadCastRate;
            }
            serverTime += SERVER_TICK_SPEED;
            try {
                Thread.sleep(TICK_MS);
            } catch (InterruptedException e) {}
        }
    }

    private void doSomethingEverySecond() {
        System.out.println("[Time]: " + secondCounter);
        pingClients();
    }

    private void pingClients() {
        PingMessage pm = new PingMessage();
        pm.secuence = secondCounter;
        clients.values().forEach(c -> {
            pm.pingTime = System.currentTimeMillis();
            c.send(pm);
        });
    }

    private void doSomethingEvery5Seconds() {
        updatePingList();
    }

    private void updatePingList() {
        PlayerListMessage plm = PlayerListItem.constructPlayerList(clients);
        broadcast(plm);
    }

    void processServerMessages() {
        ServerMessage sm;
        while ((sm = inbox.poll()) != null) {
            dispatchMessage(sm.sender, sm.message);
        }
    }

    private void dispatchMessage(ClientHandler sender, NetMessage msg) {
        switch (msg) {
            case PlayerWASDMessage pwasdm -> handlePlayerWASDMessage(sender, pwasdm);
            case ChatMessage cm -> handleChatMessage(sender, cm);
            case JoinMessage jm -> handleJoinMessage(sender, jm);
            case LeaveGameMessage lgm -> handleLeaveGameMessage(sender, lgm);
            case GetPlayerDataMessage gpdm -> handleGetPlayerDataMessage(sender, gpdm);
            case GetMapDataMessage gmdm -> handleGetMapDataMessage(sender, gmdm);
            case AllResourcesLoadedMessage arlm -> handleAllResourcesLoadedMessage(sender, arlm);
            case PingResponseMessage prm -> handlePingResponseMessage(sender, prm);
            default -> {
                System.out.println("Unknown message format: " + msg.getClass());
            }
        }
    }

    private void handlePingResponseMessage(ClientHandler sender, PingResponseMessage prm) {
        clients.get(prm.clientId).ping = (System.currentTimeMillis() - prm.pingTimeResponse);
    }

    private void handleLeaveGameMessage(ClientHandler sender, LeaveGameMessage lgm) {
        System.out.println("Someone is leaving!");
        LeaveAcceptMessage lam = new LeaveAcceptMessage();
        lam.message = "Acknowledging leave request with reason '" + lgm.reason + "'.";
        ChatMessage cm = new ChatMessage();
        cm.message = "<SERVER>" + sender.username + " left the game.";
        simulation.removePlayer(sender.id);
        sender.sendDisconnect(lam);
        disconnectClient(sender);
        broadcast(cm);
        broadcast(PlayerListItem.constructPlayerList(clients));
        idPool.release(sender.id);
    }

    private void handleGetMapDataMessage(ClientHandler sender, GetMapDataMessage gmdm) {
        MapDataMessage mdm = new MapDataMessage();
        mdm.wallBits = simulation.wallGrid;
        sender.send(mdm);
    }

    private void handleGetPlayerDataMessage(ClientHandler sender, GetPlayerDataMessage gpdm) {
        PlayerStateMessage psm = new PlayerStateMessage();
        int s = idPool.getConnectedPlayers();
        psm.ids = new int[s];
        psm.x = new float[s];
        psm.y = new float[s];
        int i = 0;
        for (Player p : simulation.players.values()) {
            psm.ids[i] = p.id;
            psm.x[i] = p.realX;
            psm.y[i] = p.realY;
            i++;
        }
        sender.send(psm);
    }

    // Method triggers once a client has loaded all resources and can enter the GameScreen
    private void handleAllResourcesLoadedMessage(ClientHandler sender, AllResourcesLoadedMessage arlm) {
        System.out.println("Handle all resources loaded message");
        AllResourcesLoadedAcknowledgedMessage arlam = new AllResourcesLoadedAcknowledgedMessage();
        arlam.serverStartTime = serverStartTime;
        sender.send(arlam);

        Player p = new Player();
        p.id = sender.id;
        simulation.addPlayer(sender.id, p);

        ChatMessage cm = new ChatMessage();
        cm.message = "<SERVER>" + sender.username + " joined the game.";
        broadcast(cm);

        PlayerListMessage plm = PlayerListItem.constructPlayerList(clients);
        broadcast(plm);

        WorldStateMessage wsm = constructWorldStateMessage();
        broadcast(wsm);
    }

    private void handleJoinMessage(ClientHandler sender, JoinMessage jm) {
        System.out.println(jm.username + " (v." + jm.protocolVersion + ") just joined!");
        simulation.playerStates();
        int id = idPool.acquire();
        if (id == -1) {
            denyClientEntry(sender);
            return;
        }
        if (jm.key == hostKey) {
            if (host != null) {
                host.isHost = false;
            }
            host = sender;
            sender.isHost = true;
        } else {
            sender.isHost = false;
        }
        sender.id = id;
        sender.username = jm.username;
        clients.put(id, sender);

        JoinAcceptMessage jam = new JoinAcceptMessage();
        jam.id = id;
        jam.isHost = sender.isHost;
        jam.serverType = serverType.ordinal();
        sender.send(jam);
        simulation.playerStates();
        //broadcastPlayerList();
    }

    private void denyClientEntry(ClientHandler sender) {
        JoinRejectedMessage jrm = new JoinRejectedMessage();
        jrm.reason = "Too many players";
        sender.send(jrm);
    }

    private void handlePlayerWASDMessage(ClientHandler sender, PlayerWASDMessage pwasdm) {
        Player p = simulation.getPlayer(sender.id);
        if (p == null) return;
        p.realX = pwasdm.x;
        p.realY = pwasdm.y;
    }

    void handleChatMessage(ClientHandler sender, ChatMessage cm) {
        cm.message = "<" + sender.username + ">" + cm.message;
        broadcast(cm);
    }

    private void broadcastGameState() {
        WorldStateMessage wsm = constructWorldStateMessage();
        broadcast(wsm);
    }

    int c = 0;
    private WorldStateMessage constructWorldStateMessage() {
        WorldStateMessage wsm = new WorldStateMessage();
        int s = idPool.getConnectedPlayers();
        wsm.ids = new int[s];
        wsm.x = new float[s];
        wsm.y = new float[s];
        int i = 0;
        for (Player p : simulation.players.values()) {
            wsm.ids[i] = p.id;
            wsm.x[i] = p.realX;
            wsm.y[i] = p.realY;
            i++;
        }
        c++;
        if (c >= 20) {
            String st = "World state looks like this: ";
            for (Player p : simulation.players.values()) {
                st += "ID: " + p.id + ", (" + p.realX + "," + p.realY + ")";
            }
            //System.out.println(st);
            c -= 20;
        }
        return wsm;
    }

    @Override
    public void enqueue(ServerMessage sm) {
        inbox.add(sm);
    }

    private void disconnectClient(ClientHandler client) {
        clients.remove(client.id);
    }

    @Override
    public void broadcast(NetMessage msg) {
        clients.values().forEach(c -> {
            try {
                c.send(msg);
            } catch (Exception e) {
                System.err.println("FAILED TO SEND TO CLIENT " + c.id + ", (" + e.getMessage() + ")");
                System.out.println("-Start of stacktrace-");
                e.printStackTrace();
                System.out.println("-End of stacktrace-");
            }
        });
    }

    @Override
    public void shutdown() {
        System.out.println("Server is shutting down!");
        LeaveAcceptMessage lam = new LeaveAcceptMessage();
        lam.message = "Server is shutting down. Time to leave!";
        broadcast(lam);
        clients.values().forEach(c -> {
            c.sendDisconnect(lam);
        });
        host = null;
        running = false;
        clients.clear();
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}
