package com.springer.knakobrak.net.messages;

public class DisconnectMessage extends NetMessage {
    // Sent from a client who wants to disconnect

    public int playerId;
    public String reason = "Unknown";

    public DisconnectMessage() {
    }
}
