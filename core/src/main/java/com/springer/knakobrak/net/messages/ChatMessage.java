package com.springer.knakobrak.net.messages;

public class ChatMessage extends NetMessage {
    // Sent from a client to the in-game chat

    public int playerId;
    public String message;

    public ChatMessage() {
    }
}
