package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChatMessage extends NetMessage {

    public static final int TYPE = 15;

    int playerId;
    String message;

    public void setMessage(String msg) {
        this.message = msg;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeInt(playerId);
        out.writeUTF(message);
    }

    // Deserialize
    public static ChatMessage read(DataInputStream in) throws IOException {
        ChatMessage input = new ChatMessage();
        input.playerId = in.readInt();
        input.playerId = in.readInt();
        input.message = in.readUTF();
        return input;
    }
}
