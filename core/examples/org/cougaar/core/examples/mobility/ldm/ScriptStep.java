/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.examples.mobility.ldm;

import java.io.Serializable;
import org.cougaar.core.mobility.Ticket;

/**
 * Script wrapper for StepOptions, allowing scripting
 * override for the otherwise immutable options.
 */
public class ScriptStep 
implements Script.Entry, Serializable 
{

  // flag modifiers; for infrastructure use only!
  public static final int ADD_PAUSE     = (1 <<  0);
  public static final int REL_PAUSE     = (1 <<  1);
  public static final int ADD_TIMEOUT   = (1 <<  2);
  public static final int REL_TIMEOUT   = (1 <<  3);

  public final int flags;

  // maybe replace with strings, to allow variable 
  // substitutions at runtime
  public final StepOptions opts;

  public ScriptStep(int flags, StepOptions opts) {
    this.flags = flags;
    this.opts = opts;
    if (opts == null) {
      throw new IllegalArgumentException(
          "null opts");
    }
  }

  public int getFlags() {
    return flags;
  }

  public boolean hasFlag(int f) {
    // helper
    return ((flags & f) != 0);
  }

  public StepOptions getStepOptions() {
    return opts;
  }

  public String toString() {
    Ticket t = opts.getTicket();
    return 
      "move "+
      ((opts.getTarget() != null) ? 
       opts.getTarget().toString() :
       "")+
      ", "+
      (hasFlag(ADD_PAUSE) ? "+" : "")+
      (hasFlag(REL_PAUSE) ? "@" : "")+
      opts.getPauseTime()+
      ", "+
      (hasFlag(ADD_TIMEOUT) ? "+" : "")+
      (hasFlag(REL_TIMEOUT) ? "@" : "")+
      opts.getTimeoutTime()+
      ", "+
      ((t.getMobileAgent() != null) ? 
       t.getMobileAgent().toString() : 
       "")+
      ", "+
      ((t.getOriginNode() != null) ? 
       t.getOriginNode().toString() : 
       "")+
      ", "+
      ((t.getDestinationNode() != null) ? 
       t.getDestinationNode().toString() : 
       "")+
      ", "+
      t.isForceRestart();
  }
}
