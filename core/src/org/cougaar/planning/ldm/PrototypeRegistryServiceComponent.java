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

package org.cougaar.planning.ldm;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PrototypeRegistry;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component provides the PrototypeRegistryService.
 */
public final class PrototypeRegistryServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private PrototypeRegistry pr;
  private PrototypeRegistryService prS;
  private PrototypeRegistryServiceProvider prSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // create a single per-agent uid service instance
    this.pr = new PrototypeRegistry();
    this.prS = new PrototypeRegistryServiceImpl();

    // create and advertise our service
    this.prSP = new PrototypeRegistryServiceProvider();
    sb.addService(PrototypeRegistryService.class, prSP);
  }

  public void unload() {
    // revoke our service
    if (prSP != null) {
      sb.revokeService(PrototypeRegistryService.class, prSP);
      prSP = null;
    }
    // clear pr?
    super.unload();
  }

  private class PrototypeRegistryServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (PrototypeRegistryService.class.isAssignableFrom(serviceClass)) {
        return prS;
      } else {
        return null;
      }
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service)  {
    }
  }

  /** adapter for PrototypeRegistry -to- PrototypeRegistryService */
  private final class PrototypeRegistryServiceImpl implements PrototypeRegistryService {
    public void addPrototypeProvider(PrototypeProvider prov) {
      pr.addPrototypeProvider(prov);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      pr.addPropertyProvider(prov);
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      pr.addLatePropertyProvider(lpp);
    }
    public void cachePrototype(String aTypeName, Asset aPrototype) {
      pr.cachePrototype(aTypeName, aPrototype);
    }
    public boolean isPrototypeCached(String aTypeName) {
      return pr.isPrototypeCached(aTypeName);
    }
    public Asset getPrototype(String aTypeName, Class anAssetClass) {
      return pr.getPrototype(aTypeName, anAssetClass);
    }
    public Asset getPrototype(String aTypeName) {
      return pr.getPrototype(aTypeName);
    }
    public void fillProperties(Asset anAsset) {
      pr.fillProperties(anAsset);
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return pr.lateFillPropertyGroup(anAsset, pg, time);
    }
    //metrics service hooks
    public int getPrototypeProviderCount() {
      return pr.getPrototypeProviderCount();
    }
    public int getPropertyProviderCount() {
      return pr.getPropertyProviderCount();
    }
    public int getCachedPrototypeCount() {
      return pr.getCachedPrototypeCount();
    }
  }  // end of PrototypeRegistryServiceImpl

}
