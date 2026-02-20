package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;

import java.util.List;

public class LobbyStateMessage extends NetMessage {

    public List<PlayerState> players;
    public int hostId;

    public LobbyStateMessage() {
    }
}
