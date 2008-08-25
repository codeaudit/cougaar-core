/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.mts;

import java.net.InetSocketAddress;

/**
 *  Encapsulate an {@link InetSocketAddress} in a Cougaar MessageAddress.
 *  Used only for true multicasting.
 */
public class SocketMessageAddress
        extends SimpleMessageAddress {
    
    private transient InetSocketAddress addr;
    
    public SocketMessageAddress() {
    }
    
    public SocketMessageAddress(InetSocketAddress addr) {
        super(addr.toString());
        this.addr = addr;
    }

    public InetSocketAddress getSocketAddress() {
        if (addr == null) {
            String addressString = getAddress();
            String[] hostAndPort = addressString.split(":");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);
            addr = new InetSocketAddress(host, port);
        }
        return addr;
    }
}
