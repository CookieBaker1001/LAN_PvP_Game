package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;

import java.util.List;

public class LobbyStateMessage extends NetMessage {

    List<PlayerState> players;
    int hostId;

    public LobbyStateMessage() {
    }
}
