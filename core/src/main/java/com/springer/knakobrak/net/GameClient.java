package com.springer.knakobrak.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.JoinMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;
import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class GameClient implements Runnable {

    private final Socket socket;
    private final Kryo kryo;

    private final Input in;
    private final Output out;

    private final Queue<NetMessage> incoming;
    //private final BlockingQueue<NetMessage> outgoing;

    private volatile boolean connected;

    String host;
    int port;

    public GameClient(String host, int port) throws IOException {
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

    // Used by Client classes to receive messages
    public void poll(Consumer<NetMessage> handler) {
        NetMessage msg;
        while ((msg = incoming.poll()) != null) {
            //System.out.println("Received message!");
            System.out.println("Polling: " + msg.getClass().getSimpleName());
            handler.accept(msg);
        }
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
