/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.core.service.*;

import org.cougaar.core.agent.*;

import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;

import java.util.*;

public class PrototypeRegistry implements PrototypeRegistryService {
  /** THEINITIALREGISTRYSIZE specifies the initial size of the HashMap theRegistry,
   *   which contains the Strings of RegistryTerms
   **/
  private final static int THE_INITIAL_REGISTRY_SIZE = 89;
  private final HashMap myRegistry = new HashMap(THE_INITIAL_REGISTRY_SIZE);

  public PrototypeRegistry() {}

  /** set of PrototypeProvider LDM Plugins **/
  // might want this to be prioritized lists
  private final List prototypeProviders = new ArrayList();

  public void addPrototypeProvider(PrototypeProvider prov) {
    synchronized (prototypeProviders) {
      prototypeProviders.add(prov);
    }
  }

  /** set of PropertyProvider LDM Plugins **/
  private final List propertyProviders = new ArrayList();
  public void addPropertyProvider(PropertyProvider prov) {
    synchronized (propertyProviders) {
      propertyProviders.add(prov);
    }
  }

  // use the registry for registering prototypes for now.
  // later, just replace with a hash table.
  public void cachePrototype(String aTypeName, Asset aPrototype) {
    myRegistry.put(aTypeName.intern(), aPrototype);
  }

  public boolean isPrototypeCached(String aTypeName) {
    return (myRegistry.get(aTypeName) != null);
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
      found = (Asset) myRegistry.get(aTypeName);
      if (found != null) return found;
    } catch (ClassCastException cce) {}
    
    synchronized (prototypeProviders) {
      // else, try the prototype providers
      int l = prototypeProviders.size();
      for (int i = 0; i<l; i++) {
        PrototypeProvider pp = (PrototypeProvider) prototypeProviders.get(i);
        found = pp.getPrototype(aTypeName, anAssetClass);
        if (found != null) return found;
      }
    }

    // might want to throw an exception in a later version
    return null;
  }

  public void fillProperties(Asset anAsset) {
    // expose the asset to all propertyproviders
    synchronized (propertyProviders) {
      int l = propertyProviders.size();
      for (int i=0; i<l; i++) {
        PropertyProvider pp = (PropertyProvider) propertyProviders.get(i);
        pp.fillProperties(anAsset);
      }
    }
  }
        
  /** hash of PropertyGroup interface to Lists of LatePropertyProvider instances. **/
  private final HashMap latePPs = new HashMap(11);
  /** list of LatePropertyProviders who supply all PropertyGroups **/
  private final ArrayList defaultLatePPs = new ArrayList(3); 
  public void addLatePropertyProvider(LatePropertyProvider lpp) {
    Collection c = lpp.getPropertyGroupsProvided();
    if (c == null) {
      synchronized (defaultLatePPs) {
        defaultLatePPs.add(lpp);
      }
    } else {
      try {
        for (Iterator it = c.iterator(); it.hasNext(); ) {
          Class pgc = (Class) it.next();
          synchronized (latePPs) {
            ArrayList l = (ArrayList) latePPs.get(pgc);
            if (l == null) {
              l = new ArrayList(3);
              latePPs.put(pgc,l);
            }
            synchronized (l) {
              l.add(lpp);
            }
          }
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
    ArrayList c;
    synchronized (latePPs) {
      c = (ArrayList) latePPs.get(pgclass);
    }
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
    synchronized (c) {
      int l = c.size();
      for (int i = 0; i<l; i++) {
        LatePropertyProvider lpp = (LatePropertyProvider) c.get(i);
        PropertyGroup pg = lpp.fillPropertyGroup(anAsset, pgclass, t);
        if (pg != null) 
          return pg;
      }
    }
    return null;
  }    
  
 /** Expose the Registry to consumers. 
   **/
  /*
  public Registry getRegistry() {
    return myRegistry;
  }
  */

  //metrics service hooks
  public int getPrototypeProviderCount() {
    return prototypeProviders.size();
  }
  public int getPropertyProviderCount() {
    return propertyProviders.size();
  }
  public int getCachedPrototypeCount() {
    return myRegistry.size();
  }

}


