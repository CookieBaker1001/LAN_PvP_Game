package com.springer.knakobrak.net;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GameClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final Queue<String> incoming = new ConcurrentLinkedQueue<>();
    private Thread listenerThread;
    private volatile boolean connected;

    String host;
    int port;

    public GameClient(String host, int port) throws IOException {
        connected = false;
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
            true
        );
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );

        connected = true;
        startListenerThread();
    }

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    incoming.add(line);
                }
            } catch (IOException ignored) {
            } finally {
                connected = false;
            }
        });
        listenerThread.start();
    }

    public void send(String msg) {
        if (connected) out.println(msg);
    }

    public void poll(Consumer<String> handler) {
        while (!incoming.isEmpty()) {
            handler.accept(incoming.poll());
        }
    }

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




//public class GameClient {
//    private java.net.Socket socket;
//    private java.io.ObjectOutputStream out;
//    private java.io.ObjectInputStream in;
//
//    public GameClient(String host, int port) throws Exception {
//        socket = new java.net.Socket(host, port);
//        out = new java.io.ObjectOutputStream(socket.getOutputStream());
//        in = new java.io.ObjectInputStream(socket.getInputStream());
//    }
//
//    public void sendInput(float dx, float dy, boolean shooting) throws Exception {
//        InputMessage msg = new InputMessage();
//        msg.dx = dx;
//        msg.dy = dy;
//        msg.shooting = shooting;
//        out.writeObject(msg);
//        out.flush();
//    }
//
//    public WorldStateMessage readWorld() throws Exception {
//        return (WorldStateMessage) in.readObject();
//    }
//}
