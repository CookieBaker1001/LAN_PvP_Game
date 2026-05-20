package com.springer.knakobrak.net.multiplayerEntities;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.EndGameMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
import com.springer.knakobrak.util.ServerType;

import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameClient implements Runnable {

    private final Socket socket;
    private final Kryo kryo;

    private final Input in;
    private final Output out;

    private final Queue<NetMessage> incoming;

    private volatile boolean connected;
    private volatile boolean disconnectRequested;

    private String host;
    private int port;
    public int id;
    public boolean isHost;
    public ServerType serverType;

    private LanPvpGame game;

    public GameClient(LanPvpGame game, String host, int port) throws IOException {
        this.game =  game;
        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        kryo = new Kryo();
        NetworkRegistry.register(kryo);
        kryo.setReferences(false);

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

        incoming = new ConcurrentLinkedQueue<>();

        isHost = false;
        connected = true;
        disconnectRequested = false;
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void readLoop() throws IOException {
        NetMessage msg;
        try {
            while (connected) {
                msg = (NetMessage) kryo.readClassAndObject(in);
                incoming.offer(msg);
                if (disconnectRequested) connected = false;
            }
        } catch (Exception e) {
            System.out.println("Client("+game.id+") is leaving.");
        }
    }

    public void send(NetMessage msg) {
        synchronized (out) {
            kryo.writeClassAndObject(out, msg);
            out.flush();
        }
    }

    public NetMessage pollOne() {
        return incoming.poll();
    }

    public void disconnect() {
        disconnectRequested = true;
        try { in.close(); } catch (Exception ignored) {}
        try { out.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (Exception ignored) {}
        game.isClientShuttingDown = true;
    }
}
