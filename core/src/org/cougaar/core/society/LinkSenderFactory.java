/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;


/**
 * A factory for making LinkSenders.  There are no aspects associated
 * with LinkSenders, so this is not an AspectFactory.  */
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


    /**
     * Instantiate a LinkSender (no find-or-make here). */
    public LinkSender getLinkSender(String name, 
				    MessageAddress destination, 
				    DestinationQueue queue)
					
    {
	return new LinkSender(name, destination, registry, transportFactory, queue);
    }

}
