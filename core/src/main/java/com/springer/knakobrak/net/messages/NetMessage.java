package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class NetMessage {
    public abstract void write(DataOutputStream out) throws IOException;

    public static NetMessage read(int type, DataInputStream in) throws IOException {
        return switch (type) {
            case 1 -> JoinMessage.read(in);
            case 2 -> JoinAcceptMessage.read(in);
            case 3 -> JoinRejectedMessage.read(in);
            case 4 -> LobbyStateMessage.read(in);
            case 5 -> ReadyMessage.read(in);
            case 6 -> StartGameMessage.read(in);
            case 7 -> EnterLoadingMessage.read(in);
            case 8 -> InitWorldMessage.read(in);
            case 9 -> LoadingCompleteMessage.read(in);
            case 10 ->StartSimulationMessage.read(in);
            case 11 -> PlayerInputMessage.read(in);
            case 12 -> WorldSnapshotMessage.read(in);
            case 13 -> SpawnProjectileMessage.read(in);
            case 14 -> DisconnectMessage.read(in);
            case 15 -> ChatMessage.read(in);
            default -> throw new IOException("Unknown message type");
        };
    }
}
