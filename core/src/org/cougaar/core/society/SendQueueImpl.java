package org.cougaar.core.society;


class SendQueueImpl extends MessageQueue implements SendQueue
{
    private Router router;
    private MessageTransportRegistry registry;

    SendQueueImpl(String name, 
		  Router router, 
		  MessageTransportRegistry registry) {
	super(name);
	this.router = router;
	this.registry = registry;
    }


    void dispatch(Message message) {
	router.routeMessage(message);
    }


    public void sendMessage(Message message) {
	add(message);
    }

    public boolean matches(String name) {
	return name.equals(getName());
    }

}
