package org.cougaar.core.society;

import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;



class MessageTransportServerImpl 
  extends ContainerSupport
  implements ContainerAPI
{

    // Factories
    private MessageTransportServerServiceFactory serviceFactory;
    protected MessageTransportFactory transportFactory;
    protected SendQueueFactory sendQFactory;
    protected ReceiveQueueFactory recvQFactory;
    protected DestinationQueueFactory destQFactory;
    protected LinkSenderFactory linkSenderFactory;
    protected RouterFactory routerFactory;


    // Singeltons
    protected NameSupport nameSupport;
    protected MessageTransportRegistry registry;
    protected Router router;
    protected SendQueue sendQ;
    protected ReceiveQueue recvQ;

    public MessageTransportServerImpl(String id) {
	nameSupport = new NameSupport(id);
	registry = new MessageTransportRegistry(id, this);
	serviceFactory = new MessageTransportServerServiceFactoryImpl();
	transportFactory = 
	    new MessageTransportFactory(id, registry, nameSupport);
	
	registry.setTransportFactory(transportFactory);

	wireComponents(id);

	transportFactory.setRecvQ(recvQ);
	// force transports to be created here
	transportFactory.getTransports();

    }

    protected void wireComponents(String id) {
	recvQFactory = new ReceiveQueueFactory(registry);
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");


	linkSenderFactory =
	    new LinkSenderFactory(registry, transportFactory);
	
	destQFactory = 
	    new DestinationQueueFactory(registry, transportFactory, linkSenderFactory);
	routerFactory =
	    new RouterFactory(registry, destQFactory);

	router = routerFactory.getRouter();

	sendQFactory = new SendQueueFactory(registry);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

    }






    // ServiceProvider factory

    // TO BE DONE - register this with node as a service provider for
    // MessageTransportServer.class
    
    private final class MessageTransportServerServiceFactoryImpl
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
	return null;
    }


    public ContainerAPI getContainerProxy() {
	return this;
    }

}
    
