
/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.core.plugin.PlugInDelegate;

/**
 * A TriggerMonitor determines a particular set of objects on which to
 * search for a particular condition.
 */

public interface TriggerMonitor extends java.io.Serializable {
  
  /** @return Object[]  The objects to be monitored. **/
  Object[] getAssociatedObjects();

  /** 
    * @param pid  PluginDelegate that allows access to plugin level methods
    * @return boolean  Is the monitor ready to run yet? 
    **/
  boolean ReadyToRun(PlugInDelegate pid);
  
  /** Preserve the fact that this trigger has run. 
    * @param pid  PluginDelegate that allows access to plugin level methods
    **/
  void IndicateRan(PlugInDelegate pid);

}
