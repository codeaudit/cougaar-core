/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.core.component.Service;
import org.cougaar.core.plugin.ScheduleablePlugIn;
import java.util.*;

/** A SharedThreadingService is an API which may be supplied by a 
 * ServiceProvider registered in a Services object that provides shared
 * thread scheduling. This is used for SharedThreading Plugins, but 
 * could be extended in the future.
 **/
public interface SharedThreadingService extends Service {
 
  /** called by the plugin (sharedthreading class right now) to register
   * the plugin with the scheduler
   **/
  void registerPlugIn(ScheduleablePlugIn plugin);
  
}



