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

package org.cougaar.core.domain;

import java.util.*;

import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;

import org.cougaar.core.blackboard.ChangeEnvelopeTuple;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.XPlanServesBlackboard;

import org.cougaar.core.component.BindingSite;

import org.cougaar.core.persist.PersistenceNotEnabledException;

import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.plan.Directive;

public abstract class DomainAdapter 
  extends GenericStateModelAdapter implements DomainBase {

  private final List myEnvelopeLPs = new ArrayList();
  private final List myMessageLPs = new ArrayList();
  private final List myRestartLPs = new ArrayList();
  
  private Factory myFactory;

  private DomainBindingSite myBindingSite;

  private XPlanServesBlackboard myXPlan;

  private LoggingService myLoggingService = null;

  abstract public String getDomainName();


  public void setBindingSite(BindingSite bindingSite) {
    if (bindingSite instanceof DomainBindingSite) {
      myBindingSite = (DomainBindingSite) bindingSite;
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bindingSite);
    }

    myLoggingService = 
      (LoggingService) bindingSite.getServiceBroker().getService(this, LoggingService.class, null);
  }

  public BindingSite getBindingSite() {
    return myBindingSite;
  }

  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  public Factory getFactory() {
    return myFactory;
  }

  public XPlanServesBlackboard getXPlan() {
    return myXPlan;
  }

  public void load() {
    super.load();

    loadFactory();
    loadXPlan();
    loadLPs();
  }

  public void invokeMessageLogicProviders(DirectiveMessage message) {
    Directive [] directives = message.getDirectives();
    for (int index = 0; index < directives.length; index ++) {
      Directive directive = directives[index];
      Collection changeReports = null;
      if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
        DirectiveMessage.DirectiveWithChangeReports dd = 
          (DirectiveMessage.DirectiveWithChangeReports) directive;
        changeReports = dd.getChangeReports();
        directive = dd.getDirective();
      }

      synchronized (myMessageLPs) {
        for (int lpIndex = 0; lpIndex < myMessageLPs.size(); lpIndex++) {
          ((MessageLogicProvider) myMessageLPs.get(lpIndex)).execute(directive, changeReports);
        }
      }
    }
  }
    
  public void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    Collection changeReports = null;
    if (tuple instanceof ChangeEnvelopeTuple) {
      changeReports = ((ChangeEnvelopeTuple) tuple).getChangeReports();
    }

    synchronized (myEnvelopeLPs) {
      for (int lpIndex = 0; lpIndex < myEnvelopeLPs.size(); lpIndex++) {
        EnvelopeLogicProvider lp = (EnvelopeLogicProvider) myEnvelopeLPs.get(lpIndex);
	if (isPersistenceEnvelope && !(lp instanceof LogicProviderNeedingPersistenceEnvelopes)) {
	  continue;	// This lp does not want contents of PersistenceEnvelopes
	}
        try {
          lp.execute(tuple, changeReports);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }


  public void invokeRestartLogicProviders(ClusterIdentifier cid) {
    synchronized (myRestartLPs) {
      for (int index = 0;  index < myRestartLPs.size(); index++) {
        try {
          ((RestartLogicProvider) myRestartLPs.get(index)).restart(cid);
        }
        catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    }
  }

  
  // default protection
  protected void addLogicProvider(LogicProvider lp) {
    if (lp instanceof MessageLogicProvider) {
      myMessageLPs.add(lp);
    }
    if (lp instanceof EnvelopeLogicProvider) {
      myEnvelopeLPs.add(lp);
    }
    if (lp instanceof RestartLogicProvider) {
      myRestartLPs.add(lp);
    }
    
    lp.init();
  }

  protected final List getEnvelopeLPs() {
    return myEnvelopeLPs;
  }

  private final List getMessageLPs() {
    return myMessageLPs;
  }

  private final List getRestartLPs() {
    return myRestartLPs;
  }

  abstract protected void loadFactory();

  abstract protected void loadXPlan();
  
  abstract protected void loadLPs();

  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  protected void setFactory(Factory factory) {
    if ((myFactory != null) && myLoggingService.isDebugEnabled()) {
      // Should we even allow this?
      myLoggingService.debug("DomainAdapter: resetting Factory");
    }

    myFactory = factory;
  }

  protected void setXPlan(XPlanServesBlackboard xPlan) {
    if ((myXPlan != null) && myLoggingService.isDebugEnabled()) {
      // Should we even allow this?
      myLoggingService.debug("DomainAdapter: resetting XPlan");
    }
    
    myXPlan = xPlan;
  }

}








