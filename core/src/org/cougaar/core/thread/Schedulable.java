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


/** 
 * This interface takes the place of Thread and TimerTask in COUGAAR.
 * Aside from a few special internal cases, all Threads and Tasks in
 * COUGAAR should come from the ThreadService in the form of
 * Schedulables.  To treat a Schedulable like a Thread, use the start
 * method.  To treat a Schedulable like a TimerTask, use the schedule
 * methods.  The ThreadService is the only source of usable
 * Schedulables.
 * 
 */
public interface Schedulable
{
    /**
     * Starting a Schedulable is conceptually the same as starting a
     * Java Thread, with the following differences. First, if no
     * thread resources are available, the Schedulable will be queued
     * instead of running righy away.  It will only when enough
     * resources have become available for it to reach the head of the
     * queue. Second, if the Schedulable is running at the time of the
     * call, it will restart itself after the current run finishes
     * (unless it's cancelled in the meantime)..
     */ 
    void start();

    /**
     * Cancelling a Schedulable will prevent starting if it's
     * currently queued or from restarting if it was scheduled to do
     * so.  It will not cancel the current run.
    */
    boolean cancel();

    /**
     * Returns the current state of the Schedulable.  The states are
    described in org.cougaar.core.thread.CougaarThread.
    */
    int getState();

    /**
     * Returns the COUGAAR entity for whom the ThreadService made this
     * Schedulable.
    */
    Object getConsumer();


    /**
     * Lane
     */
    int getLane();


    /**
     * Other status methods, for the ThreadStatusService
     */

    long getTimestamp(); // start time

    String getName();

    int getBlockingType();

    String getBlockingExcuse();

    /**
     * The following methods behave more or less as they on
     * TimerTasks, except that the schedule methods can be called more
     * than once.  In that case, later calls effectively reschedule
     * the Schedulable.  Since 'cancel' was already in use, a new
     * method had to be introduced to cancel a scheduled task.  Thus
     * 'cancelTimer'.
    */
    void schedule(long delay);
    void schedule(long delay, long interval);
    void scheduleAtFixedRate(long delay, long interval);
    void cancelTimer();
}
