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

/**
 * This is a very simple aspect which is mostly for demonstration
 * purposes.  It attaches aspect delegates to each interface, and each
 * such delegate prints a message to System.err.  This provides a
 * trivial trace of a message as it passes through the various stages
 * of the message transport subsystem.  */
public class TraceAspect 
    implements MessageTransportAspect
{

    public TraceAspect() {
    }


    public Object getDelegate(Object delegate, int cutpoint) {
	switch (cutpoint) {
	case ServiceProxy:
	    return new ServiceProxyDelegate((MessageTransportServer) delegate);

	case SendQueue:
	    return new SendQueueDelegate((SendQueue) delegate);

	case Router:
	    return new RouterDelegate((Router) delegate);

	case DestinationQueue:
	    return new DestinationQueueDelegate((DestinationQueue) delegate);

	case DestinationLink:
	    return new DestinationLinkDelegate((DestinationLink) delegate);

	case ReceiveQueue:
	    return new ReceiveQueueDelegate((ReceiveQueue) delegate);

	case ReceiveLink:
	    return new ReceiveLinkDelegate((ReceiveLink) delegate);

	default:
	    return null;
	}
    }


    public class ServiceProxyDelegate implements MessageTransportServer
    {
	private MessageTransportServer server;
	
	public ServiceProxyDelegate (MessageTransportServer server) {
	    this.server = server;
	}

	public void sendMessage(Message message) {
	    server.sendMessage(message);
	}

	public void registerClient(MessageTransportClient client) {
	    server.registerClient(client);
	}

	public void addMessageTransportWatcher(MessageTransportWatcher watcher)
	{
	    server.addMessageTransportWatcher(watcher);
	}

	public String getIdentifier() {
	    return server.getIdentifier();
	}

	public boolean addressKnown(MessageAddress address) {
	    return server.addressKnown(address);
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


    public class DestinationLinkDelegate implements DestinationLink
    {
	private DestinationLink server;
	
	public DestinationLinkDelegate (DestinationLink server)
	{
	    this.server = server;
	}
	
	public void forwardMessage(Message message) {
	    System.err.print("DestinationLink_");
	    server.forwardMessage(message);
	}
	
	public int cost(Message message){
	    return server.cost(message);
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



    
