/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.io.Serializable;

import org.cougaar.core.util.UID;

/**
 * A "lease denied" response from the ModifyService, indicating
 * a failed bind or lease renewal.
 * <p>
 * The UID will match the UID of the Record that has been denied.
 * <p>
 * Currently this can only be caused by server-side deconfliction
 * over race conditions, primarily based upon agent incarnation
 * numbers.
 * <p>
 * For example, say AgentX moves from NodeA to NodeB.
 * The following binds may be in progress:<pre>
 *   NodeA sends:
 *     AgentX={..., version=version:///1234/5678, ...}
 *   NodeB sends:
 *     AgentX={..., version=version:///1234/9999, ...}
 * </pre>
 * The format of the version entry URI is:<pre>
 *   version:///<i>incarnation</i>/<i>moveId</i>
 * </pre>
 * where the incarnation number is incremented per restart
 * (excluding moves) and the moveId is incremented per move
 * or restart (i.e. every time the agent is loaded).
 * The white pages servers will prefer the latest entries,
 * so it will deny NodeA's lease request.
 * <p>
 * Currently (see {@link Record}) this doesn't support bind-only
 * failures due to "already bound" entries.  The javadocs in
 * Record describe a proposed Map of failed-binds.
 */
public final class LeaseDenied implements Serializable {

  private final UID uid;
  private final Object reason;
  private final Object data;

  public LeaseDenied(
      UID uid,
      Object reason,
      Object data) {
    this.uid = uid;
    this.reason = reason;
    this.data = data;
    // validate
    String s =
      ((uid == null) ? "null uid" :
       (reason == null) ? "null reason" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The UID of the lease, as selected by the Record.
   * <p>
   * This is the "in response to" field.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The reason(s) for the failure.
   * <p>
   * This may indicate an "already bound" message for failed
   * "bind" requests, or some other failure condition.
   */
  public Object getReason() {
    return reason;
  }

  /**
   * The optional Record data.
   */
  public Object getData() {
    return data;
  }

  public String toString() {
    return
      "(lease-denied uid="+uid+
      " reason="+reason+
      " data="+data+")";
  }
}
