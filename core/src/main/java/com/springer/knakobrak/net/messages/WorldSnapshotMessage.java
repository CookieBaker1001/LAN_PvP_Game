package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WorldSnapshotMessage extends NetMessage {

    public static final int TYPE = 12;

    public int id;
    public Vector2 position;
    public Vector2 velocity;
    public int lastProcessedInput;

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeFloat(position.x);
        out.writeFloat(position.y);
        out.writeFloat(velocity.x);
        out.writeFloat(velocity.y);
        out.writeInt(lastProcessedInput);
    }

    public static WorldSnapshotMessage read(DataInputStream in) throws IOException {
        WorldSnapshotMessage snap = new WorldSnapshotMessage();
        snap.id = in.readInt();
        snap.position.x = in.readFloat();
        snap.position.y = in.readFloat();
        snap.velocity.x = in.readFloat();
        snap.velocity.y = in.readFloat();
        snap.lastProcessedInput = in.readInt();
        return snap;
    }
}
