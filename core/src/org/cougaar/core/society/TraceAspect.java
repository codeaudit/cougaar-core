package org.cougaar.core.society;

public class TraceAspect 
    implements MessageTransportAspect
{

    public TraceAspect() {
    }


    public Object getDelegate(Object delegate, Class iface) {
	if (iface == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) delegate);
	} else if (iface == Router.class) {
	    return new RouterDelegate((Router) delegate);
	} else if (iface == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegate);
	} else if (iface == ReceiveQueue.class) {
	    return new ReceiveQueueDelegate((ReceiveQueue) delegate);
	} else if (iface == ReceiveLink.class) {
	    return new ReceiveLinkDelegate((ReceiveLink) delegate);
	} else {
	    return null;
	}
    }


    public class SendQueueDelegate implements SendQueue
    {
	private SendQueue server;
	
	public SendQueueDelegate (SendQueue server)
	{
	    this.server = server;
	}
	
	public void sendMessage(Message message) {
	    System.err.print("SendQueue_");
	    server.sendMessage(message);
	}
	
	public boolean matches(String name){
	    return server.matches(name);
	}
    }


    public class RouterDelegate implements Router
    {
	private Router server;
	
	public RouterDelegate (Router server)
	{
	    this.server = server;
	}
	
	public void routeMessage(Message message) {
	    System.err.print("Router_");
	    server.routeMessage(message);
	}

    }



    public class DestinationQueueDelegate implements DestinationQueue
    {
	private DestinationQueue server;
	
	public DestinationQueueDelegate (DestinationQueue server)
	{
	    this.server = server;
	}
	
	public boolean isEmpty() {
	    return server.isEmpty();
	}

	public Object next() {
	    return server.next();
	}

	public void holdMessage(Message message) {
	    System.err.print("DestinationQueue_");
	    server.holdMessage(message);
	}
	
	public boolean matches(MessageAddress addr){
	    return server.matches(addr);
	}
    }


    public class ReceiveQueueDelegate implements ReceiveQueue
    {
	private ReceiveQueue server;
	
	public ReceiveQueueDelegate (ReceiveQueue server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    System.err.print("ReceiveQueue_");
	    server.deliverMessage(message);
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}
    }

 public class ReceiveLinkDelegate implements ReceiveLink
    {
	private ReceiveLink server;
	
	public ReceiveLinkDelegate (ReceiveLink server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    System.err.print("ReceiveLink_");
	    server.deliverMessage(message);
	}

    }
}



    
