package com.springer.knakobrak.net.multiplayerEntities;

import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    public ClosedGame_Server(int port, long key) throws IOException {
        this.port = port;
        this.hostKey = key;

        serverType = ServerType.LOBBIED;
        serverSocket = new ServerSocket(port);
        running = true;
        shutdownRequested = false;
        idPool = new IdPool();
        serverState = ServerState.LOBBY;
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
    private float elapsedTime = 0f;
    private float accumulator_1 = 0f;
    private float accumulator_5 = 0f;

    private final long TICK_MS = 16;
    private float SERVER_TICK_SPEED = 1f / 60f;
    private void gameLoop() {
        while (running) {
            processServerMessages();
            if (serverState == ServerState.GAME) {
                accumulator_1 += SERVER_TICK_SPEED;
                accumulator_5 += SERVER_TICK_SPEED;
                elapsedTime += SERVER_TICK_SPEED;

                if (accumulator_1 >= 1f) {
                    accumulator_1 -= 1f;
                    secondCounter++;
                    System.out.println("[Time]: " + secondCounter);
                    doSomethingEverySecond();
                }
                if (accumulator_5 >= 5f) {
                    accumulator_5 -= 5f;
                    doSomethingEvery5Seconds();
                }
            }
            else if (serverState == ServerState.LOADING) {
                tryToGoFormLoadingToGameScreen();
            }
        }
    }

    private void tryToGoFormLoadingToGameScreen() {
        if (!everyOneIsLoaded) return;
        moveToNewServerState(ServerState.GAME);
        AllResourcesLoadedMessage dlam = new AllResourcesLoadedMessage();
        broadcast(dlam);
    }

    private void moveToNewServerState(ServerState state) {
        if (state == ServerState.LOADING) {
            everyOneIsLoaded = false;
        } else if (state == ServerState.GAME) {
        }
        serverState = state;
    }

    private void doSomethingEverySecond() {

    }

    private void doSomethingEvery5Seconds() {

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
        switch (msg) {
            case JoinMessage jm -> handleJoinMessage(sender, jm);
            case LeaveLobbyMessage llm -> handleLeaveLobbyMessage(sender, llm);
            case ReadyMessage rm -> handleReadyMessage(sender, rm);
            case EveryOneIsReadyMessage eirm -> handleEveryOneIsReadyMessage(eirm);
            //case LeaveGameMessage lgm -> handleLeaveGameMessage(sender, lgm);
            default -> {
                System.out.println("[ClosedGame_Server(Lobby)]: Unknown message type: " + msg.getClass());
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
        System.out.println("[ClosedGame_Server(Loading)]");
    }

    private void handleGameMessages(ClientHandler sender, NetMessage msg) {
        switch (msg) {
            case LeaveGameMessage lgm -> handleLeaveGameMessage(sender, lgm);
            case ChatMessage cm -> handleChatMessage(sender, cm);
            default -> {
                System.out.println("[ClosedGame_Server(Game)]: Unknown message type: " + msg.getClass());
            }
        }
    }

    private void handleLeaveGameMessage(ClientHandler sender, LeaveGameMessage lgm) {

    }

    private void handleChatMessage(ClientHandler sender, ChatMessage cm) {

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
