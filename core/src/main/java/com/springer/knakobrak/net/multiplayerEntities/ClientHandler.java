package com.springer.knakobrak.net.multiplayerEntities;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.LeaveAcceptMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
import com.springer.knakobrak.world.Player;
import com.springer.knakobrak.util.ServerMessage;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Server server;
    private final Socket socket;

    private final Kryo kryo;
    private final Input in;
    private final Output out;

    public int id;
    public String username;
    public boolean isHost;
    private volatile boolean connected;
    private volatile boolean disconnectRequested;

    public int lastProcessedInput = 0;

    public boolean ready;
    public long ping = 1000;

    public Player player;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;

        kryo = new Kryo();
        NetworkRegistry.register(kryo);
        kryo.setReferences(false);

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

        connected = true;
        disconnectRequested = false;

        isHost = false;
        ready = false;
        id = 0;
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void readLoop() throws IOException {
        NetMessage msg;
        try {
            while (connected) {
                msg = (NetMessage) kryo.readClassAndObject(in);
                server.enqueue(new ServerMessage(this, msg));
                if (disconnectRequested) connected = false;
            }
        } catch (Exception e) {
            System.out.println("ClientHandler("+id+") disconnected. (" + e.getMessage() + ")");
            System.out.println("-Start of stacktrace-");
            e.printStackTrace();
            System.out.println("-End of stacktrace-");
        }
    }

    public void send(NetMessage msg) {
        synchronized (out) {
            kryo.writeClassAndObject(out, msg);
            out.flush();
        }
    }

    public void sendDisconnect(LeaveAcceptMessage lam) {
        System.out.println("ClientHandler is disconnecting!");
        synchronized (out) {
            kryo.writeClassAndObject(out, lam);
            out.flush();
        }
        disconnectRequested = true;
    }

    private void cleanup() {
        connected = false;
        try { in.close(); } catch (Exception ignored) {}
        try { out.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (Exception ignored) {}
    }
}
