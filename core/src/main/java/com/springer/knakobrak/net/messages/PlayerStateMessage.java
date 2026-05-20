package com.springer.knakobrak.net.messages;

public class PlayerStateMessage extends NetMessage {

    public int[] ids;
    public float[] x;
    public float[] y;

    public PlayerStateMessage() {}
}
