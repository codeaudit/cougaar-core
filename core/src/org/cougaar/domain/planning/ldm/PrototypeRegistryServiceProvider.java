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
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

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


  
  
