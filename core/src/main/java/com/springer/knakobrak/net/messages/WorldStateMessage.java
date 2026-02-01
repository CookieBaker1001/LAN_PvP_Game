package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.server.ServerProjectileState;

public class WorldStateMessage extends NetMessage {
    public java.util.List<PlayerState> players;
    public java.util.List<ServerProjectileState> projectiles;
}
