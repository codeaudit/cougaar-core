package org.cougaar.core.society.rmi;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransport;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.NameServer;

import java.rmi.RemoteException;
import java.util.HashMap;

public class SimpleRMIMessageTransport extends MessageTransport
{
    private static final String TRANSPORT_TYPE = "/simpleRMI";
    

    private boolean madeServerProxy;
    private HashMap remotes;


    public SimpleRMIMessageTransport(String id) {
	super(); 
	remotes = new HashMap();
    }


    private MT lookupRMIObject(MessageAddress address) throws Exception {
	Object object = nameSupport.lookupAddressInNameServer(address, TRANSPORT_TYPE);
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


    public void routeMessage(Message message) {
	MessageAddress target = message.getTarget();
	MT remote = (MT) remotes.get(target);
	if (remote == null) {
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

	    remotes.put(target, remote);
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
   
