/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

/**
 * Marker for LDM factory instances.  Although no particular API
 * is specified, instances will support domain-specific factory methods
 * and constants for the use of domain-specific plugins.
 * 
 * Factory instances are created exclusively by their matching Domain object.
 * 
 * @see org.cougaar.domain.planning.ldm.RootFactory for the factory used to create infrastructure objects.
 **/

public interface Factory
{
}
