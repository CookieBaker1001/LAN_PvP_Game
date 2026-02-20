package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;

public class PlayerSnapshotMessage extends NetMessage {
    // Sent from the server to the acting player so it can adjust and interpolate its data

    public int id;
    public float positionX, positionY;
    public float velocityX, velocityY;
    public int lastProcessedInput;

    public PlayerSnapshotMessage() {
    }
}
