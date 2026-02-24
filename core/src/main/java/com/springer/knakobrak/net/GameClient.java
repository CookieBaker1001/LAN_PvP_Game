package com.springer.knakobrak.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.LanPvpGame;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
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

    String host;
    int port;

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
        //outgoing = new LinkedBlockingQueue<>();
        connected = true;
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
        while (connected) {
            msg = (NetMessage) kryo.readClassAndObject(in);
            System.out.println("C: RECV <- " + msg.getClass().getSimpleName());
            incoming.offer(msg);
        }
    }

    // Used by Client classes to send messages
    public void send(NetMessage msg) {
        System.out.println("C: SEND -> " + msg.getClass().getSimpleName());
        synchronized (out) {
            kryo.writeClassAndObject(out, msg);
            out.flush();
        }
    }

    public NetMessage pollOne() {
        return incoming.poll();
    }

    public boolean hasOne() {
        return !incoming.isEmpty();
    }

    public void disconnect() {
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public void disconnect(int id) {
        DisconnectMessage dcm = new DisconnectMessage();
        dcm.reason = "Client quit on their own volition";
        dcm.playerId = id;
        send(dcm);
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }
}
