package org.cougaar.core.society;

import java.util.HashMap;

public class ReceiveLinkFactory extends AspectFactory
{
    private HashMap links;
    private MessageTransportRegistry registry;
	
    ReceiveLinkFactory(MessageTransportRegistry registry,
		       java.util.ArrayList aspects)
    {
	super(aspects);
	links = new HashMap();
	this.registry = registry;
    }

    ReceiveLink getReceiveLink(MessageTransportClient client) {
	ReceiveLink link = (ReceiveLink) links.get(client);
	if (link == null) {
	    link = new ReceiveLinkImpl(client);
	    link = ( ReceiveLink) attachAspects(link, ReceiveLink.class);
	    links.put(client, link);
	}
	return link;
    }
}

