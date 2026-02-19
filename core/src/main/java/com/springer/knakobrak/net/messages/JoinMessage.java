package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JoinMessage extends NetMessage {

    public static final int TYPE = 1;

    String playerName;
    int protocolVersion;

    // Serialize
    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeUTF(playerName);
        out.writeInt(protocolVersion);
    }

    // Deserialize
    public static JoinMessage read(DataInputStream in) throws IOException {
        JoinMessage input = new JoinMessage();
        input.playerName = in.readUTF();
        input.protocolVersion = in.readInt();
        return input;
    }
}
