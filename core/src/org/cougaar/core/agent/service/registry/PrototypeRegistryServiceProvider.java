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
package org.cougaar.core.agent.service.registry;

import org.cougaar.core.domain.*;

import org.cougaar.core.service.*;

import org.cougaar.core.agent.*;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;

/** A PrototypeRegistryServiceProvider is a provider class for prototype registry functions. **/
public class PrototypeRegistryServiceProvider implements ServiceProvider {

  private PrototypeRegistry protregistry;
  
  public PrototypeRegistryServiceProvider(PrototypeRegistry theRegistry) {
    this.protregistry = theRegistry;
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (PrototypeRegistryService.class.isAssignableFrom(serviceClass)) {
      return new PrototypeRegistryServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

  private final class PrototypeRegistryServiceImpl implements PrototypeRegistryService {
    public void addPrototypeProvider(PrototypeProvider prov) {
      protregistry.addPrototypeProvider(prov);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      protregistry.addPropertyProvider(prov);
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      protregistry.addLatePropertyProvider(lpp);
    }
    public void cachePrototype(String aTypeName, Asset aPrototype) {
      protregistry.cachePrototype(aTypeName, aPrototype);
    }
    public boolean isPrototypeCached(String aTypeName) {
      return protregistry.isPrototypeCached(aTypeName);
    }
    public Asset getPrototype(String aTypeName, Class anAssetClass) {
      return protregistry.getPrototype(aTypeName, anAssetClass);
    }
    public Asset getPrototype(String aTypeName) {
      return protregistry.getPrototype(aTypeName);
    }
    public void fillProperties(Asset anAsset) {
      protregistry.fillProperties(anAsset);
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return protregistry.lateFillPropertyGroup(anAsset, pg, time);
    }
    //metrics service hooks
    public int getPrototypeProviderCount() {
      return protregistry.getPrototypeProviderCount();
    }
    public int getPropertyProviderCount() {
      return protregistry.getPropertyProviderCount();
    }
    public int getCachedPrototypeCount() {
      return protregistry.getCachedPrototypeCount();
    }
  }  // end of PrototypeRegistryServiceImpl


}


  
  
