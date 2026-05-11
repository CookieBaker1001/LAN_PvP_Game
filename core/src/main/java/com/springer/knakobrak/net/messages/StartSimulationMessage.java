package com.springer.knakobrak.net.messages;

public class StartSimulationMessage extends NetMessage {
    // Sent from the server when all clients have confirmed to be ready

    long serverTick;

    public StartSimulationMessage() {
    }
}
