package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.serialization.NetworkRegistry;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GameClient implements Runnable {

    private Socket socket;
    //private BufferedReader in;
    //private PrintWriter out;

    private Kryo kryo;

    private Input in;
    private Output out;

    private final Queue<NetMessage> incoming = new ConcurrentLinkedQueue<>();
    private volatile boolean connected;

    String host;
    int port;

    public GameClient(String host, int port) throws IOException {
        connected = false;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            connect();
            readLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);

        kryo = new Kryo();
        NetworkRegistry.register(kryo);

        in = new Input(socket.getInputStream());
        out = new Output(socket.getOutputStream());

//        out = new PrintWriter(
//            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
//            true
//        );
//        in = new BufferedReader(
//            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
//        );

        connected = true;
        //startListenerThread();
    }

//    private void startListenerThread() {
//        Thread listenerThread = new Thread(() -> {
//            try {
//                String line;
//                while ((line = in.readLine()) != null) {
//                    incoming.add(line);
//                }
//            } catch (IOException ignored) {
//            } finally {
//                connected = false;
//            }
//        });
//        listenerThread.start();
//    }

    private void readLoop() throws IOException {
        NetMessage msg;
        while (connected) {
            //msg = (NetMessage) kryo.readClassAndObject(in);
            msg = kryo.readObject(in, NetMessage.class);
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

    public boolean isConnected() {
        return connected;
    }
}
