package com.springer.knakobrak.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.DisconnectMessage;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;

import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GameClient implements Runnable {

    private Socket socket;
    private Kryo kryo;

//    private InputStream is;
//    private OutputStream os;

    private Input in;
    private Output out;

    private final Queue<NetMessage> incoming = new ConcurrentLinkedQueue<>();

    //private Thread thread;
    private volatile boolean connected;

    String host;
    int port;

    public GameClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        socket = new Socket(host, port);

        kryo = new Kryo();
        NetworkRegistry.register(kryo);

//        is = socket.getInputStream();
//        os = socket.getOutputStream();
//
//        in = new Input(is);
//        out = new Output(os);

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

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
            //msg = kryo.readObject(in, NetMessage.class);
            incoming.add(msg);
        }
    }

    public synchronized void send(NetMessage msg) {
        kryo.writeClassAndObject(out, msg);
        out.flush();
    }

    public void poll(Consumer<NetMessage> handler) {
        NetMessage msg;
        while ((msg = incoming.poll()) != null) {
            System.out.println("Received message!");
            handler.accept(msg);
        }
    }

//    public void send(String msg) {
//        if (connected) out.println(msg);
//    }

//    public void poll(Consumer<String> handler) {
//        while (!incoming.isEmpty()) {
//            handler.accept(incoming.poll());
//        }
//    }

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
