package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.dto.PlayerStateDTO;

import java.util.ArrayList;

public class LobbyStateMessage extends NetMessage {
    // Sent from the server to let all clients know about the other players in the lobby screen

    public ArrayList<PlayerStateDTO> players;
    public int hostId;

    public LobbyStateMessage() {
    }
}
