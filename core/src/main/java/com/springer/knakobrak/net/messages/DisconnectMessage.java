package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DisconnectMessage extends NetMessage {

    public static final int TYPE = 14;

    int playerId;
    String reason;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeInt(playerId);
        out.writeUTF(reason);
    }

    // Deserialize
    public static DisconnectMessage read(DataInputStream in) throws IOException {
        DisconnectMessage input = new DisconnectMessage();
        input.playerId = in.readInt();
        input.reason = in.readUTF();
        return input;
    }
}
