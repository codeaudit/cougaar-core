package org.cougaar.core.society;

interface DestinationQueue 
{
    public void holdMessage(Message message);
    public boolean matches(MessageAddress address);
    public boolean isEmpty();
    public Object next();
}
