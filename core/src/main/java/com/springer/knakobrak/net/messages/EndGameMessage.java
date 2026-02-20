package com.springer.knakobrak.net.messages;

public class EndGameMessage extends NetMessage {
    // Sent from the server letting all the clients know that it is shutting down

    public String reason;

    public EndGameMessage() {
    }
}
