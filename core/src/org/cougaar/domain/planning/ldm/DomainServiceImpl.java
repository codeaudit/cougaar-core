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

import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

import java.util.HashMap;

public class DomainServiceImpl implements DomainService {

  private Domain rootDomain = null;
  private RootFactory myRootFactory = null;
  private LDMServesPlugIn registryService;

  //When cluster creates this service it will
  //pass a reference to it's PrototypeRegistryService in the form
  // of itself acting as LDMServesPlugin...
  //In the future these service may dynamically find each otehr
  public DomainServiceImpl(LDMServesPlugIn registryService) {
    this.registryService = registryService;
    //get the domains set up
    DomainManager.initialize();
    // set up the root domain, especially the root factory.
    this.rootDomain = DomainManager.find("root");
    this.myRootFactory = (RootFactory) rootDomain.getFactory(registryService);
 }

  /**
   * Answer with a reference to the Factory
   * It is inteded that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  public ClusterObjectFactory getClusterObjectFactory() {
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
        f = d.getFactory(registryService);   // create a new factory
        if (f == null) return null; // failed to create the factory
        domainFactories.put(d, f);
      }
      factories.put(key, f);    // cache the factory against the name
      return f;
    }
  }

}  

