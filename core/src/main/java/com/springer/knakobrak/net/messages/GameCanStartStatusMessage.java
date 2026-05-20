package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the closed server to inform whether the host can press the start button
public class GameCanStartStatusMessage extends NetMessage {
    public boolean canStart;
    public GameCanStartStatusMessage() {}
}
