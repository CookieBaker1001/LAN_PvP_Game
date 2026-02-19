package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InitWorldMessage extends NetMessage {

    public static final int TYPE = 8;

    int level;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.write(level);
    }

    // Deserialize
    public static InitWorldMessage read(DataInputStream in) throws IOException {
        InitWorldMessage input = new InitWorldMessage();
        input.level = in.readInt();
        return input;
    }
}
