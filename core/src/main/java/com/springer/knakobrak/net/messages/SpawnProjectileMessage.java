package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SpawnProjectileMessage extends NetMessage {

    public static final int TYPE = 13;

    int projectileId;
    int ownerId;
    float x, y;
    float vx, vy;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeInt(projectileId);
        out.writeInt(ownerId);
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(vx);
        out.writeFloat(vy);
    }

    // Deserialize
    public static SpawnProjectileMessage read(DataInputStream in) throws IOException {
        SpawnProjectileMessage input = new SpawnProjectileMessage();
        input.projectileId = in.readInt();
        input.ownerId = in.readInt();
        input.x = in.readFloat();
        input.y = in.readFloat();
        input.vx = in.readFloat();
        input.vy = in.readFloat();
        return input;
    }
}
