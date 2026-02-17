package com.springer.knakobrak.world.client;

import java.util.HashMap;
import java.util.Map;

public class ClientGameState {

    public Map<Integer, PlayerState> players = new HashMap<>();
    public Map<Integer, ProjectileState> projectiles = new HashMap<>();

    public int localPlayerId;
    public PlayerState localPlayer;
}
