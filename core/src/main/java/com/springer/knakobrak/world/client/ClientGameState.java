package com.springer.knakobrak.world.client;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

public class ClientGameState {

    public World world;

    public Map<Integer, PlayerState> players = new HashMap<>();
    public Map<Integer, ProjectileState> projectiles = new HashMap<>();

    public int localPlayerId;
    public PlayerState localPlayer;

    public Array<Wall> walls = new Array<>();
}
