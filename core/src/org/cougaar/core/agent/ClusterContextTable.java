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

import java.util.HashMap;
import org.cougaar.core.mts.MessageAddress;

/** Keep track of the Agents (Clusters) running in the 
 * current VM so that we can reconnect newly-deserialized
 * objects in the Agent's processes.
 * <p>
 * @note This is a pretty dangerous security hole.
 **/
public final class ClusterContextTable {
  /**
   * VM-cluster registry.
   *  This table of clusterids to cluster objects is available
   * via the package-protected method findContext so that the deserialization 
   * stream can hook objects back up to the environment when
   * they are read.
   **/
  private static final HashMap contextTable = new HashMap(89);
  
  /** find the agent named by the parameter in my local VM.
   * Anyone caught using this in plugins will be shot.
   **/
  static ClusterContext findContext(MessageAddress cid) {
    synchronized (contextTable) {
      return (ClusterContext) contextTable.get(cid);
    }
  }

  /** Add a context to the context table **/
  static void addContext(final MessageAddress cid) {
    ClusterContext c = new ClusterContext() {
      public MessageAddress getMessageAddress() {
        return cid;
      }
    };
    synchronized (contextTable) {
      contextTable.put(cid, c);
    }
  }
  
  /** Remove a context from the context table **/
  static void removeContext(MessageAddress cid) {
    synchronized (contextTable) {
      contextTable.remove(cid);
    }
  }

  /** The thread-local "current" context **/
  private static final ThreadLocal theContext = new ThreadLocal() {};

  /** Internal object for keeping track of contexts. **/
  public static class ContextState {
    private ClusterContext cc;
    public ContextState(ClusterContext c) {
      cc = c;
    }
    public final ClusterContext getClusterContext() { return cc; }
  }

  public static final class MessageContext extends ContextState {
    private MessageAddress from;
    private MessageAddress to;
    public MessageContext(ClusterContext c, MessageAddress f, MessageAddress t) {
      super(c);
      from = f;
      to = t;
    }
    public final MessageAddress getFromAddress() { return from; }
    public final MessageAddress getToAddress() { return to; }
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
    withContextState(new ContextState(cc), thunk);
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair **/
  public static final void withClusterContext(MessageAddress ma, Runnable thunk) {
    ClusterContext cc = findContext(ma);
    if (cc == null) {
      throw new IllegalArgumentException("Address \""+ma+"\" is not an Agent on this node.");
    } else {
      withContextState(new ContextState(cc), thunk);
    }
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair **/
  public static final void withMessageContext(MessageAddress ma, MessageAddress from, MessageAddress to, 
                                              Runnable thunk) {
    ClusterContext cc = getClusterContext();
    if (cc == null) {
      cc = findContext(ma);
      if (cc != null) {
        // normal case for 99% of the time
        withContextState(new MessageContext(cc, from, to), thunk);
      } else {
        // target is not on this node
        //
        // This is likely due to a race between remote MTS routing and
        // local agent removal for mobility (bug 1316).  The remote MTS
        // will catch this exception and re-route the message.
        throw new IllegalArgumentException(
            "Address \""+ma+"\" is not an Agent on this node.");
      }
    } else {
      MessageAddress oldMA = (MessageAddress)cc.getMessageAddress();
      if ((ma != null) ? ma.equals(oldMA) : (oldMA == null)) {
        // valid nesting, but rare in practice
        withContextState(new MessageContext(cc, from, to), thunk);
      } else {
        // unusual nested context, use the dummy context since its
        // not valid for the nested message to access a different agent
        //
        // In agent mobility this occurs when the node transfers agent
        // state that contains unsent messages (bug 1629 + bug 1634).
        // When the agent is created it will sent these nested messages
        // itself.
        withEmptyClusterContext(thunk);
      }
    }
  }

  public static final void withContextState(ContextState cs, Runnable thunk) {
    ContextState old = (ContextState) theContext.get();
    theContext.set(cs);
    try {
      thunk.run();
    } finally {
      theContext.set(old);
    }
  }
}
