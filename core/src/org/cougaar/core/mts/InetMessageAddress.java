/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.mts;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *  Encapsulate an {@link InetSocketAddress} in a Cougaar {@link MessageAddress}.
 *  Used only for (true) multicasting.
 */
public class InetMessageAddress
        extends SimpleMessageAddress {
    
    private transient InetSocketAddress addr;
    
    public InetMessageAddress() {
    }
    
    public InetMessageAddress(String host, int port) {
        super(host +":"+ Integer.toString(port));
        this.addr = new InetSocketAddress(host, port);
    }
    
    public InetMessageAddress(InetAddress host, int port) {
        super(host +":"+ Integer.toString(port));
        this.addr = new InetSocketAddress(host, port);
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
