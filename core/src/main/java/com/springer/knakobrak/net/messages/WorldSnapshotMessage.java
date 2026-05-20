package com.springer.knakobrak.net.messages;

public class WorldSnapshotMessage extends NetMessage {
    // Sent from the server that ll clients use to update the world state

    public int[] ids;
    public float[] x;
    public float[] y;

    public WorldSnapshotMessage() {
    }
}
