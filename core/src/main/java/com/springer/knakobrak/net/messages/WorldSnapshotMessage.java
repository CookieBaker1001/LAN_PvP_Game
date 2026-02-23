package com.springer.knakobrak.net.messages;

import com.springer.knakobrak.world.PlayerSnapshot;
import com.springer.knakobrak.world.ProjectileSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class WorldSnapshotMessage extends NetMessage {
    // Sent from the server that ll clients use to update the world state

    public float serverTime;
    public HashMap<Integer, PlayerSnapshot> players;
    public HashMap<Integer, ProjectileSnapshot> projectiles;

    public WorldSnapshotMessage() {
    }
}
