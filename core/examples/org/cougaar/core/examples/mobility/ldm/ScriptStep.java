/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// flag modifiers; for infrastructure use only!
  public static final int ADD_PAUSE     = (1 <<  0);
  public static final int REL_PAUSE     = (1 <<  1);
  public static final int PRI_PAUSE     = (1 <<  2);
  public static final int ADD_TIMEOUT   = (1 <<  3);
  public static final int REL_TIMEOUT   = (1 <<  4);
  public static final int PRI_TIMEOUT   = (1 <<  5);

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

  @Override
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
      (hasFlag(PRI_PAUSE) ? "^" : "")+
      opts.getPauseTime()+
      ", "+
      (hasFlag(ADD_TIMEOUT) ? "+" : "")+
      (hasFlag(REL_TIMEOUT) ? "@" : "")+
      (hasFlag(PRI_TIMEOUT) ? "^" : "")+
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
