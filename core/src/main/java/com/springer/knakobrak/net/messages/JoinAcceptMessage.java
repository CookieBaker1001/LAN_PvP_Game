package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the server to acknowledge a new Player joining
public class JoinAcceptMessage extends NetMessage {
    public int id;
    public boolean isHost;
    public int serverType;
    public JoinAcceptMessage() {}
}
