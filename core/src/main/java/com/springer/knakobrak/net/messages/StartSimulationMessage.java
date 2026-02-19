package com.springer.knakobrak.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StartSimulationMessage extends NetMessage {

    public static final int TYPE = 10;

    long serverTick;

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(TYPE);
        out.writeLong(serverTick);
    }

    // Deserialize
    public static StartSimulationMessage read(DataInputStream in) throws IOException {
        StartSimulationMessage input = new StartSimulationMessage();
        input.serverTick = in.readLong();
        return input;
    }
}
