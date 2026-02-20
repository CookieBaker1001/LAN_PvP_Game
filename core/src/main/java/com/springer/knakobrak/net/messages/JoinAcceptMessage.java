package com.springer.knakobrak.net.messages;

public class JoinAcceptMessage extends NetMessage {
    // Sent from the server to acknowledge a new Player joining

    public int clientId;
    public boolean isHost;

    public JoinAcceptMessage() {
    }
}
