package org.cougaar.core.society;


public class LinkSenderFactory 
{
    private MessageTransportRegistry registry;
    private MessageTransportFactory transportFactory;
	
    LinkSenderFactory(MessageTransportRegistry registry,
		      MessageTransportFactory transportFactory)
    {
	this.registry = registry;
	this.transportFactory = transportFactory;
    }


    public LinkSender getLinkSender(String name, 
				    MessageAddress destination, 
				    DestinationQueue queue)
					
    {
	return new LinkSender(name, destination, registry, transportFactory, queue);
    }

}
