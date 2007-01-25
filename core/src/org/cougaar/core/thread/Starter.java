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

/**
 * This native Java thread helps the standard thread service scheduler
 * to use its pool efficiently.  When a {@link Schedulable} has been
 * given the right to run, it's placed on the Starter for the actual
 * starting.  See also {@link Reclaimer}.
 * 
 * This is a singleton; there should never be more than one per Node.
 */
final class Starter extends Thread
{

    // At least one thread must be a non-daemon thread, otherwise the JVM
    // will exit.  We'll mark this thread as our non-daemon "keep-alive".
    private static final boolean DAEMON =
        SystemProperties.getBoolean("org.cougaar.core.thread.daemon");

    private static Starter singleton;

    static void startThread()  {
	singleton = new Starter();
	singleton.start();
    }

    static void push(SchedulableObject schedulable) {
	synchronized (singleton.lock) {
	    singleton.queue.add(schedulable);
	    singleton.lock.notify();
	}
    }


    private CircularQueue queue;
    private Object lock;

    private Starter() {
	super("Scheduler Starter");
	setDaemon(DAEMON);
	queue = new CircularQueue();
	lock = new Object();
    }

    private void dequeue() {
	SchedulableObject schedulable = null;
	while (true) {
	    synchronized (lock) {
		if (queue.isEmpty()) return;
		schedulable = (SchedulableObject) queue.next();
	    }
	    schedulable.getScheduler().startOrQueue(schedulable);
	}
    }

    public void run() {
	while (true) {
	    dequeue();
	    synchronized (lock) {
		while (queue.isEmpty()) {
		    try { 
			lock.wait();
			break;
		    } catch (InterruptedException ex) {
		    }
		}
	    }
	}
    }

}
