/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

import java.util.*;

/** A PrototypeRegistryServiceProvider is a provider class for prototype registry functions. **/
public class PrototypeRegistryServiceProvider implements ServiceProvider {

  private PrototypeRegistry _protregistry = null;
  
  public PrototypeRegistryServiceProvider() {
    super();
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return getPrototypeRegistry();
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    // more??
    _protregistry = null;
  }

  // should only have one per cluster  
  private PrototypeRegistry getPrototypeRegistry() {
    synchronized (this) {
      if (_protregistry == null) {
        _protregistry = new PrototypeRegistry();
      }
      return _protregistry;
    }
  }

}


  
  
