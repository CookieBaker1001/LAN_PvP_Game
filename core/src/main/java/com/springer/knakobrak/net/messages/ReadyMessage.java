package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ReadyMessage extends NetMessage {

    public static final int TYPE = 5;

    public boolean ready;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeBoolean(ready);
    }

    // Deserialize
    public static ReadyMessage read(DataInputStream in) throws IOException {
        ReadyMessage input = new ReadyMessage();
        input.ready = in.readBoolean();
        return input;
    }
}
