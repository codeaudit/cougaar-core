/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.cluster.PrivilegedClaimant;

/** Assessor interface.
 *  This is a marker interface for Assessor and Assessor like plugins
 *  ( to include the TriggerManagerPlugIn) that may rescind or change
 *  logplan objects another plugin may have created.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Assessor.java,v 1.1 2000-12-15 21:01:11 mthome Exp $
 */

public interface Assessor extends PrivilegedClaimant {
}
