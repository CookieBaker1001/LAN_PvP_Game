package com.springer.knakobrak.dto;

import com.badlogic.gdx.graphics.Color;
import com.springer.knakobrak.world.client.PlayerState;

public class PlayerStateDTO {
    public int id;
    public String name;
    public float x, y;
    public float r, g, b;

    public static PlayerStateDTO toDTO(PlayerState ps) {
        PlayerStateDTO dto = new PlayerStateDTO();
        dto.id = ps.id;
        dto.name = ps.name;
        dto.x = ps.x;
        dto.y = ps.y;
        dto.r = ps.color.r;
        dto.g = ps.color.g;
        dto.b = ps.color.b;
        return dto;
    }

    public static PlayerState fromDTO(PlayerStateDTO psDTO) {
        PlayerState ps = new PlayerState();
        ps.id = psDTO.id;
        ps.name = psDTO.name;
        ps.x = psDTO.x;
        ps.y = psDTO.y;
        ps.color = new Color(psDTO.r, psDTO.g, psDTO.b, 1.0f);
        return ps;
    }
}
