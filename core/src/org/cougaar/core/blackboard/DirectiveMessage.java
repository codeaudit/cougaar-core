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

package org.cougaar.core.blackboard;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.mts.UnresolvableReferenceException;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.util.StringUtility;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.util.log.*;

import java.util.Collection;
import java.io.*;

/**
 * A org.cougaar.core.blackboard.DirectiveMessage  provides a basic implementation of 
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
   */
  public DirectiveMessage() {
    super();
  }
    
  /** constructor that takes a directive
   * @param aDirective
   */
  public DirectiveMessage(Directive[] someDirectives) {
    directives = someDirectives;
  }
    
  /** constructor that takes source, destination and a directive
   * @param source
   * @param destination
   * @param aDirective
   */
  public DirectiveMessage(MessageAddress source, MessageAddress destination,
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

  private void withContext(MessageAddress ma, Runnable thunk) 
    throws IOException 
  {
    try {
      ClusterContextTable.withMessageContext(ma, getSource(), getDestination(), thunk);
    } catch (RuntimeException re) {
      Throwable t = re.getCause();
      if (t == null) {
        throw re;
      } else if (t instanceof IOException) {
        throw (IOException) t;
      } else {
        Logging.getLogger(DirectiveMessage.class).error("Serialization of "+this+" caught exception", t);
        throw new IOException("Serialization exception: "+t);
      }
    }
  }
  

  /**
   **/
  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    withContext( getSource(),
                 new Runnable() {
                     public void run() {
                       try {
                         stream.writeInt(directives.length);
                         for (int i = 0; i < directives.length; i++) {
                           stream.writeObject(directives[i]);
                         }
                       } catch (Exception e) {
                         throw new RuntimeException("Thunk", e);
                       }}});
  }

  /** when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the Cluster mechanism
   * @see ClusterContextTable
   **/
  private void readObject(final ObjectInputStream stream) 
    throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();

    withContext(getDestination(),
                 new Runnable() {
                     public void run() {
                       try {
                         directives = new Directive[stream.readInt()];
                         for (int i = 0; i < directives.length; i++) {
                           directives[i] = (Directive) stream.readObject();
                         }
                       } catch (Exception e) {
                         throw new RuntimeException("Thunk", e);
                       }}});
  }

  // Externalizable support
  /*
  **/
  public void writeExternal(final ObjectOutput out) throws IOException {
    super.writeExternal(out);   // Message

    out.writeBoolean(allMessagesAcknowledged);

    withContext( getSource(),
                 new Runnable() {
                     public void run() {
                       try {
                         out.writeInt(directives.length);
                         for (int i = 0; i < directives.length; i++) {
                           out.writeObject(directives[i]);
                         }
                       } catch (Exception e) {
                         throw new RuntimeException("Thunk", e);
                       }}});

  }

  /** when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the Cluster mechanism
   * @see ClusterContextTable
   **/
  public void readExternal(final ObjectInput in) 
    throws IOException, ClassNotFoundException
  {
    super.readExternal(in);     // Message

    allMessagesAcknowledged = in.readBoolean();

    withContext(getDestination(),
                 new Runnable() {
                     public void run() {
                       try {
                         directives = new Directive[in.readInt()];
                         for (int i = 0; i < directives.length; i++) {
                           directives[i] = (Directive) in.readObject();
                         }
                       } catch (Exception e) {
                         throw new RuntimeException("Thunk", e);
                       }}});
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
    public MessageAddress getSource() { return real.getSource(); }
    public MessageAddress getDestination() { return real.getDestination(); }
    public String toString() {return real.toString(); }
  }

}
