/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

/**
 * PlugIns and internal subscription clients which implement 
 * PrivilegedClaimant are allowed to manipulate logplan objects 
 * freely, effectively ignoring claims (without warnings, etc).
 * 
 * Note that Claim warnings are currently just warnings, 
 * regardless of how scary looking they are.  This is a change
 * from some previous implementations which enforced claims.
 *
 * Note also that use of this class circumvents an entire class of 
 * error checks which usually catch problems, so make sure that you
 * really need this before using it.
 *
 * @see org.cougaar.core.cluster.Claimable
 **/

public interface PrivilegedClaimant {
}
