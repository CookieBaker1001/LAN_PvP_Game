package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JoinRejectedMessage extends NetMessage {

    public static final int TYPE = 3;

    String reason;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeUTF(reason);
    }

    // Deserialize
    public static JoinRejectedMessage read(DataInputStream in) throws IOException {
        JoinRejectedMessage input = new JoinRejectedMessage();
        input.reason = in.readUTF();
        return input;
    }
}
