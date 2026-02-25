package com.springer.knakobrak.net.messages;

public class JoinMessage extends NetMessage {
    // Sent from a client to let the server know that it is joining

    public String playerName;
    public int playerIcon;
    public int protocolVersion;

    public JoinMessage() {
    }
}
