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
public abstract class NameSupportBase implements NameSupport {
    protected MessageAddress myNodeAddress;
    
    public NameSupportBase(String id){
	myNodeAddress = new MessageAddress(id+"(Node)");
    }

    public MessageAddress  getNodeMessageAddress() {
	return myNodeAddress;
    }

    public abstract void registerAgentInNameServer(Object proxy, 
                                                   MessageTransportClient client, 
                                                   String transportType);

    public abstract void registerNodeInNameServer(Object proxy, String transportType);

    public abstract Object lookupAddressInNameServer(MessageAddress address, 
                                                     String transportType);
}
