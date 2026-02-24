package com.springer.knakobrak.net;

import com.springer.knakobrak.net.messages.NetMessage;

public interface NetworkListener {
    void handleNetworkMessage(NetMessage msg);
}
