/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.society.MessageAddress;
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
