package org.cougaar.core.society.rmi;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransport;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.NameServer;

import java.rmi.RemoteException;

public class SimpleRMIMessageTransport extends MessageTransport
{
    public static final String CLUSTERDIR = "/clusters/";
    public static final String MTDIR = "/MessageTransports/";

    private MT myMT = null;
    private MessageAddress myAddress;
    private NameServer nameserver;
    private MT remote = null;


    public SimpleRMIMessageTransport(String id) {
	super(); 
	myAddress = new MessageAddress(id+"(Node)");
	nameserver=new RMINameServer();
    }


    private MT lookupRMIObject(MessageAddress address) throws Exception {
	MessageAddress addr = address;
	for (int count=0; count<2; count++) {
	    String key = CLUSTERDIR+addr.getAddress();
	    Object object = nameserver.get(key);

	    if (object == null) { 
		// unknown?
		return null; 
	    } else if (object instanceof MessageAddress) {
		addr = (MessageAddress) object;
	    } else {
		object = generateClientSideProxy(object);
		if (object instanceof MT) {
		    return (MT) object;
		} else {
		    throw new RuntimeException("Object "
					       +object+
					       " is not a MessageTransport!");
		}
	    }
	}

	throw new RuntimeException("Address "+address+" loops");

    }

      protected Object generateClientSideProxy(Object object) {
	  return object;
      }



    /** Override or wrap to generate a different proxy for a client object **/
    protected Object generateServerSideProxy(MessageAddress clientAddress) 
	throws RemoteException
    {
	return new MTImpl(this, clientAddress, recvQ);
    }

    private final void _registerWithSociety(String path, Object proxy) 
	throws RemoteException
    {
	Object old = nameserver.put(path, proxy);
	if (old != null) {
	    System.err.println("Warning: Re-registration of "+
			       path+" as "+proxy+
			       " (was "+old+").");
	}
    }

    private final void registerMTWithSociety() 
	throws RemoteException
    {
	synchronized (this) {
	    if (myMT == null) {
		// make a real shim so that overriders of generateServerSideProxy
		// can override usefully.
		myMT = new MTImpl(this, myAddress);

		Object proxy =   generateServerSideProxy(myAddress);
		// register both as an MT and as a Cluster (so that lookup Works)
		_registerWithSociety(MTDIR+myAddress.getAddress(), proxy);
		_registerWithSociety(CLUSTERDIR+myAddress.getAddress(), proxy);
	    }
	}
    }

    public NameServer getNameServer() {
	return nameserver;
    }

    public void routeMessage(Message message) {
	while (remote == null) {
	    try {
		remote = lookupRMIObject(message.getTarget());
	    }
	    catch (Exception lookup_failure) {
		System.err.println("Name lookup failure on " + message.getTarget());
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

    public final void registerClient(MessageTransportClient client) {
	try {
	    // always register the Node MT
	    registerMTWithSociety();

	    MessageAddress addr = client.getMessageAddress();
	    String p = CLUSTERDIR+addr;
	    Object proxy = generateServerSideProxy(addr);
	    _registerWithSociety(p, proxy);
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
   
