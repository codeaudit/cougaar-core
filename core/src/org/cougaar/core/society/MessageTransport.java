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
import org.cougaar.util.GenericStateModelAdapter;

public abstract class MessageTransport 
  extends GenericStateModelAdapter
  implements Component
{
    private MessageTransportServerBindingSite binder;
    protected ReceiveQueue recvQ;

    abstract public void routeMessage(Message message);
    abstract public void registerClient(MessageTransportClient client);
    abstract public boolean addressKnown(MessageAddress address);

    void setBinder(MessageTransportServerBindingSite binder) {
	this.binder = binder;
    }

    protected MessageTransportServerBindingSite getBinder() {
	return binder;
    }

    public void setRecvQ(ReceiveQueue recvQ) {
	this.recvQ = recvQ;
    }


    // Hook for handling incoming remote route requests, only
    // called from our own MTImpl.
    public void rerouteMessage(Message message) {
	getBinder().deliverMessage(message);
    }
  
    

}
