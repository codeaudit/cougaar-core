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

package org.cougaar.core.service;

/**
 * An agent-level view of the society topology.
 *
 * @see TopologyReaderService
 */
public final class TopologyEntry implements java.io.Serializable {

  // status values
  public static final int UNKNOWN = 0;
  public static final int ACTIVE  = 1;
  public static final int MOVING  = 2;
  // others TBA (unavailable, dead, etc)

  private final String enclave;
  private final String site;
  private final String host;
  private final String node;
  private final String agent;
  private final long incarnation;
  private final long moveId;
  private final boolean isNode;
  private final int status;

  public TopologyEntry(
      String agent,
      String node,
      String host,
      String site,
      String enclave,
      long incarnation,
      long moveId,
      boolean isNode,
      int status) {
    this.agent = agent;
    this.node = node;
    this.host = host;
    this.site = site;
    this.enclave = enclave;
    this.incarnation = incarnation;
    this.moveId = moveId;
    this.isNode = isNode;
    this.status = status;
  }

  public String getAgent() { return agent; }
  public String getNode() { return node; }
  public String getHost() { return host; }
  public String getSite() { return site; }
  public String getEnclave() { return enclave; }
  public long getIncarnation() { return incarnation; }
  public long getMoveId() { return moveId; }
  public boolean isNode() { return isNode; }
  public int getStatus() { return status; }

  public String getStatusAsString() {
    switch (status) {
      case UNKNOWN: return "unknown";
      case ACTIVE: return "active";
      case MOVING: return "moving";
      default: return "invalid("+status+")";
    }
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof TopologyEntry)) return false;
    TopologyEntry te = (TopologyEntry) o;
    return
      (agent.equals(te.agent) &&
       (incarnation == te.incarnation) &&
       (moveId == te.moveId) &&
       (status == te.status) &&
       node.equals(te.node) &&
       host.equals(te.host) &&
       site.equals(te.site) &&
       enclave.equals(te.enclave) &&
       (isNode == te.isNode));
  }

  public int hashCode() {
    return
      (agent.hashCode() ^
       ((int) incarnation));
  }

  public String toString() {
    return
      "Topology entry {"+
      "\n  agent:    "+agent+
      "\n  node:     "+node+
      "\n  host:     "+host+
      "\n  site:     "+site+
      "\n  enclave:  "+enclave+
      "\n  incarnation: "+incarnation+
      "\n  move id: "+moveId+
      "\n  is node: "+isNode+
      "\n  status: "+getStatusAsString()+
      "\n}";
  }
}
