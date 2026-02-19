package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StartGameMessage extends NetMessage {

    public static final int TYPE = 6;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeUTF("Start game!");
    }

    // Deserialize
    public static StartGameMessage read(DataInputStream in) throws IOException {
        StartGameMessage input = new StartGameMessage();
        in.readUTF();
        return input;
    }
}
