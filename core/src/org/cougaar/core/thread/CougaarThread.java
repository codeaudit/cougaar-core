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
 * Defines a set of Schedulable state constants.  The only states
 * supported right now are RUNNING (active), PENDING (queued), and
 * DORMANT (neither).  We may eventually have a use for DISQUALIFIED
 * (the ThreadControlService has prevented it from running).
 * SUSPENDED is here for historical reasons and will almost never be
 * supported. 
 * 
 * Note that these states are purely for informational purposes.
 * They're not used internally in any way.
 */
public interface CougaarThread
{

    /**
     * The Schedulable is currently running. 
     */
    public static final int THREAD_RUNNING = 0;

    /**
     * Not supported, but would in theory mean the Schedulable has
    suspended itself.
    */
    public static final int THREAD_SUSPENDED = 1;

    /**
     * The Schedulable is currently queued. 
     */
    public static final int THREAD_PENDING = 2;

    /**
     * The Schedulable is qualified but neither running nor queued. 
     */
    public static final int THREAD_DORMANT = 3;


    /**
     * The Schedulable has been disqualified by the ThreadControlService.
     */
    public static final int THREAD_DISQUALIFIED = 4;


}
