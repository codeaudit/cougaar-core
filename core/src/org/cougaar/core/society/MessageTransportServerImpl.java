package org.cougaar.core.society;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.society.rmi.RMIMessageTransport;
import org.cougaar.core.society.rmi.SimpleRMIMessageTransport;


class MessageTransportServerImpl 
  extends ContainerSupport
  implements ContainerAPI
{

    private MessageTransportServerServiceFactory serviceFactory;
    private MessageTransportFactory transportFactory;
    private SendQueueFactory sendQFactory;
    private ReceiveQueueFactory recvQFactory;
    private DestinationQueueFactory destQFactory;
    private NameSupport nameSupport;


    // Hardwired for now
    private Router router;
    private SendQueue sendQ;
    private ReceiveQueue recvQ;
    private MessageTransportRegistry registry;

    public MessageTransportServerImpl(String id) {

	nameSupport = new NameSupport(id);

	serviceFactory = new MessageTransportServerServiceFactoryImpl();
	transportFactory = new MessageTransportFactory(id);

	// register with Node
	
	destQFactory = new DestinationQueueFactory();
	registry = new MessageTransportRegistry(id, this);
	router = new Router(registry, destQFactory);
	registry.setTransportFactory(transportFactory);

	sendQFactory = new SendQueueFactory();
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

	recvQFactory = new ReceiveQueueFactory();
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");

	// force transports to be created here
	transportFactory.getTransports();


    }


    // Transport factory
    public class MessageTransportFactory {
	private ArrayList transports;
	private String id;
	private MessageTransport defaultTransport, loopbackTransport;

	public MessageTransportFactory(String id) {
	    this.id = id;
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
	    transport.setRecvQ(recvQ);
	    transport.setRegistry(registry);
	    transport.setNameSupport(nameSupport);
	    transports.add(transport);
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

	public  ArrayList getTransports() {
	    if (transports != null) return transports;

	    transports = new ArrayList();

	    loopbackTransport = new LoopbackMessageTransport();
	    loopbackTransport.setRecvQ(recvQ);
	    loopbackTransport.setRegistry(registry);
	    loopbackTransport.setNameSupport(nameSupport);
	    transports.add(loopbackTransport);
	
	    if (Boolean.getBoolean("org.cougaar.core.society.UseSimpleRMI"))
		defaultTransport = new SimpleRMIMessageTransport(id);
	    else
		defaultTransport = new RMIMessageTransport(id);
	    defaultTransport.setRecvQ(recvQ);
	    defaultTransport.setRegistry(registry);
	    defaultTransport.setNameSupport(nameSupport);
	    transports.add(defaultTransport);


	    makeOtherTransports();

	    return transports;
	}

	MessageTransport getDefaultTransport() {
	    return defaultTransport;
	}

	MessageTransport getLoopbackTransport() {
	    return loopbackTransport;
	}



    }







    // SendQueue factory
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


    // ReceiveQueue factory
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



    // DestinationQueue factory
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
					 registry,
					 transportFactory);
		queues.put(destination, q);
	    }
	    return q;
	}
    }



    // ServiceProvider factory

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

	// This is an odd place to put this method.  It will stay here
	// for now because Communications expects to find it here and
	// we don't want to edit that class.
	public NameServer getDefaultNameServer() {
	    return nameSupport.getNameServer();
	}

    }



    // Return the sevice-proxy factory (singleton for now)
    public MessageTransportServerServiceFactory getProvider() {
	return serviceFactory;
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


    public ContainerAPI getContainerProxy() {
	return this;
    }

}
    
