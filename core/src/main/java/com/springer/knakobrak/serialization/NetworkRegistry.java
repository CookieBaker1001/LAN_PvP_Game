package com.springer.knakobrak.serialization;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.springer.knakobrak.net.messages.*;

import java.util.ArrayList;

public final class NetworkRegistry {

    public static void register(Kryo kryo) {

        kryo.register(NetMessage.class);

        kryo.register(AllResourcesLoadedAcknowledgedMessage.class);
        kryo.register(AllResourcesLoadedMessage.class);
        kryo.register(ChatMessage.class);
        kryo.register(DisconnectMessage.class);
        kryo.register(EndGameMessage.class);
        kryo.register(EnterLoadingMessage.class);
        kryo.register(EveryOneIsReadyMessage.class);
        kryo.register(GameCanStartStatusMessage.class);
        kryo.register(GetMapDataMessage.class);
        kryo.register(GetPlayerDataMessage.class);
        kryo.register(GetWorldStateMessage.class);
        kryo.register(InitPlayersMessage.class);
        kryo.register(InitWorldMessage.class);
        kryo.register(JoinAcceptMessage.class);

        kryo.register(JoinMessage.class);
        kryo.register(JoinRejectedMessage.class);
        kryo.register(LeaveAcceptMessage.class);
        kryo.register(LeaveGameMessage.class);
        kryo.register(LeaveLobbyMessage.class);
        kryo.register(LoadingCompleteMessage.class);
        kryo.register(LobbyStateMessage.class);
        kryo.register(MapDataMessage.class);

        kryo.register(PingMessage.class);
        kryo.register(PingResponseMessage.class);

        kryo.register(PlayerDeathMessage.class);
        kryo.register(PlayerHealthMessage.class);
        kryo.register(PlayerInputMessage.class);
        kryo.register(PlayerListMessage.class);

        kryo.register(PlayerSnapshotMessage.class);
        kryo.register(PlayerStateMessage.class);
        kryo.register(PlayerWASDMessage.class);
        kryo.register(ReadyMessage.class);
        kryo.register(SpawnProjectileMessage.class);
        kryo.register(StartGameMessage.class);
        kryo.register(StartSimulationMessage.class);
        kryo.register(WorldSnapshotMessage.class);
        kryo.register(WorldStateMessage.class);

        kryo.register(ArrayList.class);
        kryo.register(int[].class);
        kryo.register(int[][].class);

        kryo.register(float.class);
        kryo.register(float[].class);

        kryo.register(String.class);
        kryo.register(String[].class);

        kryo.register(Vector2.class);
        kryo.register(Vector3.class);

        kryo.register(long.class);
    }

    private NetworkRegistry() {}
}
