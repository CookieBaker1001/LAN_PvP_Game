package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;

public class InitPlayerMessage extends NetMessage {

    public PlayerState player;

    public InitPlayerMessage() {
    }
}
