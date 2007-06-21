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

import org.cougaar.util.CircularQueue;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This native Java thread helps the standard thread service scheduler
 * to use its pool efficiently.  When a pooled thread ends a given
 * run, it places itself on the Reclaimer so that it can be reused.
 * See also {@link Starter}.
 * 
 * This is a singleton; there should never be more than one per Node.
 */
final class Reclaimer extends Thread
{

    static Reclaimer singleton;

    static void startThread() {
	singleton = new Reclaimer();
	singleton.start();
    }

    static void stopThread() {
        Reclaimer instance = singleton;
        if (instance == null) return;
        singleton = null;
        instance.quit();
        try {
            instance.join();
        } catch (InterruptedException ie) {
        }
    }

    static void push(SchedulableObject schedulable) {
        Reclaimer instance = singleton;
        if (instance == null) {
            Logger logger = Logging.getLogger(Reclaimer.class);
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

    private Reclaimer() {
	super("Scheduler Reclaimer");
	setDaemon(true);
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
	    // Verify that the Schedulable isn't already on the
	    // queue.  This is potentially expensive and in theory
	    // should never happen.  But it does, so until we know
	    // why, we need to check.
	    if (queue.contains(schedulable)) {
		Logger logger = Logging.getLogger(Reclaimer.class);
		logger.error(schedulable + " is already in the Reclaimer queue" +
			" Start Count=" +schedulable.getStartCount());
		// XXX: Figure out why this happens !!
		return;
	    }
	    queue.add(schedulable);
	    lock.notify();
	}	
    }
    
    public void run() {
        while (true) {
            // dequeue
            SchedulableObject schedulable;
            synchronized (lock) {
                while (true) {
                    if (should_stop) return;
                    if (!queue.isEmpty()) break;
                    try { 
                        lock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                schedulable = (SchedulableObject) queue.next();
            }

            // reclaim
	    schedulable.reclaimNotify();
        }
    }

}
