package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the server to each client to get a sense of latency.
public class PingMessage extends NetMessage {
    public int secuence;
    public long pingTime;
    public PingMessage() {}
}
