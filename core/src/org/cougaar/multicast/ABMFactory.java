/*
 * <copyright>
 *  Copyright 2000-2002 BBNT Solutions, LLC
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

import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesPlugin;
import org.cougaar.core.agent.ClusterImpl;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.util.UID;

/**
 * Creates <code>ABM</code> Objects and <code>ContextWrapper</code>s, used in
 * ABM messaging from sensors within a community. This Factory may
 * be extended by particular management domains to allow easy creation
 * of particular messages to be sent with the ABM system.
 *
 **/
public class ABMFactory
    implements Factory {

  protected ClusterIdentifier selfClusterId;
  protected UIDServer myUIDServer;
  protected ClusterServesPlugin cspi;
  protected YP myYP;
  
  private LoggingService logger;

  /**
   * Constructor for use by domain specific Factories
   * extending this class
   */
  public ABMFactory() { }
  
  public ABMFactory(LDMServesPlugin ldm) {
    cspi = (ClusterServesPlugin)ldm;
    selfClusterId = cspi.getClusterIdentifier();
    myUIDServer = ((ClusterContext)ldm).getUIDServer();
    myYP = new YPImpl();
    
    logger = (LoggingService)((ClusterImpl)cspi).getServiceBroker().getService(this, LoggingService.class, null);
    if(logger == null)
      System.err.println("Error obtaining LoggingService at " + this.getClass() + " in agent " + selfClusterId);
  }
  
  /**
   * Creates a new ABM Object, given a wrapped object and 
   * destination. The destination will often be a <code>MessageType</code>,
   * indicating the kind of message contained in the <code>ContextWrapper</code>, 
   * and therefore the the set of recipients for this <code>ABM</code>. But 
   * it may in fact also be a generic address.<br>
   *
   * @param bw a <code>ContextWrapper</code> wrapped message to send
   * @param ma a <code>MessageAddress</code> destination for the message
   * @return an <code>ABM</code> to send, null on error
   */
  public ABM newABM(ContextWrapper bw, MessageAddress ma) {
    if (bw!=null && ma!=null) 
      return new ABM(getNextUID(), bw, ma, selfClusterId);
    else {
      if (logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null content or destination, returning null.");
      }
      return null; 
    }
  }
  
  /**
   * Creates a new ABM Object, given an existing ABM Object and 
   * destination. Used by the <code>ABMTransportLP</code> to expand
   * a message addressed to a <code>MessageType</code> into multiple
   * messages for each subscribing Agent.
   *
   * @param anABM an <code>ABM</code> to re-address
   * @param ma a <code>MessageAddress</code> destination, usually a <code>ClusterIdentifier</code>
   * @return an <code>ABM</code> destined to the new address, null on error
   */
  public ABM newABM(ABM anABM, MessageAddress ma){ 
    if (anABM!=null && ma!=null) 
      return this.newABM(anABM.getContent(), ma);
    else {
      if (logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null ABM or destination, returning null.");
      }
      return null; 
    }  
  }
  
  /**
   * Creates a new ABM Object, given a <code>Serializable</code> message, publisher, 
   * and destination. This implicitly creates a <code>ContextWrapper</code> 
   * to hold the message to be sent. Sensors that know that their reports should
   * be sent to a particular destination-type will likely use this constructor.
   *
   * @param content a <code>Serializable</code> message to be sent
   * @param publisher a <code>String</code> identifier for the publishing Plugin
   * @param ma a <code>MessageAddress</code> destination for the ABM
   * @return an <code>ABM</code> to send, null on error
   */
  public ABM newABM(Serializable content, String publisher, MessageAddress ma) {
    if (content!=null && ma!=null) 
      return new ABM(getNextUID(), this.newContextWrapper(content, publisher), ma, selfClusterId);
    else {
      if(logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null content or destination, returning null.");
      }
      return null;
    }  
  }
  
  /**
   * Creates new ContextWrapper Object, given a Serializable content and publisher. 
   * These objects ensure that the messages sent using ABMs have some context
   * about who sent them. 
   *
   * @param content a <code>Serializable</code> message to wrap in context
   * @param publisher a <code>String</code> ID for the Plugin publishing it
   * @return a <code>ContextWrapper</code> containing the message, null on error
   */
  public ContextWrapper newContextWrapper(Serializable content, String publisher) {
    // Must add in the current time, current Agent, etc
    if (content!=null) 
      return new ContextWrapper(getNextUID(), content, publisher, cspi.currentTimeMillis(), selfClusterId);
    else {
      if (logger.isErrorEnabled()) {
	logger.error("Error creating ContextWrapper - null content, returning null.");
      }
      return null;
    } 
  }
  

  /**
   * Primarily for Factory use only.
   * @return a new <code>UID</code>
   */
  public UID getNextUID() {
    return myUIDServer.nextUID();
  }
  
  /**
   * Allow access to YellowPages implementation for 
   * multicast messenging. This method is temporary until true YellowPages support is implemented.
   *
   * @return myYP a <code>YP</code>
   **/
  public YP getYP() {
    return myYP;
  }

} // end of ABMFactory.java
