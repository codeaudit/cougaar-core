/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society.rmi;

import org.cougaar.core.society.DestinationLink;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransport;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.NameServer;
import org.cougaar.core.society.NameSupport;

import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * This is a minimal rmi message transport which does nothing other
 * than rmi-specific functionality.  The hope is that we can pull out
 * a number of other aspects from the big RMIMessageTransport, leaving
 * this simple and maintainable body of code to deal with the actual
 * rmi functionality per se.  
 *
 * The cost function of the DestinationLink inner subclass is
 * currently hardwired to an arbitrary value of 1000.  This should be
 * made smarter eventually. */
public class SimpleRMIMessageTransport 
    extends MessageTransport
{
    private static final String TRANSPORT_TYPE = "/simpleRMI";
    

    private boolean madeNodeProxy;
    private HashMap links;


    public SimpleRMIMessageTransport(String id) {
	super(); 
	links = new HashMap();
    }


    private MT lookupRMIObject(MessageAddress address) throws Exception {
	Object object = 
	    nameSupport.lookupAddressInNameServer(address, TRANSPORT_TYPE);
	if (object == null) return null;

	object = generateClientSideProxy(object);
	if (object instanceof MT) {
	    return (MT) object;
	} else {
	    throw new RuntimeException("Object "
				       +object+
				       " is not a MessageTransport!");
	}

    }

    /** Override or wrap to generate a different proxy for a client object **/
    protected Object generateClientSideProxy(Object object) {
	return object;
    }


    /** Override or wrap to generate a different proxy for a server object **/
    protected Object generateServerSideProxy(MessageAddress clientAddress) 
	throws RemoteException
    {
	return new MTImpl(this, clientAddress, recvQ);
    }

    private final void registerNodeWithSociety() 
	throws RemoteException
    {
	synchronized (this) {
	    if (!madeNodeProxy) {
		madeNodeProxy = true;
		Object proxy =
		    generateServerSideProxy(nameSupport.getNodeMessageAddress());
		nameSupport.registerNodeInNameServer(proxy,TRANSPORT_TYPE);
	    }
	}
    }

    public final void registerClient(MessageTransportClient client) {
	try {
	    // Register a MT for the Node
	    // Since there is no explicit time for registering the Node
	    // Attempt every time you register a Client
	    registerNodeWithSociety();

	    MessageAddress addr = client.getMessageAddress();
	    if (NameSupport.DEBUG)
		System.out.println("***Client address is  " + addr);
	    
	    Object proxy = generateServerSideProxy(addr);
	    nameSupport.registerAgentInNameServer(proxy,client,TRANSPORT_TYPE);
	} catch (Exception e) {
	    System.err.println("Error registering MessageTransport:");
	    e.printStackTrace();
	}
    }

    public boolean addressKnown(MessageAddress address) {
	try {
	    return lookupRMIObject(address) != null;
	} catch (Exception e) {
	    //System.err.println("Failed in addressKnown:"+e);
	    //e.printStackTrace();
	}
	return false;
    }

    public DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link(address);
	    links.put(address, link);
	}
	return link;
    }



    /**
     * The DestinationLink class for this transport.  Forwarding a
     * message with this link means looking up the MT proxy for a
     * remote MTImpl, and calling rerouteMessage on it.  The cost is
     * currently hardwired at an arbitrary value of 1000. */
    class Link implements DestinationLink {
	
	private MessageAddress target;
	private MT remote;

	Link(MessageAddress destination) {
	    this.target = destination;
	}

	public int cost (Message message) {
	    return 1000;
	}


	public void forwardMessage(Message message) {
	    while (remote == null) {
		try {
		    remote = lookupRMIObject(target);
		}
		catch (Exception lookup_failure) {
		    System.err.println("Name lookup failure on " + target);
		    lookup_failure.printStackTrace();
		    return;
		}
		try { Thread.sleep(500); } catch (InterruptedException ex) {}
	    }


	    try {
		remote.rerouteMessage(message);
	    } 
	    catch (RemoteException ex) {
		System.err.println("Reroute failure on " + message);
		ex.printStackTrace();
	    }

	}
    }

}
   
