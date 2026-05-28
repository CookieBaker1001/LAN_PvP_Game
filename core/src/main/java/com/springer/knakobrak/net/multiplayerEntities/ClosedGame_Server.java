package com.springer.knakobrak.net.multiplayerEntities;

import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.*;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.Player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ClosedGame_Server implements Server {

    private ServerSocket serverSocket;
    private Thread gameLoopThread;
    private ServerType serverType = ServerType.LOBBIED;
    private int port;
    private long hostKey;
    private volatile boolean running;
    private volatile boolean shutdownRequested;

    private ClientHandler host;
    private final static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();

    private IdPool idPool;

    private ServerState serverState;

    private PhysicsSimulation simulation;

    private float serverTime;
    private long serverStartTime;

    public ClosedGame_Server(int port, long key) throws IOException {
        this.port = port;
        this.hostKey = key;

        serverType = ServerType.LOBBIED;
        serverSocket = new ServerSocket(this.port);
        running = true;
        shutdownRequested = false;
        idPool = new IdPool();
        simulation = new PhysicsSimulation("Server");
        simulation.wallGrid = LoadUtilities.loadLevel("levels/level1.txt");

        serverState = ServerState.LOBBY;
        serverStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            gameLoopThread = new Thread(this::gameLoop, "Lobbied game loop");
            gameLoopThread.start();
            while (running) {
                Socket socket = serverSocket.accept();
                if (!running) break;

                ClientHandler client = new ClientHandler(this, socket);
                Thread clientThread = new Thread(client);
                clientThread.start();

                System.out.println("New client connected!");
                if (shutdownRequested) running = false;
            }
        } catch (Exception e) {
            System.out.println("SERVER ERROR: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private boolean everyOneIsLoaded = false;

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
            if (serverState == ServerState.GAME) {
                accumulator_1 += SERVER_TICK_SPEED;
                accumulator_5 += SERVER_TICK_SPEED;
                broadCastAccumulator += SERVER_TICK_SPEED;

                if (accumulator_1 >= 1f) {
                    accumulator_1 = accumulator_1 - 1f;
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
            }
            else if (serverState == ServerState.LOADING) {
                tryToGoFormLoadingToGameScreen();
            }
            try {
                Thread.sleep(TICK_MS);
            } catch (InterruptedException e) {}
        }
    }

    private void broadcastGameState() {
        WorldStateMessage wsm = constructWorldStateMessage();
        broadcast(wsm);
    }

    Map<ClientHandler, Boolean> readyClients = new HashMap<ClientHandler, Boolean>();
    private void tryToGoFormLoadingToGameScreen() {
        everyOneIsLoaded = getEveryOneIsLoadedStatus();
        if (!everyOneIsLoaded) return;
        moveToNewServerState(ServerState.GAME);
        AllResourcesLoadedAcknowledgedMessage dlam = new AllResourcesLoadedAcknowledgedMessage();
        broadcast(dlam);
    }

    private boolean getEveryOneIsLoadedStatus() {
        boolean status = true;
        for (Boolean b : readyClients.values()) {
            if (!b) {
                status = false;
                break;
            }
        }
        return status;
    }

    private void moveToNewServerState(ServerState state) {
        if (state == ServerState.LOADING) {
            everyOneIsLoaded = false;
        } else if (state == ServerState.GAME) {
        }
        serverState = state;
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
        PlayerListMessage plm = PlayerListItem.constructPlayerList(clients);
        broadcast(plm);
    }

    private void processServerMessages() {
        ServerMessage sm;
        while ((sm = inbox.poll()) != null) {
            messageDispatcher(sm.sender, sm.message);
        }
    }

    private void messageDispatcher(ClientHandler sender, NetMessage msg) {
        System.out.println("Server is in state: " + serverState);
        switch (serverState) {
            case LOBBY: {handleLobbyMessages(sender, msg);}
            case LOADING: {handleLoadingMessages(sender, msg);}
            case GAME: {handleGameMessages(sender, msg);}
        }
    }

    private void handleLobbyMessages(ClientHandler sender, NetMessage msg) {
        System.out.println("[ClosedGame_Server(Lobby message)]");
        switch (msg) {
            case JoinMessage jm -> handleJoinMessage(sender, jm);
            case LeaveLobbyMessage llm -> handleLeaveLobbyMessage(sender, llm);
            case ReadyMessage rm -> handleReadyMessage(sender, rm);
            case EveryOneIsReadyMessage eirm -> handleEveryOneIsReadyMessage(eirm);
            //case LeaveGameMessage lgm -> handleLeaveGameMessage(sender, lgm);
            default -> {
                System.out.println("[ClosedGame_Server(Lobby message)]: Unknown message type: " + msg.getClass());
            }
        }
    }

    private void handleReadyMessage(ClientHandler sender, ReadyMessage rm) {
        sender.ready = rm.ready;
        boolean allReady = getReadyStatus();
        GameCanStartStatusMessage gcsm = new GameCanStartStatusMessage();
        gcsm.canStart = allReady;
        host.send(gcsm);
    }

    private void handleEveryOneIsReadyMessage(EveryOneIsReadyMessage eirm) {
        moveToNewServerState(ServerState.LOADING);
        StartGameMessage sgm = new StartGameMessage();
        broadcast(sgm);
    }

    private void handleLeaveLobbyMessage(ClientHandler sender, LeaveLobbyMessage llm) {
        LeaveAcceptMessage lam = new LeaveAcceptMessage();
        lam.message = "Acknowledging leave request with reason '" + llm.reason + "'.";
        sender.sendDisconnect(lam);
        disconnectClient(sender);
        broadcast(PlayerListItem.constructPlayerList(clients));
        GameCanStartStatusMessage gcsm = new GameCanStartStatusMessage();
        gcsm.canStart = getReadyStatus();
        host.send(gcsm);

        readyClients.remove(sender);
    }

    private void handleJoinMessage(ClientHandler sender, JoinMessage jm) {
        int id = idPool.acquire();
        if (jm.key == hostKey) {
            if (host != null) {
                sender.isHost = false;
            } else {
                host = sender;
                sender.isHost = true;
            }
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

        ChatMessage cm = new ChatMessage();
        cm.message = "<SERVER>" + sender.username + " joined the game.";
        broadcast(cm);

        PlayerListMessage plm = PlayerListItem.constructPlayerList(clients);
        broadcast(plm);
        GameCanStartStatusMessage gcsm = new GameCanStartStatusMessage();
        gcsm.canStart = getReadyStatus();
        host.send(gcsm);

        readyClients.put(sender, false);
    }

    private boolean getReadyStatus() {
        boolean ready = true;
        for (ClientHandler ch : clients.values()) {
            if (ch == host) continue;
            if (!ch.ready) {
                ready = false;
                break;
            }
        }
        return ready;
    }

    private void handleLoadingMessages(ClientHandler sender, NetMessage msg) {
        System.out.println("[ClosedGame_Server(Loading message)]");
        switch(msg) {
            case GetPlayerDataMessage gpdm -> handleGetPlayerDataMessage(sender, gpdm);
            case GetMapDataMessage gmdm -> handleGetMapDataMessage(sender, gmdm);
            case AllResourcesLoadedMessage arlm -> handleAllResourcesLoadedMessage(sender, arlm);
            default -> {
                System.out.println("[ClosedGame_Server(Loading message)]: Unknown message type: " + msg.getClass());
            }
        }
    }

    private void handleAllResourcesLoadedMessage(ClientHandler sender, AllResourcesLoadedMessage arlm) {
        AllResourcesLoadedAcknowledgedMessage arlam = new AllResourcesLoadedAcknowledgedMessage();
        arlam.serverStartTime = serverStartTime;
        sender.send(arlam);

        Player p = new Player();
        p.id = sender.id;
        simulation.addPlayer(sender.id, p);

//        ChatMessage cm = new ChatMessage();
//        cm.message = "<SERVER>" + sender.username + " joined the game.";
//        broadcast(cm);

        PlayerListMessage plm = PlayerListItem.constructPlayerList(clients);
        broadcast(plm);

        WorldStateMessage wsm = constructWorldStateMessage();
        broadcast(wsm);

        readyClients.put(sender, true);
    }

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
        String st = "World state";
        for (Player p : simulation.players.values()) {
            st += "ID: " + p.id + ", (" + p.realX + "," + p.realY + ")";
        }
        //System.out.println(st);
        return wsm;
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

    private void handleGetMapDataMessage(ClientHandler sender, GetMapDataMessage gmdm) {
        MapDataMessage mdm = new MapDataMessage();
        mdm.wallBits = simulation.wallGrid;
        sender.send(mdm);
    }

    private void handleGameMessages(ClientHandler sender, NetMessage msg) {
        //System.out.println("[ClosedGame_Server(Game message)]");
        switch (msg) {
            case PlayerWASDMessage pwasdm -> handlePlayerWASDMessage(sender, pwasdm);
            case LeaveGameMessage lgm -> handleLeaveGameMessage(sender, lgm);
            case ChatMessage cm -> handleChatMessage(sender, cm);
            case PingResponseMessage prm -> handlePingResponseMessage(sender, prm);
            default -> {
                System.out.println("[ClosedGame_Server(Game)]: Unknown message type: " + msg.getClass());
            }
        }
    }

    private void handlePingResponseMessage(ClientHandler sender, PingResponseMessage prm) {
        clients.get(prm.clientId).ping = (System.currentTimeMillis() - prm.pingTimeResponse);
    }

    private void handlePlayerWASDMessage(ClientHandler sender, PlayerWASDMessage pwasdm) {
        System.out.println("PlayerWASDMessage! ID: " + sender.id);
        Player p = simulation.getPlayer(sender.id);
        if (p == null) return;
        p.realX = pwasdm.x;
        p.realY = pwasdm.y;
    }

    private void handleLeaveGameMessage(ClientHandler sender, LeaveGameMessage lgm) {
        System.out.println("Disconnecting all from server!");
        moveToNewServerState(ServerState.LOBBY);
        shutdown();
    }

    private void handleChatMessage(ClientHandler sender, ChatMessage cm) {
        cm.message = "<" + sender.username + ">" + cm.message;
        broadcast(cm);
    }

    private void disconnectClient(ClientHandler client) {
        clients.remove(client.id);
    }

    @Override
    public void enqueue(ServerMessage sm) {
        inbox.add(sm);
    }

    @Override
    public void broadcast(NetMessage msg) {
        clients.values().forEach(c -> {
            try {
                c.send(msg);
            } catch (Exception e) {
                System.out.println("FAILED TO SEND TO CLIENT " + c.id + ", " + e.getMessage());
                e.printStackTrace();
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
