package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EnterLoadingMessage extends NetMessage {

    public static final int TYPE = 7;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeUTF("Enter Loading");
    }

    // Deserialize
    public static EnterLoadingMessage read(DataInputStream in) throws IOException {
        in.readUTF();
        return new EnterLoadingMessage();
    }
}
