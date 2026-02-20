package com.springer.knakobrak.serialization;

import com.esotericsoftware.kryo.Kryo;
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
        kryo.register(StartGameMessage.class);
        kryo.register(ReadyMessage.class);
        kryo.register(StartSimulationMessage.class);
        kryo.register(InitWorldMessage.class);
        kryo.register(PlayerInputMessage.class);
        kryo.register(PlayerSnapshotMessage.class);
        kryo.register(DisconnectMessage.class);
        kryo.register(WorldSnapshotMessage.class);

        kryo.register(PlayerState.class);
        kryo.register(ProjectileState.class);
//        kryo.register(PlayerSnapshot.class);
//        kryo.register(ProjectileSnapshot.class);
        kryo.register(Wall.class);

        kryo.register(int[].class);
        kryo.register(int[][].class);

        kryo.register(ArrayList.class);
    }
}
