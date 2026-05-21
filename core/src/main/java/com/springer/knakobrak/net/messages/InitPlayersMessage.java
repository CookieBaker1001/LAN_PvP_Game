package com.springer.knakobrak.net.messages;

import java.util.ArrayList;

// SENT FORM SERVER
// Sent from the server to let all clients know of this new Player in the loading screen
public class InitPlayersMessage extends NetMessage {
    //public ArrayList<PlayerStateDTO> players;
    public InitPlayersMessage() {}
}
