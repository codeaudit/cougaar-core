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
public class NameSupport
{
    public static final boolean DEBUG = 
	Boolean.getBoolean("org.cougaar.core.society.DebugTransport");
    public static final String CLUSTERDIR = "/clusters/";
    public static final String MTDIR = "/MessageTransports/";
    
    private MessageAddress myNodeAddress;
    private NameServer nameserver;
    
    public NameSupport(String id){
	myNodeAddress = new MessageAddress(id+"(Node)");
	nameserver=new RMINameServer();
    }


    private final void _registerWithSociety(String key, Object proxy) 
	throws RemoteException
    {
	if (DEBUG)
	    System.out.println("***Registering :" + key + ":proxy = "+ proxy);
	Object old = nameserver.put(key, proxy);
	if (old != null) {
	    System.err.println("Warning: Re-registration of "+
			       key+" as "+proxy+
			       " (was "+old+").");
	}
    }

    public MessageAddress  getNodeMessageAddress() {
	return myNodeAddress;
    }
    public void registerAgentInNameServer(Object proxy, 
					  MessageTransportClient client, 
					  String transportType)
    {	
	MessageAddress addr = client.getMessageAddress();
	try {
	    String key = CLUSTERDIR + addr + transportType;
	    _registerWithSociety(key, proxy);
	} catch (Exception e) {
	    System.err.println("Failed to add Client "+ addr + 
			       " to NameServer for transport" + transportType);
	    e.printStackTrace();
	}
    }

    public void registerNodeInNameServer(Object proxy, String transportType) {
	try {
	    _registerWithSociety(MTDIR+myNodeAddress.getAddress()+transportType, proxy);
	    _registerWithSociety(CLUSTERDIR+myNodeAddress.getAddress()+transportType, proxy);
	} catch (Exception e) {
	    System.err.println("Failed to add Node " + myNodeAddress.getAddress() +
			       "to NameServer for transport" + transportType);
	    e.printStackTrace();
	}
    }

    public Object lookupAddressInNameServer(MessageAddress address, 
					    String transportType)
	throws Exception 
    {
	MessageAddress addr = address;
	for (int count=0; count<2; count++) {
	    String key = CLUSTERDIR + addr.getAddress() + transportType ;
	    Object object = nameserver.get(key);

	    if (DEBUG)
		System.out.println("***Looking Up :" + key + 
				   ":object "+ object);
	    if (object == null) { 
		// unknown?
		return null; 
	    } else if (object instanceof MessageAddress) {
		addr = (MessageAddress) object;
	    } else {
	        return object;
	    }
	}
	throw new RuntimeException("Address "+address+" loops");
    }
	



    public NameServer getNameServer() {
	return nameserver;
    }


}
