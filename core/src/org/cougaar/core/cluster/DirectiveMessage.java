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

package org.cougaar.core.cluster;

import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.util.StringUtility;
import org.cougaar.domain.planning.ldm.plan.Plan;

import java.util.Collection;
import java.io.*;

/**
 * A org.cougaar.core.cluster.DirectiveMessage  provides a basic implementation of 
 *  DirectiveMessage
 */

public class DirectiveMessage extends ClusterMessage
  implements Externalizable
{
  private transient Directive[] directives;

  /**
   * This signals that all messages prior to this have been acked.
   * Used in keep alive messages to detect out-of-sync condition.
   **/
  private boolean allMessagesAcknowledged = false;
   
  /** 
   *	no-arg Constructor.
   *  	@return org.cougaar.core.cluster.DirectiveMessage
   */
  public DirectiveMessage() {
    super();
  }
    
  /** constructor that takes a directive
   * @param aDirective
   * @return org.cougaar.core.cluster.DirectiveMessage
   */
  public DirectiveMessage(Directive[] someDirectives) {
    directives = someDirectives;
  }
    
  /** constructor that takes source, destination and a directive
   * @param source
   * @param destination
   * @param aDirective
   * @return org.cougaar.core.cluster.DirectiveMessage
   */
  public DirectiveMessage(ClusterIdentifier source, ClusterIdentifier destination,
                          long incarnationNumber,
                          Directive[] someDirectives) 
  {
    super(source, destination, incarnationNumber);
    directives = someDirectives;
  }
    
  /** getDirectives method
   * Returns an array of the directives in this message.  
   * @return Directive[]
   **/
    
  public Directive[] getDirectives() {
    return directives;
  }
    
  /** setDirective method
   * Sets an object that represents the directive
   * that this message is in reference to.
   * @param aDirective
   **/

  public void setDirectives(Directive[] someDirectives) {
    directives = someDirectives;
  }

  public void setAllMessagesAcknowledged(boolean val) {
    allMessagesAcknowledged = val;
  }

  public boolean areAllMessagesAcknowledged() {
    return allMessagesAcknowledged;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<DirectiveMessage "+getSource()+" - "+getDestination());
    if (directives == null) {
      buf.append("(Null directives)");
    } else {
      StringUtility.appendArray(buf, directives);
    }
    buf.append(">");
    return buf.substring(0);
  }

  /**
   **/
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    ClusterIdentifier cid = getSource();
    ClusterContext context = ClusterContextTable.findContext(cid);
    if (context == null) {
      System.err.println("Directive Message Sent from "+cid+" before Context is known, Ignored");
      Thread.dumpStack();
    } else {
      try {
        ClusterContextTable.enterContext(context, getSource(), getDestination());
        stream.writeInt(directives.length);
        for (int i = 0; i < directives.length; i++) {
         stream.writeObject(directives[i]);
        }
      } catch (IOException e) { // These can happen anytime communications is lost. be quiet.
        throw e;                // In case anyone else is interested
      } catch (Exception e) {
        throw new IOException("Caught Exception while serializing a DirectiveMessage:" + e);
      } finally {
        ClusterContextTable.exitContext();
      }
    }
  }

  /** when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the Cluster mechanism
   * @see ClusterContextTable
   **/
  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();

    // find the cluster
    ClusterIdentifier cid = getDestination();
    ClusterContext context = ClusterContextTable.findContext(cid);
    if (context == null) {
      System.err.println("Directive Message read in "+cid+" before Context is known.");
    }
    
    try {
      ClusterContextTable.enterContext(context, getSource(), getDestination());
      directives = new Directive[stream.readInt()];
      for (int i = 0; i < directives.length; i++) {
        directives[i] = (Directive) stream.readObject();
      }
    } finally {
      ClusterContextTable.exitContext();
    }
  }

  // Externalizable support
  /*
  **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);   // Message

    out.writeBoolean(allMessagesAcknowledged);

    // serialize directive through another stream
    ClusterIdentifier cid = getSource();
    ClusterContext context = ClusterContextTable.findContext(cid);

    if (context == null) {
      System.err.println("Directive Message Sent from "+cid+" before Context is known, Ignored");
      Thread.dumpStack();
    } else {
      try {
        ClusterContextTable.enterContext(context, getSource(), getDestination());
        out.writeInt(directives.length);
        for (int i = 0; i < directives.length; i++) {
          out.writeObject(directives[i]);
        }
      } catch (IOException e) { // These can happen anytime communications is lost. be quiet.
        throw e;                // In case anyone else is interested
      } catch (Exception e) {
        throw new IOException("Caught Exception while serializing a DirectiveMessage:" + e);
      } finally {
        ClusterContextTable.exitContext();
      }
    }
  }

  /** when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the Cluster mechanism
   * @see ClusterContextTable
   **/
  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    super.readExternal(in);     // Message

    allMessagesAcknowledged = in.readBoolean();

    // find the cluster
    ClusterIdentifier cid = getDestination();
    ClusterContext context = ClusterContextTable.findContext(cid);
    if (context == null) {
      System.err.println("Directive Message read in "+cid+" before Context is known.");
    }
    
    try {
      ClusterContextTable.enterContext(context, getSource(), getDestination());
      directives = new Directive[in.readInt()];
      for (int i = 0; i < directives.length; i++) {
        directives[i] = (Directive) in.readObject();
      }
    } finally {
      ClusterContextTable.exitContext();
    }
  }

  /** Wrapper for a Directive so that we can propagate ChangeReports with
   * the actual directive in-band.
   **/
  public static final class DirectiveWithChangeReports implements Directive {
    private final Directive real;
    private final Collection changes;
    public DirectiveWithChangeReports(Directive d, Collection cc) {
      real = d;
      changes=cc;
    }
    public Directive getDirective() { return real; }
    public Collection getChangeReports() { return changes; }

    public Plan getPlan() { return real.getPlan(); }
    public ClusterIdentifier getSource() { return real.getSource(); }
    public ClusterIdentifier getDestination() { return real.getDestination(); }
    public String toString() {return real.toString(); }
  }

}
