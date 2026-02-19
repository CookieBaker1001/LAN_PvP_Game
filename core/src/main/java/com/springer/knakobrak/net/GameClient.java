package com.springer.knakobrak.net;

import com.badlogic.gdx.graphics.Color;
import com.springer.knakobrak.net.messages.NetMessage;

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

    private DataInputStream in;
    private DataOutputStream out;

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

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

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
        while (connected) {
            int messageType = in.readInt(); // blocks safely

            NetMessage msg = NetMessage.read(messageType, in);
            incoming.add(msg);
        }
    }

    public synchronized void send(NetMessage msg) throws IOException {
        msg.write(out);
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
