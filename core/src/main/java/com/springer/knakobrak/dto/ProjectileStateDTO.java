package com.springer.knakobrak.dto;

import com.springer.knakobrak.world.client.ProjectileState;

public class ProjectileStateDTO {
    public int id;
    public int ownerId;
    public float x, y;
//    public float lifeTime = 0f;
//    public int lifeTimeLimit = 3; // milliseconds
//
//    public boolean isDead;

    public static ProjectileStateDTO toDTO(ProjectileState ps) {
        ProjectileStateDTO dto = new ProjectileStateDTO();
        dto.id = ps.id;
        dto.ownerId = ps.ownerId;
        dto.x = ps.x;
        dto.y = ps.y;
        return dto;
    }

    public static ProjectileState fromDTO(ProjectileStateDTO psDTO) {
        ProjectileState ps = new ProjectileState();
        ps.id = psDTO.id;
        ps.ownerId = psDTO.ownerId;
        ps.x = psDTO.x;
        ps.y = psDTO.y;
        return ps;
    }
}
