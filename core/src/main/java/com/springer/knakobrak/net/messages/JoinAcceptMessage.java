package com.springer.knakobrak.net.messages;

public class JoinAcceptMessage extends NetMessage {
    // Sent from the server to acknowledge a new Player joining

    public int id;
    public boolean isHost;
    public int serverType;

    public JoinAcceptMessage() {
    }
}
