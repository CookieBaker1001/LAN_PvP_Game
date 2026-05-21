package com.springer.knakobrak.net.messages;

// SENT FROM CLIENT
// Sent from a client to let the server know that it is joining
public class JoinMessage extends NetMessage {
    public String username;
    public long key;
    public int playerIcon;
    public int ballIcon;
    public int protocolVersion;
    public JoinMessage() {}
}
