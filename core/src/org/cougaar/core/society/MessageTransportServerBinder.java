package org.cougaar.core.society;

import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.Services;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.Component;

public class MessageTransportServerBinder 
    extends BinderSupport
    implements MessageTransportServerBindingSite
{
    private Container container;

    public MessageTransportServerBinder(Container container,
					Component child)
    {
	super(null, container, child);
	this.container = container;
    }

    // not present yet (?) in BinderSupport
    Container getContainer() {
	return container;
    }

    public void deliverMessage(Message m) {
	((MessageTransportServerImpl) getContainer()).deliverMessage(m);
    }


    public boolean isLocalAddress (MessageAddress address) {
	return ((MessageTransportServerImpl) getContainer()).isLocalAddress(address);
    }

    public String getID() {
	return ((MessageTransportServerImpl) getContainer()).getIdentifier();
    }



    // Also allow downcalls from parent, but don't advertise this
    // method in the declared interface (not public)
    void routeMessage(Message m) {
	((MessageTransport) getChildComponent()).routeMessage(m);
    }

    void registerClient(MessageTransportClient client) {
	((MessageTransport) getChildComponent()).registerClient(client);
    }

    boolean addressKnown(MessageAddress address) {
	return ((MessageTransport) getChildComponent()).addressKnown(address);
    }


}
