package com.springer.knakobrak.serialization;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.client.PlayerState;
import com.springer.knakobrak.world.client.ProjectileState;
import com.springer.knakobrak.world.client.Wall;

import java.util.ArrayList;

public final class NetworkRegistry {

    public static void register(Kryo kryo) {

        kryo.register(NetMessage.class);
        kryo.register(JoinMessage.class);
        kryo.register(JoinAcceptMessage.class);
        kryo.register(JoinRejectedMessage.class);
        kryo.register(LobbyStateMessage.class);
        kryo.register(StartGameMessage.class);
        kryo.register(EnterLoadingMessage.class);
        kryo.register(InitPlayerMessage.class);
        kryo.register(ReadyMessage.class);
        kryo.register(StartSimulationMessage.class);
        kryo.register(InitWorldMessage.class);
        kryo.register(PlayerInputMessage.class);
        kryo.register(PlayerSnapshotMessage.class);
        kryo.register(DisconnectMessage.class);
        kryo.register(WorldSnapshotMessage.class);

        kryo.register(PlayerStateDTO.class);
        kryo.register(ProjectileState.class);
        kryo.register(WallDTO.class);

        kryo.register(PlayerState.class);
        kryo.register(ProjectileState.class);
//        kryo.register(PlayerSnapshot.class);
//        kryo.register(ProjectileSnapshot.class);

        kryo.register(Vector2.class);
        kryo.register(Vector3.class);
        kryo.register(int[].class);
        kryo.register(int[][].class);
        kryo.register(ArrayList.class);
    }
}
