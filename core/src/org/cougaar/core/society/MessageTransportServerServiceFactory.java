package org.cougaar.core.society;

import org.cougaar.core.component.ServiceProvider;

public interface MessageTransportServerServiceFactory 
    extends ServiceProvider
{

    public NameServer getDefaultNameServer();
}

