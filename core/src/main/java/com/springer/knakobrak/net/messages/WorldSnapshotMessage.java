package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.PlayerSnapshot;
import com.springer.knakobrak.world.ProjectileSnapshot;

import java.util.ArrayList;

public class WorldSnapshotMessage extends NetMessage {
    // Sent from the server that ll clients use to update the world state

    public float serverTime;
    public ArrayList<PlayerSnapshot> players;
    public ArrayList<ProjectileSnapshot> projectiles;

    public WorldSnapshotMessage() {
    }
}
