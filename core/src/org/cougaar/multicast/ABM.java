/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
package org.cougaar.multicast;

import java.io.Serializable;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.DirectiveImpl;

/**
 * Messaging object that is sent via multicast messaging 
 * from a sensor within a community. Non-persistable so as to 
 * be lightweight. In addition, once an ABM is put in the delivery queue, 
 * it is publishRemoved from the local Blackboard. Note that all Subscribers
 * will have an opportunity to see the ABM on their Added list before it 
 * is removed. <br>
 * Made up of a <code>ContextWrapper</code>, which wraps the
 * real content with some context, and a <code>MessageAddress</code>. 
 * This <code>MessageAddress</code> could be an actual destination, like a
 * <code>ClusterIdentifier</code>. But for multicast it will be a 
 * <code>ABMAddress</code>. This type indicates the kind of message being 
 * sent. Recipients must register an interest in messages of this type
 * within their community, via the Yellow Pages.
 * 
 * @see ContextWrapper
 * @see ABMFactory
 * @see ABMAddress
 */
public class ABM extends DirectiveImpl implements Directive, UniqueObject, Publishable, Serializable
{
  private UID uid = null;
  private MessageAddress dest;
  private ContextWrapper content;
  
  /**
   * Creates a new <code>ABM</code> instance. Note that ABMs should not be 
   * modified once created, so this is the only method that sets
   * any of the member values.
   *
   * @param uid an <code>UID</code> to identify it
   * @param bw a <code>ContextWrapper</code>, the content to be sent
   * @param dest a <code>MessageAddress</code> destination, often a <code>ABMAddress</code>
   * @param source a <code>ClusterIdentifier</code> to ID the sending Agent
   */
  public ABM (UID uid, ContextWrapper bw, MessageAddress dest, ClusterIdentifier source) {
    
    // set slots
    this.uid = uid;    
    this.content = bw;
    
    if (dest != null) {
      this.dest = dest;
      if(dest instanceof ClusterIdentifier)
	super.setDestination((ClusterIdentifier)dest);
    } 
    else
      System.err.println("Error creating ABM, destination is null.");
    
    if (source != null) {
      super.setSource(source);
    } 
    else
      System.err.println("Error creating ABM, source is null.");
  }
  
  /**
   * Gets Object's Unique Identifier.
   *
   * @return an <code>UID</code> value
   */
  public UID getUID() {
    return uid;
  }
  
  /**
   * No-op, to forbid modifying the UID.
   *
   * @param uid an <code>UID</code> value
   */
  public void setUID(UID uid) {}
  
  /**
   * Gets Content's time from its <code>ContextWrapper</code>.
   *
   * @return a <code>long</code> value
   */
  public long getTime() {
    return content.getTime();
  }

  /**
   * Allows access to <code>ABM</code>'s destination. This is distinct
   * from the <code>getDestination()</code> inherited from DirectiveImpl.
   * This slot may contain a ABMAddress or other non-deliverable address,
   * while the inherited slot should always contain a deliverable address.
   * 
   * @return dest, a <code>MessageAddress</code>
   **/
  public MessageAddress getDest() {
    return dest;
  }

  /**
   * Gets name of plugin who published <code>ABM</code>,
   * returned from its <code>ContextWrapper</code>.
   *
   * @return a <code>String</code> value
   */
  public String getPublisher() {
    return content.getPublisher();
  }
  
  /**
   * Allows access to ContextWrapper in ABM Object. This is the content
   * of the message to be delivered.
   *
   * @return a <code>ContextWrapper</code> value
   */
  public ContextWrapper getContent() {
    return content;
  }
  
  /**
   * ABMs are not persisted.
   *
   * @return a <code>boolean</code>, false
   */
  public boolean isPersistable() {return false;}
  
  /**
   * Over-ride setDestination to forbid modifications
   *
   * @param dest a <code>ClusterIdentifier</code> value
   */
  public void setDestination(ClusterIdentifier dest){}
    
  /**
   * Over-ride setSource to prevent modifications
   *
   * @param asource a <code>ClusterIdentifier</code> value
   */
  public void setSource(ClusterIdentifier asource){}

  /**
   * Allows visibility into ABM Object. 
   *
   * @return a <code>String</code> value
   **/
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<ABM " + this.uid.toString());
    buf.append(" Source: " + this.source.toString()); 
    buf.append(" Destination: " + this.dest.toString()); 
    buf.append(" Content: " + this.content.toString());
    buf.append(">");
    return buf.toString();
  }
}
