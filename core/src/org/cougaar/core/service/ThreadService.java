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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.thread.Schedulable;

/**
 * The ThreadService provides Schedulables for use by a COUGAAR
 * objects (known as the 'consumer') The body of code to be run is
 * passed in as a Runnable.  An optional name can be passed in as
 * well, and should be passed in if at all possible.
 */
public interface ThreadService extends Service
{
    public static final int BEST_EFFORT_LANE  = 0;
    public static final int WILL_BLOCK_LANE   = 1;
    public static final int CPU_INTENSE_LANE  = 2;
    public static final int WELL_BEHAVED_LANE = 3;

    public static final int LANE_COUNT = 4;

    Schedulable getThread(Object consumer, Runnable runnable);
    Schedulable getThread(Object consumer, Runnable runnable, String name);

    Schedulable getThread(Object consumer, Runnable runnable, String name,
			  int lane);

    /**
     * @deprecated Use the schedule methods on Schedulable.
     */
    void schedule(java.util.TimerTask task, long delay);

    /**
     * @deprecated Use the schedule methods on Schedulable.
     */
    void schedule(java.util.TimerTask task, long delay, long interval);

    /**
     * @deprecated Use the schedule methods on Schedulable.
     */
    void scheduleAtFixedRate(java.util.TimerTask task, long delay, long interval);

}
