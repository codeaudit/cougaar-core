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

import java.util.List;

import org.cougaar.core.component.Service;

/**
 * This service is designed to provide a low-fidelity snapshot of the
 * current state of the ThreadServices.  It returns a list of Record
 * instances, one for each active or queued 'thread' (Schedulable).
 * The Record contains the name of the 'thread', the name of the
 * consumer for whom it was made, the name of the scheduler that made
 * it, the state (implicitly) and how long the schedulable has been in
 * that state, in ms).  These results are mostly useful as deugging
 * aids, for example in the Top servlet.
 */
public interface ThreadStatusService extends Service
{
    public static final String QUEUED = "queued";
    public static final String ACTIVE = "active";

    abstract public class Record {
	public String scheduler;
	public String consumer;
	public String schedulable;
	public long elapsed;

	abstract public String getState();
    }

    public class ActiveRecord extends Record {
	public String getState() { return ACTIVE; }
    }

    public class QueuedRecord extends Record {
	public String getState() { return QUEUED; }
    }


    public List getStatus();
}
