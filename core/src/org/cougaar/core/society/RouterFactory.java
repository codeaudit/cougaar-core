package org.cougaar.core.society;



public class RouterFactory 
{
    private MessageTransportRegistry registry;
    private DestinationQueueFactory destQFactory;

    RouterFactory(MessageTransportRegistry registry,
		  DestinationQueueFactory destQFactory)
    {
	this.registry = registry;
	this.destQFactory = destQFactory;
    }

    Router getRouter() {
	return new Router(registry, destQFactory);
    }
}
