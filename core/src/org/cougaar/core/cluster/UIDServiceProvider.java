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



  
  
