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

import org.cougaar.core.nameservice.NameService;
import javax.naming.NamingException;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public class NewNameSupport extends NameSupportBase implements NameSupport {
    private NameService nameService;
    
    public NewNameSupport(String id) {
        super(id);
    }

    private final void _registerWithSociety(String key, Object proxy) 
	throws NamingException
    {
	if (DEBUG)
	    System.out.println("***Registering :" + key + ":proxy = "+ proxy);
	nameService.getRootContext().bind(key, proxy);
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
    {
	MessageAddress addr = address;
	for (int count=0; count<2; count++) {
	    String key = CLUSTERDIR + addr.getAddress() + transportType ;
	    try {
                Object object = nameService.getRootContext().lookup(key);

                if (DEBUG)
                    System.out.println("***Looked Up :" + key + 
                                       ":object "+ object);
                if (object instanceof MessageAddress) {
                    addr = (MessageAddress) object;
                    continue;   // Follow the link?
                }
                return object;
            } catch (NamingException ne) {
		// unknown?
		return null; 
	    }
	}
        throw new RuntimeException("Address " + address + " loops");
    }
}
