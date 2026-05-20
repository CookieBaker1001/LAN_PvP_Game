package com.springer.knakobrak.screens;

import com.springer.knakobrak.net.messages.NetMessage;

public interface NetworkListener {
    void handleNetworkMessage(NetMessage msg);
}
