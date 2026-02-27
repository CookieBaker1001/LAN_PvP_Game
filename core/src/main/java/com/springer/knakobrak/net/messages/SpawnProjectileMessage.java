package com.springer.knakobrak.net.messages;

public class SpawnProjectileMessage extends NetMessage {
    // Sent from a client who just fired

    public int ownerId;
    public int fireSequence;
    public float dx, dy;

    public SpawnProjectileMessage() {
    }
}
