package org.cougaar.core.society;

import org.cougaar.core.component.BindingSite;

public interface MessageTransportServerBindingSite extends BindingSite
{
    // transport can call this on Server
    void deliverMessage(Message m);
    boolean isLocalAddress(MessageAddress address);
    String getID();
}
