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

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.Services;

import java.util.*;

/** A SharedThreadingServiceProvider is a provider class that PluginManager
 * calls when a client(plugin at this point) requests SharedThreading.
 **/
public class SharedThreadingServiceProvider implements ServiceProvider {

  private ClusterIdentifier clusterid;
  private SharedPlugInManager _sharedPlugInManager = null;
  
  public SharedThreadingServiceProvider(ClusterIdentifier cid) {
    super();
    this.clusterid = cid;
  }
  
  public Object getService(Services services, Object requestor, Class serviceClass) {
    return getSharedPlugInManager();
  }

  public void releaseService(Services services, Object requestor, Class serviceClass, Object service)  {
    // TODO:  put in an unregisterPlugin in the SharedPlugInManager
    // getSharedPlugInManager().unregisterPlugin((ScheduleablePlugIn)requester);
  }

  // for now start one manager per cluster  
  private SharedPlugInManager getSharedPlugInManager() {
    synchronized (this) {
      if (_sharedPlugInManager == null) {
        _sharedPlugInManager = new SharedPlugInManager(clusterid);
      }
      return _sharedPlugInManager;
    }
  }

}


  
  
