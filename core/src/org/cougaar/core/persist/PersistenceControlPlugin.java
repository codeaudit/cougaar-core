/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.persist;

import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OperatingModeImpl;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PersistenceControlService;
import org.cougaar.util.UnaryPredicate;

public class PersistenceControlPlugin extends ServiceUserPlugin {
  private static class MyOperatingMode
    extends OperatingModeImpl
    implements NotPersistable
  {
    String mediaName;
    String controlName;

    MyOperatingMode(String mediaName, String controlName, OMCRangeList values) {
      super("Persistence." + mediaName + "." + controlName, values);
      this.mediaName = mediaName;
      this.controlName = controlName;
    }

    MyOperatingMode(String controlName, OMCRangeList values) {
      super("Persistence." + controlName, values);
      this.mediaName = null;
      this.controlName = controlName;
    }
  }

  private static UnaryPredicate myOperatingModePredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof MyOperatingMode;
      }
    };

  private static Class[] requiredServices = {
    PersistenceControlService.class
  };

  private boolean createdOperatingModes = false;

  private IncrementalSubscription myOperatingModes;

  private ServiceProvider serviceProvider;

  private PersistenceControlService persistenceControlService;

  public PersistenceControlPlugin() {
    super(requiredServices);
  }

  public void load() {
    super.load();
    Persistence persistence = blackboard.getPersistence();
    serviceProvider = persistence.getServiceProvider();
    getServiceBroker().addService(PersistenceControlService.class,
                                  serviceProvider);
  }

  public void unload() {
    if (serviceProvider != null) {
      getServiceBroker().revokeService(PersistenceControlService.class,
                                       serviceProvider);
    }
    super.unload();
  }

  protected boolean haveServices() {
    if (persistenceControlService != null) return true;
    if (acquireServices()) {
      persistenceControlService = (PersistenceControlService)
        getServiceBroker().getService(this, PersistenceControlService.class, null);
      return true;
    }
    return false;
  }


  public void setupSubscriptions() {
    myOperatingModes = (IncrementalSubscription) blackboard.subscribe(myOperatingModePredicate);
  }

  public void execute() {
    if (haveServices()) {
      if (!createdOperatingModes ) {
        createOperatingModes();
        createdOperatingModes = true;
      } else if (myOperatingModes.hasChanged()) {
        updateOperatingModes(myOperatingModes.getChangedCollection());
      }
    }
  }

  private void createOperatingModes() {
    String[] controlNames = persistenceControlService.getControlNames();
    for (int i = 0; i < controlNames.length; i++) {
      String controlName = controlNames[i];
      OMCRangeList controlValues = persistenceControlService.getControlValues(controlName);
      MyOperatingMode om = new MyOperatingMode(controlName, controlValues);
      blackboard.publishAdd(om);
      if (logger.isDebugEnabled()) logger.debug("Added " + om);
    }
    String[] mediaNames = persistenceControlService.getMediaNames();
    for (int j = 0; j < mediaNames.length; j++) {
      String mediaName = mediaNames[j];
      controlNames = persistenceControlService.getMediaControlNames(mediaName);
      for (int i = 0; i < controlNames.length; i++) {
        String controlName = controlNames[i];
        OMCRangeList controlValues = persistenceControlService.getMediaControlValues(mediaName, controlName);
        MyOperatingMode om = new MyOperatingMode(mediaName, controlName, controlValues);
        blackboard.publishAdd(om);
      if (logger.isDebugEnabled()) logger.debug("Added " + om);
      }
    }
  }

  private void updateOperatingModes(Collection changedOperatingModes) {
    for (Iterator i = changedOperatingModes.iterator(); i.hasNext(); ) {
      MyOperatingMode om = (MyOperatingMode) i.next();
      try {
        if (om.mediaName == null) {
          persistenceControlService.setControlValue(om.controlName, om.getValue());
        } else {
          persistenceControlService.setMediaControlValue(om.mediaName, om.controlName, om.getValue());
        }
      } catch (RuntimeException re) {
        logger.error("set persistence control failed", re);
      }
    }
  }
}
