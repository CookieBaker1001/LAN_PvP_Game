package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LoadingCompleteMessage extends NetMessage {

    public static final int TYPE = 9;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeUTF("Loading complete!");
    }

    // Deserialize
    public static LoadingCompleteMessage read(DataInputStream in) throws IOException {
        LoadingCompleteMessage input = new LoadingCompleteMessage();
        in.readUTF();
        return input;
    }
}
