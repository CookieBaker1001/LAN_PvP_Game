package com.springer.knakobrak.net.messages;

public class SpawnProjectileMessage extends NetMessage {

    int projectileId;
    int ownerId;
    float x, y;
    float vx, vy;

    public SpawnProjectileMessage() {
    }
}
