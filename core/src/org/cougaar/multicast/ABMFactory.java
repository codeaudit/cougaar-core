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
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.LoggingService;

/**
 * Creates ABM Objects and ContextWrappers, used in
 * <code>ABM</code> messenging between sensors in a
 * community
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
      System.err.println("Error obtaining LoggingService at " + this.getClass());    
  }
  
  /**
   * Creates a new ABM Object, given a wrapped object and 
   * destination.
   *
   * @param bw a <code>ContextWrapper</code> value
   * @param ma a <code>MessageAddress</code> value
   * @return an <code>ABM</code> value
   */
  public ABM newABM(ContextWrapper bw, MessageAddress ma) {
    if (bw!=null && ma!=null) 
      return new ABM(getNextUID(), bw, ma, selfClusterId);
    else {
      //System.out.println("Error creating ABM, null content or destination");
      if(logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null content or destination, returning.");
      }
      return null; 
    }
  }
  
  /**
   * Creates a new ABM Object, given an existing ABM Object and 
   * destination. 
   *
   * @param anABM an <code>ABM</code> value
   * @param ma a <code>MessageAddress</code> value
   * @return an <code>ABM</code> value
   *
   * @see ABMFactory
   */
  public ABM newABM(ABM anABM, MessageAddress ma){ 
    if (anABM!=null && ma!=null) 
      return this.newABM(anABM.getContent(), ma);
    else {
      //System.out.println("Error creating ABM, null ABM or destination");
      if(logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null ABM or destination, returning.");
      }
      return null; 
    }  
  }
  
  /**
   * Creates a new ABM Object, given a Serializable, publisher, 
   * and destination. 
   *
   * @param content a <code>Serializable</code> value
   * @param publisher a <code>String</code> value
   * @param ma a <code>MessageAddress</code> value
   * @return an <code>ABM</code> value
   */
  public ABM newABM(Serializable content, String publisher, MessageAddress ma) {
    if (content!=null && ma!=null) 
      return new ABM(getNextUID(), this.newContextWrapper(content, publisher), ma, selfClusterId);
    else {
      //System.out.println("Error creating ABM, null content or destination");
      if(logger.isErrorEnabled()) {
	logger.error("Error creating ABM, null content or destination, returning.");
      }
      return null;
    }  
  }
  
  /**
   * Creates new ContextWrapper Object, given a Serializable content and publisher.
   *
   * @param content a <code>Serializable</code> value
   * @param publisher a <code>String</code> value
   * @return a <code>ContextWrapper</code> value
   *
   * @see ContextWrapper newABM(ContextWrapper, MessageAddress);
   */
  public ContextWrapper newContextWrapper(Serializable content, String publisher) {
    // Must add in the current time, current Agent, etc
    if (content!=null) 
      return new ContextWrapper(getNextUID(), content, publisher, cspi.currentTimeMillis(), selfClusterId);
    else {
      //System.out.println("Error creating new ContextWrapper: null content");    
      if(logger.isErrorEnabled()) {
	logger.error("Error creating ContextWrapper - null content, returning.");
      }
      return null;
    } 
  }
  

  /**
   * @return a new <code>UID</code>
   */
  public UID getNextUID() {
    return myUIDServer.nextUID();
  }
  
  /**
   * Allow access to YellowPages implementation for 
   * multicast messenging. 
   *
   * @return myYP a <code>YP</code>
   **/
  public YP getYP() {
    return myYP;
  }

} // end of ABMFactory.java
