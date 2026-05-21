package com.springer.knakobrak.net.messages;

// SENT FROM CLIENT
// Sent from a client that wants to disconnect
public class DisconnectMessage extends NetMessage {
    public String reason = "Unknown";
    public DisconnectMessage() {}
}
