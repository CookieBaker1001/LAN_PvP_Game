package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JoinAcceptMessage extends NetMessage {

    public static final int TYPE = 2;

    public int clientId;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.write(clientId);
    }

    // Deserialize
    public static JoinAcceptMessage read(DataInputStream in) throws IOException {
        JoinAcceptMessage input = new JoinAcceptMessage();
        input.clientId = in.readInt();
        return input;
    }
}
