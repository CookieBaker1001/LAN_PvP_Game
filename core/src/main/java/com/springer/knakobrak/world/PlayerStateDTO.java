package com.springer.knakobrak.world;

import com.badlogic.gdx.graphics.Color;

public class PlayerStateDTO {
    public int id;
    public float x, y;
    public Color color = Color.WHITE;

    public static PlayerStateDTO FromServerToDTO(ServerPlayerState serverPlayerState) {
        PlayerStateDTO dto = new PlayerStateDTO();
        dto.id = serverPlayerState.id;
        dto.x = serverPlayerState.x;
        dto.y = serverPlayerState.y;
        dto.color = serverPlayerState.color;
        return dto;
    }
}
