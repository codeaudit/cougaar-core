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

final class SchedulableObject implements Schedulable
{
    private long timestamp;
    private Object consumer;
    private ThreadPool pool;
    private Scheduler scheduler;
    private ThreadPool.PooledThread thread;
    private Runnable runnable;
    private String name;
    private int start_count;
    private boolean cancelled;
    private boolean queued;
    private boolean disqualified;
    private TimerTask task;
    private int blocking_type = SchedulableStatus.NOT_BLOCKING;
    private String blocking_excuse;
    private int lane;

    SchedulableObject(TreeNode treeNode, 
                      Runnable runnable, 
                      String name,
                      Object consumer,
		      int lane) 
    {
	this.lane = lane;
        this.pool = treeNode.getPool(lane);
        this.scheduler = treeNode.getScheduler(lane);
        this.runnable = runnable;
        if (name == null)
            this.name =  pool.generateName();
        else
            this.name = name;
        this.consumer = consumer;
	this.start_count = 0;
    }


    public int getLane()
    {
	return lane;
    }

    String getBlockingExcuse () {
	return blocking_excuse;
    }

    int getBlockingType() {
	return blocking_type;
    }


    void setBlocking(int type, String excuse) {
	blocking_type = type;
	blocking_excuse = excuse;
    }

    void clearBlocking() {
	blocking_excuse = null;
	blocking_type = SchedulableStatus.NOT_BLOCKING;
    }

    String getName() {
        return name;
    }

    Scheduler getScheduler() {
        return scheduler;
    }

    public String toString() {
        return "<Schedulable " 
	    +(name == null ? "anonymous" : name)+ 
	    " for " +consumer+ ">";
    }

    long getTimestamp() {
        return timestamp;
    }

    void setQueued(boolean flag) {
        queued = flag;
        if (flag) timestamp = System.currentTimeMillis();
    }

    boolean isQueued() {
        return queued;
    }


    boolean isDisqualified() {
        return disqualified;
    }

    void setDisqualified(boolean flag) {
        disqualified = flag;
        if (flag) queued = false;
    }

    public Object getConsumer() {
        return consumer;
    }



    void claim() {
        // thread has started or restarted
        scheduler.threadClaimed(this);
    }






    void reclaim() {
        // Notify listeners
        synchronized (this) { 
            thread = null;
        }
        scheduler.threadReclaimed(this);
    }

    // Callback from the Reclaimer.
    void reclaimNotify() {
        scheduler.releaseRights(scheduler, this);

	// If start_count > 1, start() was called while the
	// Schedulable was running.  Now that it's finished,  start it
	// again. 
	boolean restart = false;
	synchronized (this) {
	    restart = --start_count > 0;
	}
        if (restart) Starter.push(this);
    }

    void thread_start() {
        synchronized (this) {
            start_count = 1; // forget any extra intervening start() calls
            queued = false;
            thread = pool.getThread(runnable, name);
            thread.start(this);
        }
    }

    public void start() {
        synchronized (this) {
	    // If the Schedulable has been cancelled, or has already
	    // been asked to start, there's nothing further to do.
            if (cancelled) return;
            if (++start_count > 1) return;
        }

	// We only get here if the Schedulable has not been
	// cancelled  and if start_count has gone from 0 to 1.
        Starter.push(this);
    }

    
    private TimerTask task() {
	cancelTimer();
	task = new TimerTask() {
		public void run() {
		    start();
		}
	    };
	return task;
    }

    public synchronized void schedule(long delay) {
	Timer timer = scheduler.getTreeNode().timer();
	timer.schedule(task(), delay);
    }


    public synchronized void schedule(long delay, long interval) 
    {
	Timer timer = scheduler.getTreeNode().timer();
	timer.schedule(task(), delay, interval);
    }

    public synchronized void scheduleAtFixedRate(long delay, long interval)
    {
	Timer timer = scheduler.getTreeNode().timer();
	timer.scheduleAtFixedRate(task(), delay, interval);
    }


    public synchronized void cancelTimer() 
    {
	if (task != null) task.cancel();
	task = null;
    }

    public synchronized int getState() {
        // Later add a 'disqualified' state
        if (queued)
            return CougaarThread.THREAD_PENDING;
        else if (thread != null)
            return CougaarThread.THREAD_RUNNING;
        else
            return CougaarThread.THREAD_DORMANT;
    }

    public boolean cancel() {
        synchronized (this) {
	    cancelTimer();
            cancelled = true;
	    start_count = 0;
            if (thread != null) {
                // Currently running. 
                return false;
            } 
            if (queued) scheduler.dequeue(this);
            queued = false;
            return true;
        }

    }

}
