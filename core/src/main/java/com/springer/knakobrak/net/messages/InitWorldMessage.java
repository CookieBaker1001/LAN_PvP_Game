package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.world.client.Wall;

import java.util.ArrayList;

public class InitWorldMessage extends NetMessage {

    public ArrayList<Wall> walls;
    public ArrayList<Vector2> spawnPoints;

    public InitWorldMessage() {
    }

    public InitWorldMessage(ArrayList<Wall> walls, ArrayList<Vector2> spawnPoints) {
        this.walls = walls;
        this.spawnPoints = spawnPoints;
    }
}
