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

import org.cougaar.core.component.Component;

public abstract class MessageTransport implements Component
{
    private MessageTransportServerBindingSite binder;

    abstract public void routeMessage(Message message);
    abstract public void registerClient(MessageTransportClient client);
    abstract public boolean addressKnown(MessageAddress address);

    void setBinder(MessageTransportServerBindingSite binder) {
	this.binder = binder;
    }

    protected MessageTransportServerBindingSite getBinder() {
	return binder;
    }

    // Hook for handling incoming remote route requests, only
    // called from our own MTImpl.
    public void rerouteMessage(Message message) {
	getBinder().deliverMessage(message);
    }
  
    

}
