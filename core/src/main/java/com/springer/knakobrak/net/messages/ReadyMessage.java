package com.springer.knakobrak.net.messages;

public class ReadyMessage extends NetMessage {
    // Send from a client that is fully ready to start the game

    public boolean ready;

    public ReadyMessage() {
    }
}
