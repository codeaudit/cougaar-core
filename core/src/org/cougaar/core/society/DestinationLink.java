package org.cougaar.core.society;


public interface DestinationLink
{
    public void forwardMessage(Message message);
    public int cost(Message message);
}
