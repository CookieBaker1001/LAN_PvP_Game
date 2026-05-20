package com.springer.knakobrak.net.messages;

// Response from server
// Contains the state for players, projectiles, power-ups etc...
public class WorldStateMessage extends NetMessage {

    public int[] ids;
    public float[] x;
    public float[] y;

    public WorldStateMessage() {}
}
