package com.springer.knakobrak.dto;

import com.springer.knakobrak.world.Wall;

import java.util.ArrayList;

public class WallDTO {
    public float x, y;
    public float width, height;

    public static ArrayList<WallDTO> toDTO(ArrayList<Wall> ws) {
        ArrayList<WallDTO> wsDTO = new ArrayList<>();
        for (Wall w : ws) {
            WallDTO wDTO = new WallDTO();
            wDTO.x = w.x;
            wDTO.y = w.y;
            wDTO.width = w.width;
            wDTO.height = w.height;
            wsDTO.add(wDTO);
        }
        return wsDTO;
    }

    public static ArrayList<Wall> fromDTO(ArrayList<WallDTO> wsDTO) {
        ArrayList<Wall> ws = new ArrayList<>();
        for (WallDTO wDTO : wsDTO) {
            Wall w = new Wall();
            w.x = wDTO.x;
            w.y = wDTO.y;
            w.x = wDTO.x;
            w.y = wDTO.y;
            ws.add(w);
        }
        return ws;
    }
}
