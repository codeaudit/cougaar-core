package org.cougaar.core.society;


interface SendQueue 
{
    public void sendMessage(Message message);   
    public boolean matches(String name);

}
