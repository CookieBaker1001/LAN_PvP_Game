package com.springer.knakobrak.net.gameServerHelpers;

import com.springer.knakobrak.net.ClientHandler;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.net.messages.ReadyMessage;
import com.springer.knakobrak.net.messages.StartSimulationMessage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LoadingHelper {

    private GameServer gameServer;
    private Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    public LoadingHelper(GameServer gameServer, Map<Integer, ClientHandler> clients) {
        this.gameServer = gameServer;
        this.clients = clients;
    }

    public void handleLoadingMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof ReadyMessage) {
            ReadyMessage rm = (ReadyMessage) msg;
            handleReadyMessage(sender, rm);
        }
    }

    Set<Integer> readyClients = new HashSet<>();
    void handleReadyMessage(ClientHandler sender, ReadyMessage rm) {
        if (!rm.ready) {
            readyClients.remove(sender.id);
            return;
        }
        readyClients.add(sender.id);
        if (readyClients.size() == clients.size()) {
            startGame();
        }
    }

    private void startGame() {
        System.out.println("[Server]: Ladies and gentlemen; we are starting the GAME!!!");
        gameServer.setState(2);
        gameServer.broadcast(new StartSimulationMessage());
        //broadcast("START_GAME");
    }
}
