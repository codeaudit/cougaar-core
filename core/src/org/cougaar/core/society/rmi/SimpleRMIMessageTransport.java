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

public class SimpleRMIMessageTransport 
    extends MessageTransport
{
    private static final String TRANSPORT_TYPE = "/simpleRMI";
    

    private boolean madeServerProxy;
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

    protected Object generateClientSideProxy(Object object) {
	return object;
    }


    public DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link(address);
	    links.put(address, link);
	}
	return link;
    }


    /** Override or wrap to generate a different proxy for a client object **/
    protected Object generateServerSideProxy(MessageAddress clientAddress) 
	throws RemoteException
    {
	return new MTImpl(this, clientAddress, recvQ);
    }


    private final void registerMTWithSociety() 
	throws RemoteException
    {
	synchronized (this) {
	    if (!madeServerProxy) {
		madeServerProxy = true;

		Object proxy =   generateServerSideProxy(nameSupport.getNodeMessageAddress());
		nameSupport.registerNodeInNameServer(proxy,TRANSPORT_TYPE);
	    }
	}
    }

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


    public final void registerClient(MessageTransportClient client) {
	try {
	    // always register the Node MT
	    registerMTWithSociety();

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

}
   
