package org.cougaar.core.society;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import java.util.Map;
import java.util.StringTokenizer;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.ComponentFactory;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.*;
import org.cougaar.core.society.rmi.RMIMessageTransport;
import org.cougaar.util.CircularQueue;
import org.cougaar.util.GenericStateModelAdapter;

class MessageTransportServerImpl 
  extends ContainerSupport
  implements ContainerAPI
{

    private String myId;
    private MessageTransportServerBinderFactory binderFactory;
    private MessageTransportServerServiceFactory serviceFactory;
    private NameServer defaultNameServer;

    // Hardwired for now
    private MessageTransportServerBinder loopback;
    private MessageTransportServerBinder remote;
    private HashMap transports; // hash transports -> binders

    public MessageTransportServerImpl(String id) {
	myId = id;

	serviceFactory = new MessageTransportServerServiceFactoryImpl();
	// register with Node

	binderFactory = new MessageTransportServerBinderFactory();
        binderFactory.setBindingSite(this);
	//binderFactory.setParentComponent(this);

	transports = new HashMap();

	// Hardwire two children now, later keep a list.
	MessageTransport tpt = new LoopbackMessageTransport();
	loopback =
	    (MessageTransportServerBinder)
	    binderFactory.getBinder(MessageTransportServerBindingSite.class,
				    tpt);
        loopback.setBindingSite(this);
	transports.put(tpt, loopback);
	
	RMIMessageTransport rtpt = new RMIMessageTransport(id);
	defaultNameServer = rtpt.getNameServer();
	remote =
	    (MessageTransportServerBinder)
	    binderFactory.getBinder(MessageTransportServerBindingSite.class,
				rtpt);
        remote.setBindingSite(this);
	transports.put(rtpt, remote);

	makeOtherTransports();

	startQueues();

    }

    private MessageTransport makeTransport(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class };
	Object[] args = { myId };
	MessageTransport transport = null;
	try {
	    Class transport_class = Class.forName(classname);
	    Constructor constructor = 
		transport_class.getConstructor(types);
	    transport = (MessageTransport) constructor.newInstance(args);
	} catch (Exception xxx) {
	    xxx.printStackTrace();
	    return null;
	}
	Object binder =
	    binderFactory.getBinder(MessageTransportServerBindingSite.class,
				    transport);
        BindingUtility.setBindingSite(binder, this);
	transports.put(transport, binder);
	return transport;
    }


    private void makeOtherTransports() {
	String property = "org.cougaar.message.transportClasses";
	String transport_classes = System.getProperty(property);
	if (transport_classes == null) return;

	StringTokenizer tokenizer = 
	    new StringTokenizer(transport_classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    makeTransport(classname);
	}
    }

    public MessageTransportServerServiceFactory getProvider() {
	// singleton for now
	return serviceFactory;
    }

    // ServiceProvider

    // TO BE DONE - register this with node as a service provider for
    // MessageTransportServer.class
    
    private class MessageTransportServerServiceFactoryImpl
	implements MessageTransportServerServiceFactory
    {

	public MessageTransportServerServiceFactoryImpl() {
	}

	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (requestor instanceof Communications &&
		serviceClass == MessageTransportServer.class)
		{
		    return new MessageTransportServerProxy
			(MessageTransportServerImpl.this);
		}
	    else
		{
		    return null;
		}
	}


	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	}

	public NameServer getDefaultNameServer() {
	    // get it from the rmi transport
	    return defaultNameServer;
	}

    }





    private static class MessageTransportServerBinderFactory 
      extends BinderFactorySupport
    {
	// BinderFactory
	private Vector binders = new Vector();
	private ContainerAPI parent;

      public void setBindingSite(BindingSite parent) {
	    this.parent = (ContainerAPI) parent;        
      }

	public int getPriority() {
	    return NORM_PRIORITY;
	}

	Enumeration getBinders() {
	    return binders.elements();
	}

	public Binder getBinder(Class bindingSite, Object child) {
	    // verify bindingSite == MessageTransportServerBinder
	    MessageTransportServerBinder binder = 
		new MessageTransportServerBinder(this, child);
	    binders.addElement(binder);
	    ((MessageTransport)child).setBinder(binder);
	    return binder;
	}
      public ComponentFactory getComponentFactory() {
        return ComponentFactory.getInstance();
      }
    }




    // Container

    // may need to override ComponentFactory specifyComponentFactory()

    protected String specifyContainmentPoint() {
	return "messagetransportservice.messagetransport";
    }

    protected ServiceBroker specifyChildServiceBroker() {
	// TO BE DONE
	return null;
    }
  public ServiceBroker getServiceBroker() {
	// TO BE DONE
    return null;
  }
  public void requestStop() {}

    protected Class specifyChildBindingSite() {
	return MessageTransportServerBindingSite.class;
    }






    // Queues

    private void startQueues() {
	outQThread = new Thread(new Runnable() {
		public void run() {
		    dispatchOutgoingMessages();
		}
	    }, myId+"/OutQ");
	outQThread.start();
	inQThread = new Thread(new Runnable() {
		public void run() {
		    dispatchIncomingMessages();
		}
	    }, myId+"/InQ");
	inQThread.start();

    }

    // output queue
    // (OutQ dispatch thread)

    /** outbound message queue */
    private CircularQueue outQ = new CircularQueue();
    /** the thread running dispatchOutgoingMessages() */
    private Thread outQThread;
    /** Incoming message queue */
    private CircularQueue inQ = new CircularQueue();
    /** the thread running dispatchIncomingMessages() */
    private Thread inQThread;
    
    /** deal with any outgoing messages as needed */
    private void dispatchOutgoingMessages() {
	while (true) {
	    Message m;
	    int osize;
	    // wait for a message to handle
	    synchronized (outQ) {
		while (outQ.isEmpty()) {
		    try {
			outQ.wait();
		    } catch (InterruptedException e) {} // dont care
		}
        
		m = (Message) outQ.next(); // from top
		osize = outQ.size();
	    }

	    if (m != null) {
		route(m);
	    }
	}
    }

    private void dispatchIncomingMessages() {
	while (true) {
	    Message m;
	    int isize;
	    synchronized (inQ) {
		// wait for a message to handle
		while (inQ.isEmpty()) {
		    try {
			inQ.wait();
			//Thread.sleep(100);	// sleep instead
		    } catch (InterruptedException e) {
			// we don't care if we are interrupted - just
			// continue to cycle.
		    } 
		}
      
		m = (Message) inQ.next();
		isize = inQ.size();
	    }
	    // deliver outside synchronization so that we don't block
	    // the queue
	    sendMessageToClient(m);
	}
    }


    private MessageTransportServerBinder selectTransport(Message message) {
	String prop = "org.cougaar.message.transportClass";
	String defaultTransportClass = System.getProperty(prop);
	MessageTransportClient client = findLocalClient(message.getTarget());
	if (client != null) {
	    return loopback;
	} else if (defaultTransportClass == null) {
	    return remote;
	} else {
	    return selectPreferredTransport(defaultTransportClass);
	}
    }

    private MessageTransportServerBinder 
	selectPreferredTransport(String preferredClassname) 
    {
	Class preferredClass = null;
	try {
	    preferredClass = Class.forName(preferredClassname);
	} catch (ClassNotFoundException ex) {
	    ex.printStackTrace();
	    // default - return RMI transport
	    return remote;
	}

	// Simple matcher: take the first acceptable one
	Set keys = transports.entrySet();
	Iterator itr = keys.iterator();
	while (itr.hasNext()) {
	    Map.Entry pair = (Map.Entry) itr.next();
	    MessageTransport tpt = (MessageTransport) pair.getKey();
	    Class tpt_class = tpt.getClass();
	    if (preferredClass.isAssignableFrom(tpt_class)) {
		return (MessageTransportServerBinder) pair.getValue();
	    }
	}
	return remote;
    }

    private void route(Message message) {
	MessageTransportServerBinder transport = selectTransport(message);
	if (message != null) {
	    transport.routeMessage(message);
	} else {
	    throw new RuntimeException("No transport for " + message);
	}
    }






    // Watchers
    private Vector watchers = new Vector();

    private void watchingOutgoing(Message m) {
	if (watchers.size() == 0) return;
	for (Enumeration e = watchers.elements() ; e.hasMoreElements(); ) {
	    ((MessageTransportWatcher)e.nextElement()).messageSent(m);
	}
    }


    protected void watchingIncoming(Message m) {
	if (watchers.size() == 0) return;
	for (Enumeration e = watchers.elements() ; e.hasMoreElements(); ) {
	    ((MessageTransportWatcher)e.nextElement()).messageReceived(m);
	}
    }



    // Client (Agent) registry

    /** reference to the (local) cluster(s) we are serving.
     * Type is MessageTransportClient
     **/
    private HashMap myClients = new HashMap(89);

    private void addLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.put(client.getMessageAddress(), client);
	    } catch(Exception e) {}
	}
    }
    private void removeLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.remove(client.getMessageAddress());
	    } catch (Exception e) {}
	}
    }

    private MessageTransportClient findLocalClient(MessageAddress id) {
	synchronized (myClients) {
	    return (MessageTransportClient) myClients.get(id);
	}
    }

    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    private Iterator findLocalMulticastClients(MulticastMessageAddress addr)
    {
	synchronized (myClients) {
	    return new ArrayList(myClients.values()).iterator();
	}
    }

    private void registerClientWithSociety(MessageTransportClient client) {
	// register with each component transport
	Enumeration binders = binderFactory.getBinders();
	while (binders.hasMoreElements()) {
	    MessageTransportServerBinder binder = 
		(MessageTransportServerBinder) binders.nextElement();
	    binder.registerClient(client);
	}
    }


    private void sendMessageToClient(Message m) {
	if (m != null) {
	    try {
		MessageAddress addr = m.getTarget();
		if (addr instanceof MulticastMessageAddress) {
		    Iterator i = findLocalMulticastClients((MulticastMessageAddress)addr); 
		    while (i.hasNext()) {
			MessageTransportClient client = (MessageTransportClient) i.next();
			client.receiveMessage(m);
		    }
		} else {
		    MessageTransportClient client = findLocalClient(addr);
		    if (client != null) {
			client.receiveMessage(m);
		    } else {
			throw new RuntimeException("Misdelivered message "+m+" sent to "+this);
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    System.err.println("MessageTransport Received non-Message "+m);
	}
    }








    // Non-public implementation of MessageTransportServer interface.
    
    private boolean checkMessage(Message message) {
	MessageAddress target = message.getTarget();
	// message is ok as long as the target is not empty or null
	return target != null && !target.toString().equals("");
    }

    void sendMessage(Message m) {
	int osize = 0;

	if (checkMessage(m)) {
	    synchronized (outQ) {
		outQ.add(m);
		outQ.notify();
		osize = outQ.size();
	    }


	    watchingOutgoing(m);
	} else {
	    System.err.println("Warning: MessageTransport.sendMessage of malformed message: "+m);
	    Thread.dumpStack();
	    return;
	}
    }

    void registerClient(MessageTransportClient client) {
	addLocalClient(client);
	registerClientWithSociety(client);
    }

    void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	watchers.addElement(watcher);
    }

    String getIdentifier() {
	return myId;
    }


    boolean addressKnown(MessageAddress address) {
	Enumeration binders = binderFactory.getBinders();
	while (binders.hasMoreElements()) {
	    MessageTransportServerBinder binder = 
		(MessageTransportServerBinder) binders.nextElement();
	    if (binder.addressKnown(address)) return true;
	}
	return false;
    }






    // Binding site 

    boolean isLocalAddress(MessageAddress address) {
	return findLocalClient(address) != null;
    }

    void deliverMessage(Message m) {
	int isize;
    
	synchronized (inQ) {
	    inQ.add(m);
	    inQ.notify();
	    isize = inQ.size(); 
	}
	
	watchingIncoming(m);
    }


  public ContainerAPI getContainerProxy() {
    return this;
  }

}
    
