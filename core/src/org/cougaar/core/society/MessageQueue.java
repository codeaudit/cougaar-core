package org.cougaar.core.society;

import org.cougaar.util.CircularQueue;

abstract class MessageQueue extends Thread
{
    private CircularQueue queue;

    MessageQueue(String name) {
	super(name);
	queue = new CircularQueue();
	start();
    }

    public void run() {
	while (true) {
	    Message m;
	    // wait for a message to handle
	    synchronized (queue) {
		while (queue.isEmpty()) {
		    try {
			queue.wait();
		    } catch (InterruptedException e) {} // dont care
		}
        
		m = (Message) queue.next(); // from top
	    }

	    if (m != null) {
		dispatch(m);
	    }
	}
    }

    void add(Message m) {
	synchronized (queue) {
	    queue.add(m);
	    queue.notify();
	}
    }



    abstract void dispatch(Message m);

}
