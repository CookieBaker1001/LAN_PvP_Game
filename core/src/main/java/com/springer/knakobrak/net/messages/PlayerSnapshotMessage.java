package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;

public class PlayerSnapshotMessage extends NetMessage {
    // Sent from the server to the acting player so it can adjust and interpolate its data

    public int id;
    public Vector2 position;
    public Vector2 velocity;
    public int lastProcessedInput;

    public PlayerSnapshotMessage() {
    }
}
