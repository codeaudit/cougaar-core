/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.ReadOnlySet;
import org.cougaar.core.component.BindingSite;

/**
 * This Plugin serves as an ConditionServiceProvider. It
 * subscribes to {@link Condition}s using a subscription allowing
 * immediate access to individual Conditions by name. Provides
 * ConditionService to components needing name-based access to
 * Conditions.
 **/
public class ConditionServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
  /**
   * A {@link KeyedSet} for sets of Conditions keyed by name.
   **/
  private static class ConditionSet extends KeyedSet {
    public ConditionSet() {
      super();
      makeSynchronized();
    }

    protected Object getKey(Object o) {
      Condition sm = (Condition) o;
      return sm.getName();
    }

    public Condition getCondition(String name) {
      return (Condition) inner.get(name);
    }
  }

  /**
   * An implementation of ConditionService in terms of the
   * information in the ConditionSet.
   **/
  private class ConditionServiceImpl implements ConditionService {
    public Condition getConditionByName(String knobName) {
      return smSet.getCondition(knobName);
    }

    public Set getAllConditionNames() {
      return new ReadOnlySet(smSet.keySet());
    }

    public void addListener(Listener l) {
      listeners.add(l);
    }

    public void removeListener(Listener l) {
      listeners.remove(l);
    }
  }

  private static UnaryPredicate ConditionPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof Condition;
    }
  };

  private ConditionSet smSet = new ConditionSet();
  private IncrementalSubscription conditions;

  private List listeners = new ArrayList(2);

  private LoggingService logger;

  /**
   * Override base class method to register our service with the
   * service broker.
   **/
  public void load() {
    super.load();
    logger = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    getServiceBroker().addService(ConditionService.class, this);
  }

  public void unload() {
    getServiceBroker().releaseService(this, LoggingService.class, logger);
    getServiceBroker().revokeService(ConditionService.class, this);
    super.unload();
  }

  /**
   * Standard setupSubscriptions subscribes to all Conditions.
   **/
  public void setupSubscriptions() {
    conditions = (IncrementalSubscription)
      getBlackboardService().subscribe(ConditionPredicate, smSet);
  }

  /**
   * Standard execute method does nothing. Our subscription
   * automatically maintains the information of interest in smSet
   * where it is referenced directly by the service API.
   **/
  public void execute() {
    if (conditions.hasChanged()) fireListeners();
  }

  private void fireListeners() {
    BlackboardService bb = getBlackboardService();
    if (logger.isDebugEnabled()) {
      for (Iterator i = conditions.getAddedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition added: " + i.next());
      }
      for (Iterator i = conditions.getChangedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition changed: " + i.next());
      }
      for (Iterator i = conditions.getRemovedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition removed: " + i.next());
      }
      logger.debug("ConditionServiceProvider.fireListeners:");
    }
    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (logger.isDebugEnabled()) logger.debug("    " + o);
      bb.publishChange(o);
    }
  }

  /**
   * Gets (creates) an implementation of the ConditionService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == ConditionService.class) {
      return new ConditionServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  /**
   * Releases an implementation of the ConditionService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != ConditionService.class
        || svc.getClass() != ConditionServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
