package com.springer.knakobrak.net.messages;

import com.badlogic.gdx.math.Vector2;
import com.springer.knakobrak.dto.WallDTO;

import java.util.ArrayList;

public class InitWorldMessage extends NetMessage {
    // Sent from the server during loading screen to give all clients info about the map

    public ArrayList<WallDTO> walls;
    public int[][] wallBits;
    public ArrayList<Vector2> spawnPoints;

    public InitWorldMessage() {
    }
}
