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

import org.cougaar.core.component.Service;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

public interface DomainService extends Service {
  /**
   * Answer with a reference to the Factory
   * It is inteded that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  ClusterObjectFactory getClusterObjectFactory();

  /** expose the LDM factory instance to consumers.
   *  @return LdmFactory The fatory object to use in constructing LDM Objects
   **/
  RootFactory getFactory();
 
  /** @deprecated use getFactory() **/
  RootFactory getLdmFactory();

  /** create a domain-specific factory **/
  Factory getFactory(String domainname);

}  
