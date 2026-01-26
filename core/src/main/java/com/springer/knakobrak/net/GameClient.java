package com.springer.knakobrak.net;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GameClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final Queue<String> incoming = new ConcurrentLinkedQueue<>();
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
        Thread listenerThread = new Thread(() -> {
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
