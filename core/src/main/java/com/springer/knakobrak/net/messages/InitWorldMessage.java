package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.world.client.Wall;

import java.util.ArrayList;

public class InitWorldMessage extends NetMessage {
    // Sent from the server during loading screen to give all clients info about the map

    public ArrayList<WallDTO> walls;
    public ArrayList<Vector2> spawnPoints;

    public InitWorldMessage() {
    }

    public InitWorldMessage(ArrayList<WallDTO> walls, ArrayList<Vector2> spawnPoints) {
        this.walls = walls;
        this.spawnPoints = spawnPoints;
    }
}
