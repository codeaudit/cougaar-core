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
import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.persist.PersistenceState;
import org.cougaar.core.cluster.persist.StatePersistable;

/** A UIDServiceProvider is a provider class for the UID services. **/
public class UIDServiceProvider implements ServiceProvider {
  private UIDServiceImpl theServer;
  public UIDServiceProvider(UIDServiceImpl theServer) {
    this.theServer = theServer;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (UIDService.class.isAssignableFrom(serviceClass)) {
      return new UIDServiceProxy();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

  private final class UIDServiceProxy implements UIDService {
    public ClusterIdentifier getClusterIdentifier() {
      return theServer.getClusterIdentifier();
    }
    public UID nextUID() {
      return theServer.nextUID();
    }
    public UID registerUniqueObject(UniqueObject o) {
      return theServer.registerUniqueObject(o);
    }
     // persistence backwards compat
    public PersistenceState getPersistenceState() {
      return theServer.getPersistenceState();
    }

    /** called during persistence rehydration to reset the state **/
    public void setPersistenceState(PersistenceState state) {
      theServer.setPersistenceState(state);
    }
  }

}



  
  
