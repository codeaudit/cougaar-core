/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin.util;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.plan.Verb;

public interface NewAllocationsPredicate extends UnaryPredicate {
    public void setVerb( Verb vb );
}
