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

/**
 * Factory methods for all LDM objects.
 * @deprecated Use RootFactory as a direct replacement for LdmFactory
 * or Factory as appropriate.
 **/

public interface LdmFactory 
  extends ClusterObjectFactory, Factory
{
}
