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

import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

import java.util.HashMap;

public class DomainServiceImpl implements DomainService {
  public DomainServiceImpl() { }

  // set up the root domain, especially the root factory.
  private Domain rootDomain = DomainManager.find("root");
  // this was LDMServesPlugIn 
  // MUST FIX BEFORE WE CONNECT THIS!!!!
  //private RootFactory myRootFactory = (RootFactory) rootDomain.getFactory(this);
  private RootFactory myRootFactory = (RootFactory) rootDomain.getFactory(null);

  /**
   * Answer with a reference to the Factory
   * It is inteded that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  public ClusterObjectFactory getClusterObjectFactory()
  {
    return myRootFactory;
  }

  /** expose the LDM factory instance to consumers.
   *  @return LdmFactory The fatory object to use in constructing LDM Objects
   **/
  public RootFactory getFactory(){
    return myRootFactory;
  }

  /** map of domainname to domain factory instance **/
  private HashMap factories = new HashMap(11);
  /** map of domains to factories. synchronized on factories **/
  private HashMap domainFactories = new HashMap(11);
 
  /** @deprecated use getFactory() **/
  public RootFactory getLdmFactory() {
    return getFactory();
  }

  /** create a domain-specific factory **/
  public Factory getFactory(String domainname) {
    String key = domainname;
    synchronized (factories) {
      Factory f = (Factory) factories.get(key);
      if (f != null) return f;

      // bail out for root
      if ("root".equals(domainname)) {
        factories.put(key, myRootFactory);
        return myRootFactory;
      }
        
      Domain d = DomainManager.find(key);
      if (d == null) return null; // couldn't find the domain!

      f = (Factory) domainFactories.get(d); // check the domain factories set first
      if (f == null) {
        // In this case the this was an LDMServesPlugIn 
        // MUST FIX THIS WHEN WE CONNECT THIS UP
        //f = d.getFactory(this);   // create a new factory
        f = d.getFactory(null);   // create a new factory
        if (f == null) return null; // failed to create the factory
        domainFactories.put(d, f);
      }
      factories.put(key, f);    // cache the factory against the name
      return f;
    }
  }

}  
