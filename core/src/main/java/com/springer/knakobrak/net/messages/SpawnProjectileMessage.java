package com.springer.knakobrak.net.messages;

public class SpawnProjectileMessage extends NetMessage {
    // Sent from a client who just fired

    public int projectileId;
    public int ownerId;
    public float x, y;
    public float vx, vy;

    public SpawnProjectileMessage() {
    }
}
