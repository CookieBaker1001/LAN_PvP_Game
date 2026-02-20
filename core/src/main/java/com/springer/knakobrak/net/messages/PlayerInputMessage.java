package com.springer.knakobrak.net.messages;

public class PlayerInputMessage extends NetMessage {
    // Sent from a client when it is inputting

    public int playerId;
    public int sequence;     // incremental
    public float dx, dy;
    public boolean shoot;

    public PlayerInputMessage() {
    }
}
