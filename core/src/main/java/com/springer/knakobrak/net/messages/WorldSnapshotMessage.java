package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;

import java.util.ArrayList;

public class WorldSnapshotMessage extends NetMessage {
    // Sent from the server that ll clients use to update the world state

    public ArrayList<PlayerState> players;
    public ArrayList<ProjectileState> projectiles;

    public WorldSnapshotMessage() {
    }
}
