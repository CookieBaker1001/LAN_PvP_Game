package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.dto.PlayerStateDTO;

import java.util.ArrayList;

public class InitPlayersMessage extends NetMessage {
    // Sent from the server to let all clients know of this new Player in the loading screen

    public ArrayList<PlayerStateDTO> players;

    public InitPlayersMessage() {
    }
}
