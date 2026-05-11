package com.springer.knakobrak.net.gameServerHelpers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.ClientHandler;
import com.springer.knakobrak.net.GameServer;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.util.LoadUtillities;
import com.springer.knakobrak.world.PhysicsSimulation;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.Wall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LobbyHelper {

    private GameServer gameServer;
    private PhysicsSimulation simulation;
    private Map<Integer, ClientHandler> clients;
    private ClientHandler host;
    private AtomicInteger nextId;

    public LobbyHelper(GameServer gameServer, PhysicsSimulation simulation, Map<Integer, ClientHandler> clients, ClientHandler host, AtomicInteger nexId) {
        this.gameServer = gameServer;
        this.simulation = simulation;
        this.clients = clients;
        this.host = host;
        this.nextId = nexId;
    }

    public void handleLobbyMessage(ClientHandler sender, NetMessage msg) {
        if (msg instanceof StartGameMessage && sender == host) {
            transitionToLoading();
        } else if (msg instanceof JoinMessage) {
            JoinMessage jm = (JoinMessage) msg;
            handleJoin(sender, jm);
        } else if (msg instanceof DisconnectMessage) {
            DisconnectMessage dcm = (DisconnectMessage) msg;
            handleDisconnect(sender, dcm);
        } else if (msg instanceof EndGameMessage) {
            EndGameMessage egm = (EndGameMessage) msg;
            handleEndGame(egm);
        }
    }

    private void transitionToLoading() {
        gameServer.broadcast(new EnterLoadingMessage());
        gameServer.setState(1);
        loadData();
        spawnPlayers();
        sendInitialDataToAllClients();
        gameServer.broadcast(new LoadingCompleteMessage());
    }

    private void handleJoin(ClientHandler sender, JoinMessage jm) {
        System.out.println(jm.playerName + " (v." + jm.protocolVersion + ") just joined!");
        int id = nextId.getAndIncrement();
        sender.id = id;
        sender.name = jm.playerName;
        if (clients.isEmpty()) {
            host = sender;
            sender.isHost = true;
        }
        PlayerState ps = new PlayerState();
        ps.id = id;
        ps.name = jm.playerName;
        ps.playerIcon = jm.playerIcon;
        ps.ballIcon = jm.ballIcon;
        simulation.addPlayer(ps);
        //simulation.players.put(id, ps);
        sender.playerState = ps;
        clients.put(id, sender);
        JoinAcceptMessage accept = new JoinAcceptMessage();
        accept.clientId = id;
        accept.isHost = sender.isHost;
        sender.send(accept);
        broadcastPlayerList();
    }

    private void handleEndGame(EndGameMessage egm) {
        gameServer.requestShutdown();
        //shutdownRequested = true;
        DisconnectMessage dcm = new DisconnectMessage();
        dcm.reason = egm.reason;
        clients.values().forEach(c -> {
            //c.requestDisconnect();
            dcm.playerId = c.id;
            removeClient(c, dcm);
        });
        gameServer.shutdown();
    }

    private void handleDisconnect(ClientHandler sender, DisconnectMessage dcm) {
        removeClient(sender, dcm);
        broadcastPlayerList();
    }

    private void loadData() {
        try {
            simulation.setWallGrid(LoadUtillities.loadLevel("levels/level1.txt"));
            //printWallGrid(grid);
            simulation.setWalls(LoadUtillities.generateWallsFromGrid(simulation.getWorld(), simulation.getWallGrid()));
            //printWalls(simulation.walls);
            simulation.setPlayerSpawnPoints(LoadUtillities.getPlayerSpawnPoints(simulation.getWallGrid()));
        } catch (IOException e) {
            System.out.println("Error loading level: " + e.getMessage());
            e.printStackTrace();
            gameServer.setState(0);
        }
    }

    private void removeClient(ClientHandler sender, DisconnectMessage dcm) {
        System.out.println("Player " + dcm.playerId + " left. Reason: " + dcm.reason);
        clients.remove(sender.id);
        sender.requestDisconnect();
    }

    private void broadcastPlayerList() {
        LobbyStateMessage lsm = new LobbyStateMessage();
        lsm.hostId = host.id;
        lsm.players = new ArrayList<>();
        for (ClientHandler c : clients.values()) {
            PlayerStateDTO p = new PlayerStateDTO();
            p.name = c.playerState.name;
            p.id = c.playerState.id;
            lsm.players.add(p);
        }
        System.out.println("Broadcasting players!");
        gameServer.broadcast(lsm);
    }

    private void sendInitialDataToAllClients() {
        InitPlayersMessage ipm = new InitPlayersMessage();
        ArrayList<PlayerStateDTO> playersDTO = new ArrayList<>();
        System.out.println("Lets check for players here.");
        for (PlayerState p : simulation.getPlayers().values()) {
            System.out.println("Found one player here....");
            PlayerStateDTO pDTO = new PlayerStateDTO();
            pDTO.name = p.name;
            pDTO.id = p.id;
            pDTO.playerIcon = p.playerIcon;
            pDTO.ballIcon = p.ballIcon;
            pDTO.x = p.x;
            pDTO.y = p.y;
            playersDTO.add(pDTO);
        }
        ipm.players = playersDTO;
        InitWorldMessage iwm = new InitWorldMessage();
        ArrayList<WallDTO> walls = new ArrayList<>();
        for (Wall w : simulation.getWalls()) {
            WallDTO wDTO = new WallDTO();
            wDTO.x = w.x;
            wDTO.y = w.y;
            wDTO.width = w.width;
            wDTO.height = w.height;
            walls.add(wDTO);
        }
        iwm.walls = walls;
        iwm.spawnPoints = simulation.getPlayerSpawnPoints();
        iwm.wallBits = simulation.getWallGrid();
        for (ClientHandler c : clients.values()) {
            c.send(ipm);
            c.send(iwm);
        }
    }

    private void spawnPlayers() {
        int i = 0;
        ArrayList<Vector2> points = simulation.getPlayerSpawnPoints();
        for (ClientHandler c : clients.values()) {
            c.playerState.id = c.id;
            c.playerState.x = points.get(i).x + 0.5f;
            c.playerState.y = points.get(i).y + 0.5f;
            c.playerState.body = LoadUtillities.createPlayerBody(simulation.getWorld(), c.playerState.x, c.playerState.y, c.id);

//            Filter f = c.playerState.body.getFixtureList().first().getFilterData();
//            System.out.println("Server: PLAYER: cat=" + f.categoryBits + " mask=" + f.maskBits);

            i++;
        }
    }
}
