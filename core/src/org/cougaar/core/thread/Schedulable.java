/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
