package org.cougaar.core.society;

import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.BindingSite;

public class MessageTransportServerBinder 
    extends BinderSupport
    implements MessageTransportServerBindingSite
{
    public MessageTransportServerBinder(BinderFactory bf, Object child)
    {
      super(bf, child);
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
	((MessageTransport) getComponent()).routeMessage(m);
    }

    void registerClient(MessageTransportClient client) {
	((MessageTransport) getComponent()).registerClient(client);
    }

    boolean addressKnown(MessageAddress address) {
	return ((MessageTransport) getComponent()).addressKnown(address);
    }

  protected final BindingSite getBinderProxy() {
    // horribly unsecure! Means that the component has full access to the binder.
    return (BindingSite) this;
  }

}
