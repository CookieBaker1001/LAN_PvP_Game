package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.world.client.PlayerState;

public class InitPlayerMessage extends NetMessage {
    // Sent from the server to let all clients know of this new Player in the loading screen

    public PlayerStateDTO player;

    public InitPlayerMessage() {
    }
}
