/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.society.rmi.RMINameServer;

import java.rmi.RemoteException;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public interface NameSupport {
    public static final boolean DEBUG = 
	Boolean.getBoolean("org.cougaar.core.society.DebugTransport");
    public static final String CLUSTERDIR = "/clusters/";
    public static final String MTDIR = "/MessageTransports/";
    MessageAddress  getNodeMessageAddress();

    void registerAgentInNameServer(Object proxy, 
                                   MessageTransportClient client, 
                                   String transportType);

    void registerNodeInNameServer(Object proxy, String transportType);

    Object lookupAddressInNameServer(MessageAddress address, 
                                     String transportType);
}
