package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PlayerInputMessage extends NetMessage {

    public static final int TYPE = 11;

    public int playerId;
    public int sequence;     // incremental
    public float dx, dy;
    public boolean shoot;

    // Serialize
    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.write(playerId);
        out.writeInt(sequence);
        out.writeFloat(dx);
        out.writeFloat(dy);
        out.writeBoolean(shoot);
    }

    // Deserialize
    public static PlayerInputMessage read(DataInputStream in) throws IOException {
        PlayerInputMessage input = new PlayerInputMessage();
        input.playerId = in.readInt();
        input.sequence = in.readInt();
        input.dx = in.readFloat();
        input.dy = in.readFloat();
        input.shoot = in.readBoolean();
        return input;
    }
}
