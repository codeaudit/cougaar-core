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
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Wraps sensor's content -i.e. a health report, alert, or other, along 
 * with personal information of the publishing sensor. This object
 * provides context for the message to ensure proper handling when it 
 * is received on remote Agents.
 * Used in <code>ABM</code> messaging from sensors in a community.  
 * 
 * @see ABMFactory
 */
public class ContextWrapper implements Serializable, Publishable, UniqueObject
{
  // misc info about context
  // must be private so no one can mess with these
  private Serializable data;
  private long time;
  private ClusterIdentifier source;
  private UID uid;
  private String publisher;
  
  /**
   * Creates a new <code>ContextWrapper</code> instance with 
   * useful tracking information consisting of a UniqueIdentifier, 
   * its source agent, which plugin published it, and its creation time. <br>
   * Note that this is the only means to set the values in this object,
   * so that the values do not change.
   *
   * @param uid an <code>UID</code> identifier
   * @param aBlob a <code>Serializable</code> content
   * @param publisher a <code>String</code> id of the Plugin that published it
   * @param time a <code>long</code> when it was created
   * @param source a <code>ClusterIdentifier</code> value
   */
  public ContextWrapper(UID uid, Serializable aBlob, String publisher, long time, ClusterIdentifier source) {
    data = aBlob;
    this.uid = uid;
    this.publisher = publisher;
    this.time = time;
    this.source = source;
  } 

  /**
   * Accessor to the message's content.
   *
   * @return a <code>Serializable</code> content
   */
  public Serializable getContent(){
    return data;
  }

  /**
   * Accessor to creation time of the message
   *
   * @return a <code>long</code> time in milliseconds
   */
  public long getTime(){
    return time;
  }

  /**
   * Accessor to source agent.
   *
   * @return a <code>ClusterIdentifier</code> Agent name
   */
  public ClusterIdentifier getSource(){
    return source;
  }


  /**
   * Accessor to plugin that published ABM.
   *
   * @return a <code>String</code> Plugin name or label
   */
  public String getPublisher() {
    return publisher;
  }

  /**
   * Accessor to UniqueIdentifier for the object.
   *
   * @return an <code>UID</code> value
   */
  public UID getUID() {
    return uid;
  }
  
  /**
   * Does nothing to ensure a static UID.
   *
   * @param uid an <code>UID</code> value
   */
  public void setUID(UID uid) {}
  
  /**
   * ContextWrappers are persistable by default.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isPersistable() {return true;}
  
  /**
   * Provides visibility to ContextWrapper's information. 
   *
   * @return a <code>String</code>
   **/
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<ContextWrapper " + this.uid.toString());
    buf.append(" Source: " + this.source.toString());
    buf.append(" Publisher: " + this.publisher.toString());
    buf.append(" Time: " + this.time);
    buf.append(" Content: " + this.data.toString());
    buf.append(">");
    return buf.toString();
  }
}
