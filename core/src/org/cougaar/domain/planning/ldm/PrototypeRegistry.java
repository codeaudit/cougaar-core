/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

import java.util.*;

public class PrototypeRegistry implements PrototypeRegistryService {

  private Registry myRegistry = null;

  public PrototypeRegistry() {
    // initialize LDM parts
    myRegistry = new Registry();
  }

  /** set of PrototypeProvider LDM PlugIns **/
  // might want this to be prioritized lists
  private List prototypeProviders = new ArrayList();

  public void addPrototypeProvider(PrototypeProvider prov) {
    prototypeProviders.add(prov);
  }

  /** set of PropertyProvider LDM PlugIns **/
  private List propertyProviders = new ArrayList();
  public void addPropertyProvider(PropertyProvider prov) {
    propertyProviders.add(prov);
  }

  // use the registry for registering prototypes for now.
  // later, just replace with a hash table.
  public void cachePrototype(String aTypeName, Asset aPrototype) {
    myRegistry.createRegistryTerm(aTypeName, aPrototype);
  }

  public boolean isPrototypeCached(String aTypeName) {
    return (myRegistry.findDomainName(aTypeName) != null);
  }    

  public Asset getPrototype(String aTypeName) {
    return getPrototype(aTypeName, null);
  }
  public Asset getPrototype(String aTypeName, Class anAssetClass) {
    Asset found = null;

    // look in our registry first.
    // the catch is in case some bozo registered a non-asset under this
    // name.
    try {
      found = (Asset) myRegistry.findDomainName(aTypeName);
      if (found != null) return found;
    } catch (ClassCastException cce) {}
    
    // else, try the prototype providers
    for (Iterator pps = prototypeProviders.iterator(); pps.hasNext(); ) {
      PrototypeProvider pp = (PrototypeProvider) pps.next();
      found = pp.getPrototype(aTypeName, anAssetClass);
      if (found != null) return found;
    }
    // might want to throw an exception in a later version
    return null;
  }

  public void fillProperties(Asset anAsset) {
    // expose the asset to all propertyproviders
    for (Iterator pps = propertyProviders.iterator(); pps.hasNext(); ) {
      PropertyProvider pp = (PropertyProvider) pps.next();
      pp.fillProperties(anAsset);
    }
  }
        
  /** hash of PropertyGroup interface to Lists of LatePropertyProvider instances. **/
  private HashMap latePPs = new HashMap(11);
  /** list of LatePropertyProviders who supply all PropertyGroups **/
  private ArrayList defaultLatePPs = new ArrayList(3); 
  public void addLatePropertyProvider(LatePropertyProvider lpp) {
    Collection c = lpp.getPropertyGroupsProvided();
    if (c == null) {
      defaultLatePPs.add(lpp);
    } else {
      try {
        for (Iterator it = c.iterator(); it.hasNext(); ) {
          Class pgc = (Class) it.next();
          ArrayList l = (ArrayList) latePPs.get(pgc);
          if (l == null) {
            l = new ArrayList(3);
            latePPs.put(pgc,l);
          }
          l.add(lpp);
        }
      } catch (ClassCastException e) {
        System.err.println("LatePropertyProvider "+lpp+" returned an illegal PropertyGroup spec:");
        e.printStackTrace();
      }
    }
  }

  /** hook for late-binding **/
  public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pgclass, long t) {
    // specifics
    ArrayList c = (ArrayList) latePPs.get(pgclass);
    PropertyGroup pg = null;
    if (c != null) {
      pg = tryLateFillers(c, anAsset, pgclass, t);
    }
    if (pg == null) {
      pg = tryLateFillers(defaultLatePPs, anAsset, pgclass, t);
    }
    return pg;
  }

  /** utility method of lateFillPropertyGroup() **/
  private PropertyGroup tryLateFillers(ArrayList c, Asset anAsset, Class pgclass, long t)
  {
    int l = c.size();
    for (int i = 0; i<l; i++) {
      LatePropertyProvider lpp = (LatePropertyProvider) c.get(i);
      PropertyGroup pg = lpp.fillPropertyGroup(anAsset, pgclass, t);
      if (pg != null) 
        return pg;
    }
    return null;
  }    
 /** Expose the Registry to consumers. 
   **/
  public Registry getRegistry() {
    return myRegistry;
  }

}
