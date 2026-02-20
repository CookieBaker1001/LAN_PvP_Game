package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;

public class WorldSnapshotMessage extends NetMessage {

    public int id;
    public Vector2 position;
    public Vector2 velocity;
    public int lastProcessedInput;

    public WorldSnapshotMessage() {
    }
}
