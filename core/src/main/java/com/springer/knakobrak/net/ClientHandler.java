package com.springer.knakobrak.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.ServerMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

    private volatile boolean connected;
    private volatile boolean disconnectRequested = false;

    public PlayerState playerState;

    public int lastProcessedInput = 0;

    ClientHandler(GameServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;

        kryo = new Kryo();
        NetworkRegistry.register(kryo);
        kryo.setReferences(false);

        os = socket.getOutputStream();
        is = socket.getInputStream();

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

        connected = true;
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            //System.err.println("CLIENT HANDLER CRASHED: " + id);
            if (!disconnectRequested) e.printStackTrace();
        } finally {
            //System.err.println("CLIENT HANDLER DISCONNECTING: " + id);
            cleanup();
            //disconnect();
        }
    }

    private void readLoop() throws IOException {
        NetMessage msg;
        while (connected) {
            msg = (NetMessage) kryo.readClassAndObject(in);
            //System.out.println("CH"+id+": RECV <- " + msg.getClass().getSimpleName());
            server.enqueue(new ServerMessage(this, msg));

            if (disconnectRequested) connected = false;
        }
    }

    public void send(NetMessage msg) {
        //System.out.println("CH"+id+": SEND -> " + msg.getClass().getSimpleName());
        synchronized (out) {
            kryo.writeClassAndObject(out, msg);
            out.flush();
        }
    }

    public void requestDisconnect() {
        disconnectRequested = true;
    }

    public void disconnect() {
        disconnectRequested = true;
        connected = false;
        cleanup();
    }

    private void cleanup() {
        connected = false;

        try { in.close(); } catch (Exception ignored) {}
        try { out.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (Exception ignored) {}
    }
}
