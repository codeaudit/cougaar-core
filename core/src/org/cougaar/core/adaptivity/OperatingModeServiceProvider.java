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
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Thunk;
import org.cougaar.util.Collectors;

/**
 * This Plugin serves as an OperatingModeServiceProvider. It
 * subscribes to {@link OperatingMode}s using a subscription allowing
 * immediate access to individual OperatingModes by name. Provides
 * OperatingModeService to components needing name-based access to
 * OperatingModes.
 **/
public class OperatingModeServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
  /**
   * A {@link KeyedSet} for sets of OperatingModes keyed by name.
   **/
  private static class OperatingModeSet extends KeyedSet {
    public OperatingModeSet() {
      super();
      makeSynchronized();
    }

    protected Object getKey(Object o) {
      OperatingMode om = (OperatingMode) o;
      return om.getName();
    }

    public OperatingMode getOperatingMode(String name) {
      return (OperatingMode) inner.get(name);
    }
  }

  /**
   * An implementation of OperatingModeService in terms of the
   * information in the OperatingModeSet.
   **/
  private class OperatingModeServiceImpl implements OperatingModeService {
    public void addListener(OperatingModeService.Listener l) {
      synchronized (listeners) {
        listeners.add(l);
      }
    }
    public void removeListener(OperatingModeService.Listener l) {
      synchronized (listeners) {
        listeners.remove(l);
      }
    }
    public OperatingMode getOperatingModeByName(String knobName) {
      synchronized (omSet) {
        return omSet.getOperatingMode(knobName);
      }
    }
    public Set getAllOperatingModeNames() {
      synchronized (omSet) {
        return new HashSet(omSet.keySet());
      }
    }
  }

  private static UnaryPredicate operatingModePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof OperatingMode;
    }
  };

  LoggingService logger;
  private OperatingModeSet omSet = new OperatingModeSet();
  private IncrementalSubscription operatingModes;
  private List listeners = new ArrayList(2);
  private Thunk listenerAddThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantAdds()) blackboard.publishChange(o);
      }
    };
  private Thunk listenerChangeThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantChanges()) blackboard.publishChange(o);
      }
    };
  private Thunk listenerRemoveThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantRemoves()) blackboard.publishChange(o);
      }
    };

  /**
   * Override base class method to register our service with the
   * service broker.
   **/
  public void load() {
    super.load();
    getServiceBroker().addService(OperatingModeService.class, this);
    logger = (LoggingService)
      getServiceBroker().getService(this, LoggingService.class, null);
  }

  /**
   * Override base class method to unregister our service with the
   * service broker.
   **/
  public void unload() {
    getServiceBroker().revokeService(OperatingModeService.class, this);
    super.unload();
  }

  /**
   * Standard setupSubscriptions subscribes to all OperatingModes.
   **/
  public void setupSubscriptions() {
    synchronized (omSet) {
      operatingModes = (IncrementalSubscription)
        getBlackboardService().subscribe(operatingModePredicate);
      omSet.addAll(operatingModes);
    }
  }

  /**
   * Standard execute method does nothing. Our subscription
   * automatically maintains the information of interest in omSet
   * where it is referenced directly by the service API.
   **/
  public void execute() {
    synchronized (omSet) {
      if (operatingModes.hasChanged()) {
        Collection c;
        c = operatingModes.getAddedCollection();
        if (c.size() > 0) {
          omSet.addAll(c);
          if (logger.isDebugEnabled()) logger.debug("OM Added");
          Collectors.apply(listenerAddThunk, listeners);
        }
        c = operatingModes.getChangedCollection();
        if (c.size() > 0) {
          if (logger.isDebugEnabled()) logger.debug("OM Changed");
          Collectors.apply(listenerChangeThunk, listeners);
        }
        c = operatingModes.getRemovedCollection();
        if (c.size() > 0) {
          omSet.removeAll(c);
          if (logger.isDebugEnabled()) logger.debug("OM Removed");
          Collectors.apply(listenerRemoveThunk, listeners);
        }
      }
    }
  }

  /**
   * Gets (creates) an implementation of the OperatingModeService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == OperatingModeService.class) {
      return new OperatingModeServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  /**
   * Releases an implementation of the OperatingModeService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != OperatingModeService.class
        || svc.getClass() != OperatingModeServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
