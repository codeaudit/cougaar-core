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

import java.util.Timer;
import java.util.TimerTask;

class TrivialSchedulable implements Schedulable
{
    private Object consumer;
    private Thread thread;
    private Runnable runnable;
    private String name;
    private int start_count;
    private boolean cancelled;
    private TimerTask task;

    TrivialSchedulable(Runnable runnable, 
		       String name,
		       Object consumer) 
    {
        this.runnable = runnable;
        if (name == null)
            this.name =  TrivialThreadPool.pool().generateName();
        else
            this.name = name;
        this.consumer = consumer;
	this.start_count = 0;
    }


    Runnable getRunnable()
    {
	return runnable;
    }

    public int getLane() 
    {
	return -1;
    }

    String getName() 
    {
	return name;
    }

    public String toString() 
    {
        return "<TrivialSchedulable " 
	    +(name == null ? "anonymous" : name)+ 
	    " for " +consumer+ ">";
    }

    public Object getConsumer() 
    {
        return consumer;
    }

    // caller synchronizes
    private void thread_start() 
    {
	start_count = 1; // forget any extra intervening start() calls
	thread = runThread();
    }

    Thread runThread()
    {
	return TrivialThreadPool.pool().getThread(this, runnable, name);
    }

    void thread_stop() 
    {
	// If start_count > 1, start() was called while the
	// Schedulable was running.  Now that it's finished,  start it
	// again. 
	boolean restart = false;
	synchronized (this) {
	    restart = --start_count > 0;
	    thread = null;
	    if (restart) thread_start();
	}
    }

    public void start() 
    {
        synchronized (this) {
	    // If the Schedulable has been cancelled, or has already
	    // been asked to start, there's nothing further to do.
            if (cancelled) return;
            if (++start_count > 1) return;
	    thread_start();
	}
    }

    
    private TimerTask task() 
    {
	cancelTimer();
	task = new TimerTask() {
		public void run() {
		    start();
		}
	    };
	return task;
    }

    public synchronized void schedule(long delay) 
    {
	TreeNode.timer().schedule(task(), delay);
    }


    public synchronized void schedule(long delay, long interval) 
    {
	TreeNode.timer().schedule(task(), delay, interval);
    }

    public synchronized void scheduleAtFixedRate(long delay, long interval)
    {
	TreeNode.timer().scheduleAtFixedRate(task(), delay, interval);
    }


    public synchronized void cancelTimer() 
    {
	if (task != null) task.cancel();
	task = null;
    }

    public synchronized int getState() 
    {
        if (thread != null)
            return CougaarThread.THREAD_RUNNING;
        else
            return CougaarThread.THREAD_DORMANT;
    }

    public boolean cancel() 
    {
        synchronized (this) {
	    cancelTimer();
            cancelled = true;
	    start_count = 0;
            if (thread != null) {
                // Currently running. 
                return false;
            } 
            return true;
        }

    }

}
