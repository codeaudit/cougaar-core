package org.cougaar.core.society;



public class RouterFactory extends AspectFactory
{
    private MessageTransportRegistry registry;
    private DestinationQueueFactory destQFactory;

    RouterFactory(MessageTransportRegistry registry,
		  DestinationQueueFactory destQFactory,
		  java.util.ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
	this.destQFactory = destQFactory;
    }

    Router getRouter() {
	Router router = new RouterImpl(registry, destQFactory);
	router = (Router) attachAspects(router, Router.class);
	return router;
    }
}
