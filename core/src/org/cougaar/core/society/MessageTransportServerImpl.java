package org.cougaar.core.society;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
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
import org.cougaar.core.society.rmi.SimpleRMIMessageTransport;


class MessageTransportServerImpl 
  extends ContainerSupport
  implements ContainerAPI
{

    private MessageTransportServerBinderFactory binderFactory;
    private MessageTransportServerServiceFactory serviceFactory;
    private SendQueueFactory sendQFactory;
    private ReceiveQueueFactory recvQFactory;
    private DestinationQueueFactory destQFactory;
    private NameServer defaultNameServer;

    // Hardwired for now
    private Router router;
    private SendQueue sendQ;
    private ReceiveQueue recvQ;
    private MessageTransportRegistry registry;

    public MessageTransportServerImpl(String id) {

	serviceFactory = new MessageTransportServerServiceFactoryImpl();
	// register with Node

	binderFactory = new MessageTransportServerBinderFactory();
        binderFactory.setBindingSite(this);

	//binderFactory.setParentComponent(this);

	
	destQFactory = new DestinationQueueFactory();
	registry = new MessageTransportRegistry(id, this);
	router = new Router(registry, destQFactory);

	sendQFactory = new SendQueueFactory();
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

	recvQFactory = new ReceiveQueueFactory();
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");

	// Hardwire two children now, later keep a list.
	MessageTransport tpt = new LoopbackMessageTransport();
	MessageTransportServerBinder loopback =
	    (MessageTransportServerBinder)
	    binderFactory.getBinder(MessageTransportServerBindingSite.class,
				    tpt);
        loopback.setBindingSite(this);
	tpt.setRecvQ(recvQ);
	router.addTransport(tpt);
	router.setLoopbackTransport(tpt);
	
	RMIMessageTransport rtpt = new RMIMessageTransport(id);
	// SimpleRMIMessageTransport rtpt = new SimpleRMIMessageTransport(id);
	defaultNameServer = rtpt.getNameServer();
	MessageTransportServerBinder remote =
	    (MessageTransportServerBinder)
	    binderFactory.getBinder(MessageTransportServerBindingSite.class,
				rtpt);
        remote.setBindingSite(this);
	rtpt.setRecvQ(recvQ);
	router.addTransport(rtpt);
	router.setDefaultTransport(rtpt);

	makeOtherTransports();


    }



    private MessageTransport makeTransport(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class };
	Object[] args = { registry.getIdentifier() };
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
	transport.setRecvQ(recvQ);
	router.addTransport(transport);
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
    
    class MessageTransportServerServiceFactoryImpl
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
		    return new MessageTransportServerProxy(registry, sendQ);
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





    static class MessageTransportServerBinderFactory 
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


    class SendQueueFactory {
	private ArrayList queues = new ArrayList();

	SendQueue getSendQueue(String name, Router router) {
	    Iterator i = queues.iterator();
	    while (i.hasNext()) {
		SendQueue candidate = (SendQueue) i.next();
		if (candidate != null && candidate.matches(name, router)) return candidate;
	    }
	    // No match, make a new one
	    SendQueue queue = new SendQueue(name, router, registry);
	    queues.add(queue);
	    return queue;
	}
    }


    class ReceiveQueueFactory {
	private ArrayList queues = new ArrayList();

	ReceiveQueue getReceiveQueue(String name) {
	    Iterator i = queues.iterator();
	    while (i.hasNext()) {
		ReceiveQueue candidate = (ReceiveQueue) i.next();
		if (candidate != null && candidate.matches(name)) return candidate;
	    }
	    // No match, make a new one
	    ReceiveQueue queue = new ReceiveQueue(name, registry);
	    queues.add(queue);
	    return queue;
	}
    }



    class DestinationQueueFactory {
	private HashMap queues;
	
	DestinationQueueFactory() {
	    queues = new HashMap();
	}

	DestinationQueue getDestinationQueue(MessageAddress destination) {
	    
	    DestinationQueue q = (DestinationQueue) queues.get(destination);
	    if (q == null) { 
		q = new DestinationQueue(destination.toString(), 
					 destination,
					 router);
		queues.put(destination, q);
	    }
	    return q;
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


    Enumeration getBinders() {
	return binderFactory.getBinders();
    }





    // Binding site support

    String getIdentifier() {
	return registry.getIdentifier();
    }

    boolean isLocalAddress(MessageAddress address) {
	return registry.findLocalClient(address) != null;
    }

    void deliverMessage(Message m) {
	recvQ.deliverMessage(m);
    }


  public ContainerAPI getContainerProxy() {
    return this;
  }

}
    
