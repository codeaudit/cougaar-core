/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.core.plugin.PlugInDelegate;

/**
 * A TriggerAction performs an action (presumed to be publishing changes
 * to the LDM) when 'fired'.
 */

public interface TriggerAction extends java.io.Serializable {

  /**
   * @param objects  The objects to perform the action against.
   * @param pid  The plugin delegate to allow things like publish add/remove/change and getClusterObjectFactory
   */
  void Perform(Object[] objects, PlugInDelegate pid);

}
