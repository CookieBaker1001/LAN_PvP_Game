package com.springer.knakobrak.serialization;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.springer.knakobrak.dto.PlayerStateDTO;
import com.springer.knakobrak.dto.WallDTO;
import com.springer.knakobrak.net.messages.*;
import com.springer.knakobrak.world.PlayerSnapshot;
import com.springer.knakobrak.world.ProjectileSnapshot;
import com.springer.knakobrak.world.ProjectileState;

import java.util.ArrayList;
import java.util.HashMap;

public final class NetworkRegistry {

    public static void register(Kryo kryo) {

        kryo.register(NetMessage.class);

        kryo.register(JoinMessage.class);
        kryo.register(JoinAcceptMessage.class);
        kryo.register(JoinRejectedMessage.class);
        kryo.register(LobbyStateMessage.class);
        kryo.register(StartGameMessage.class);
        kryo.register(EnterLoadingMessage.class);
        kryo.register(InitPlayersMessage.class);
        kryo.register(LoadingCompleteMessage.class);
        kryo.register(ReadyMessage.class);
        kryo.register(StartSimulationMessage.class);
        kryo.register(InitWorldMessage.class);
        kryo.register(PlayerInputMessage.class);
        kryo.register(PlayerSnapshotMessage.class);
        kryo.register(SpawnProjectileMessage.class);
        kryo.register(DisconnectMessage.class);
        kryo.register(WorldSnapshotMessage.class);
        kryo.register(ChatMessage.class);
        kryo.register(EndGameMessage.class);

        kryo.register(PlayerStateDTO.class);
        kryo.register(WallDTO.class);
        kryo.register(PlayerSnapshot.class);
        kryo.register(ProjectileSnapshot.class);

        kryo.register(ArrayList.class);
        kryo.register(int[].class);
        kryo.register(int[][].class);

        kryo.register(Vector2.class);
        kryo.register(Vector3.class);
    }

    private NetworkRegistry() {}
}
