/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.thread;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.CircularQueue;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This native Java thread helps the standard thread service scheduler
 * to use its pool efficiently.  When a {@link Schedulable} has been
 * given the right to run, it's placed on the Starter for the actual
 * starting.  See also {@link Reclaimer}.
 * 
 * This is a singleton; there should never be more than one per Node.
 */
final public class Starter extends Thread
{

    // At least one thread must be a non-daemon thread, otherwise the JVM
    // will exit.  We'll mark this thread as our non-daemon "keep-alive".
    private static final boolean DAEMON =
        SystemProperties.getBoolean("org.cougaar.core.thread.daemon");

    public static Starter singleton;

    static void startThread()  {
	singleton = new Starter();
	singleton.start();
    }

    static void stopThread() {
        Starter instance = singleton;
        if (instance == null) return;
        singleton = null;
        instance.quit();
        try {
            instance.join();
        } catch (InterruptedException ie) {
        }
    }

    static void push(SchedulableObject schedulable) {
        Starter instance = singleton;
        if (instance == null) {
            Logger logger = Logging.getLogger(Starter.class);
            if (logger.isWarnEnabled()) {
                logger.warn("Ignoring enqueue request on stopped thread");
            }
            return;
        }
        instance.add(schedulable);
    }


    private final CircularQueue queue;
    private final Object lock;
    private boolean should_stop;
    private SchedulableObject schedulable;
    private boolean wasWoken;

    private Starter() {
	super("Scheduler Starter");
	setDaemon(DAEMON);
	queue = new CircularQueue();
	lock = new Object();
    }
    
    private void quit() {
	synchronized (lock) {
	    should_stop = true;
	    lock.notify();
	}
    }
    
    private void add(SchedulableObject schedulable) {
	synchronized (lock) {
	    queue.add(schedulable);
	    lock.notify();
	}	
    }
    
    public void wakeup() {
	synchronized (lock) {
	    wasWoken = true;
	    lock.notifyAll();
	}
    }
    
    public SchedulableObject getLastSchedulable() {
	return schedulable;
    }
    
    public CircularQueue getQueue() {
	return queue;
    }
    
    public void run() {
	while (true) {
	    synchronized (lock) {
		while (true) {
		    if (should_stop) return;
		    if (!queue.isEmpty()) break;
		    try { 
			lock.wait();
			if (wasWoken) {
			    System.out.println("woken");
			    wasWoken = false;
			}
		    } catch (InterruptedException ex) {
		    }
		}
		schedulable = (SchedulableObject) queue.next();
	    }
	    schedulable.getScheduler().startOrQueue(schedulable);
	}
    }

}
