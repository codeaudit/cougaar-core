/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;

/** Context is a domain-refined description of the "problem"
 * that a task a task (and perhaps other plan elements) are
 * related to when there may be multiple such problems competing
 * for resources within a single logplan.
 *
 * A Context must be Serializable and will be both persisted and
 * transferred between clusters.  Instances should be small and 
 * relatively self-contained.
 *
 * Example: in the ALP domain, the Context of a task will
 * be an object which describes zero or more OPlans (e.g. their
 * UIDs or Names).
 *
 * @see org.cougaar.domain.planning.ldm.plan.Task
 **/

public interface Context 
  extends Serializable
{
}
