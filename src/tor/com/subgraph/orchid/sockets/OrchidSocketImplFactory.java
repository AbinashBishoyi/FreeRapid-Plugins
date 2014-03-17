package com.subgraph.orchid.sockets;

import com.subgraph.orchid.TorClient;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

public class OrchidSocketImplFactory implements SocketImplFactory {
    private final TorClient torClient;

    public OrchidSocketImplFactory(TorClient torClient) {
        this.torClient = torClient;
    }

    public SocketImpl createSocketImpl() {
        return new OrchidSocketImpl(torClient);
    }
}
