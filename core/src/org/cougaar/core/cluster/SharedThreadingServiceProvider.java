/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.cluster;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

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
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return getSharedPlugInManager();
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
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

  public void suspend() {
    synchronized (this) {
      if (_sharedPlugInManager != null) {
        _sharedPlugInManager.suspend();
      }
    }
  }

  public void resume() {
    synchronized (this) {
      if (_sharedPlugInManager != null) {
        _sharedPlugInManager.resume();
      }
    }
  }
}


  
  
