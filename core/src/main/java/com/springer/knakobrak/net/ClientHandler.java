package com.springer.knakobrak.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.server.ServerMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientHandler implements Runnable {

    private GameServer server;

    public int id;
    public String name;

    private Socket socket;
    private Kryo kryo;

    private OutputStream os;
    private InputStream is;

    private final Input in;
    private final Output out;

    public boolean isHost;

    private final Queue<NetMessage> incoming;
    public PlayerState playerState;

    public int lastProcessedInput = 0;

    ClientHandler(GameServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;

        kryo = new Kryo();
        NetworkRegistry.register(kryo);

        os = socket.getOutputStream();
        is = socket.getInputStream();

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

        incoming = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            e.printStackTrace();
            DisconnectMessage dcm = new DisconnectMessage();
            dcm.playerId = this.id;
            dcm.reason = "Connection lost";
            server.enqueue(new ServerMessage(this, dcm));
        } finally {
            disconnect();
        }
    }

    private void readLoop() throws IOException {
        NetMessage msg;
        while (!socket.isClosed()) {
            msg = (NetMessage) kryo.readClassAndObject(in);
            server.enqueue(new ServerMessage(this, msg));
        }
    }

    public synchronized void send(NetMessage msg) {
        System.out.println("Sending message to client!");
        kryo.writeClassAndObject(out, msg);
        out.flush();
    }

    public void disconnect() {
        System.out.println("Disconnecting!");
        DisconnectMessage dm = new DisconnectMessage();
        dm.playerId = id;
        dm.reason = "Not specified";
        send(dm);
//        clients.remove(id);
//        simulation.removePlayer(id);
        //simulation.players.remove(id);
//        if (this == host) {
//            DisconnectMessage dm = new DisconnectMessage();
//            dm.playerId = id;
//            dm.reason = "Not specified";
//            broadcast(dm);
//            serverState = GameServer.ServerState.SHUTDOWN;
//            shutdown();
//        }
//        else {
//            broadcastPlayerList();
//        }
        try {
            socket.close();
            socket = null;
        } catch (IOException ignored) {}
    }
}
