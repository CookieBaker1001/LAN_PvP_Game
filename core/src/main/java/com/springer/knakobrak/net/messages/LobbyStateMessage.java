package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;

import java.util.List;

public class LobbyStateMessage extends NetMessage {
    // Sent from the server to let all clients know about the other players in the lobby screen

    public List<PlayerState> players;
    public int hostId;

    public LobbyStateMessage() {
    }
}
