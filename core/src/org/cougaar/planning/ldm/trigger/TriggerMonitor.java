
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


package org.cougaar.planning.ldm.trigger;

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
