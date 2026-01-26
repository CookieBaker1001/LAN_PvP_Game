package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.PlayerState;
import com.springer.knakobrak.world.ProjectileState;

public class WorldStateMessage extends NetMessage {
    public java.util.List<PlayerState> players;
    public java.util.List<ProjectileState> projectiles;
}
