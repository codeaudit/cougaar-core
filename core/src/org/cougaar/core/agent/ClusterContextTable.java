/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.mts.MessageAddress;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

public final class ClusterContextTable {
  /**
   * VM-cluster registry.
   *  This table of clusterids to cluster objects is available
   * via the public findCluster so that the deserialization 
   * stream can hook objects back up to the environment when
   * they are read.
   **/
  private static final HashMap contextTable = new HashMap(89);
  
  /** find the cluster named by CID in my local VM.
   * Anyone caught using this in plugins will be shot.
   **/
  public static ClusterContext findContext(ClusterIdentifier cid) {
    return (ClusterContext) contextTable.get(cid);
  }

  public static void addContext(ClusterIdentifier cid, ClusterContext c) {
    contextTable.put(cid, c);
  }
  
  public static void removeContext(ClusterIdentifier cid) {
    contextTable.remove(cid);
  }

  private static final ThreadLocal theContext = new ThreadLocal() {};

  public static class ContextState {
    protected ClusterContext cc;
    public ContextState(ClusterContext c) {
      cc = c;
    }
    public final ClusterContext getClusterContext() { return cc; }
  }
    

  public static final class MessageContext extends ContextState {
    protected MessageAddress from;
    protected MessageAddress to;
    public MessageContext(ClusterContext c, MessageAddress f, MessageAddress t) {
      super(c);
      from = f;
      to = t;
    }
    public final MessageAddress getFromAddress() { return from; }
    public final MessageAddress getToAddress() { return to; }
  }

  public static void enterContext(ClusterContext c) {
    // check for lossage - shouldn't be possible.
    if (theContext.get() != null) {
      System.err.println("Nested cluster contexts detected: was "+theContext.get()+" will be "+c);
    }

    theContext.set(new ContextState(c));
  }

  public static void enterContext(ClusterContext c, MessageAddress from, MessageAddress to) {
    // check for lossage - shouldn't be possible.
    if (theContext.get() != null) {
      System.err.println("Nested cluster contexts detected: was "+theContext.get()+" will be "+c);
    }

    theContext.set(new MessageContext(c, from, to));
  }
  public static void exitContext() {
    theContext.set(null);
  }

  public static ContextState getContextState() {
    return ((ContextState)theContext.get());
  }
  public static MessageContext getMessageContext() {
    Object cs = theContext.get();
    return (cs instanceof MessageContext)?((MessageContext)cs):null;
  }
  public static ClusterContext getClusterContext() {
    ContextState cs = (ContextState)theContext.get();
    
    return (cs!=null)?cs.getClusterContext():null;
  }

  private static final ClusterContext _dummyContext = new ClusterContext.DummyClusterContext();
  /** May be used by non-society classes to provide an empty context
   * for deserialization of objects sent from within a society.
   * The resulting instances may still be "broken" in various ways, wrapping
   * this call around the deserialization will at least allow 
   * avoiding warning messages.  <em>WARNING</em>:  This must <em>never</em> be
   * used within society code.
   **/
  public static final void withEmptyClusterContext(Runnable thunk) {
    withClusterContext(_dummyContext, thunk);
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair **/
  public static final void withClusterContext(ClusterContext cc, Runnable thunk) {
    try {
      enterContext(cc);
      thunk.run();
    } finally {
      exitContext();
    }
  }

}
