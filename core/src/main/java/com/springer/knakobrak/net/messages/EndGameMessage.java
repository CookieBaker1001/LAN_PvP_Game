package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the server letting all the clients know that it is shutting down
public class EndGameMessage extends NetMessage {
    public String reason;
    public EndGameMessage() {}
}
