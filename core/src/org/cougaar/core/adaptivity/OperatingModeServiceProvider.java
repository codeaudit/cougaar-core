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

import org.cougaar.core.component.Service;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.ReadOnlySet;
import org.cougaar.core.component.BindingSite;
import java.util.Set;
import java.util.Collections;

/**
 * This Componentserves as an OperatingModeServiceProvider
 **/
public class OperatingModeServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
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

  private class OperatingModeServiceImpl implements OperatingModeService {
    public OperatingMode getOperatingModeByName(String knobName) {
      return omSet.getOperatingMode(knobName);
    }
    public Set getAllOperatingModeNames() {
      return new ReadOnlySet(omSet.keySet());
    }
  }

  private static UnaryPredicate operatingModePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof OperatingMode;
    }
  };

  private OperatingModeSet omSet = new OperatingModeSet();
  private IncrementalSubscription operatingModes;

  private BindingSite bindingSite;

  public void setBindingSite(BindingSite bs) {
    bindingSite = bs;
  }

  public void setParameter(Object param) {
  }

  public void load() {
    super.load();
    bindingSite.getServiceBroker().addService(OperatingModeService.class, this);
  }

  public void unload() {
    bindingSite.getServiceBroker().revokeService(OperatingModeService.class, this);
    super.unload();
  }

  public void setupSubscriptions() {
    operatingModes = (IncrementalSubscription)
      getBlackboardService().subscribe(operatingModePredicate, omSet);
  }

  public void execute() {
    // Our subscription has been changed, but there is nothing more to do.
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == OperatingModeService.class) {
      return new OperatingModeServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != OperatingModeService.class
        || svc.getClass() != OperatingModeServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
