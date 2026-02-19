package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.Net;
import com.springer.knakobrak.world.client.PlayerState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class LobbyStateMessage extends NetMessage {

    public static final int TYPE = 4;

    List<PlayerState> players;
    int hostId;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        //out.write(players);
    }

    // Deserialize
    public static LobbyStateMessage read(DataInputStream in) throws IOException {
        LobbyStateMessage input = new LobbyStateMessage();
        return input;
    }
}
