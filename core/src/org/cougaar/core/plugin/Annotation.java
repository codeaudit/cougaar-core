/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.io.Serializable;

/** 
 * An Infrastructure-opaque type used for tagging various LDM objects 
 * with PlugIn-specific information.  The infrastructure will never copy
 * or transmit Annotations between clusters, but will save them when 
 * persisting (hence the extends Serializable).
 *
 * Annotations may be attached to appropriate plan objects at object
 * construction time.  They should never be modified (or even examined)
 * by any entity other than the one that created the plan object.
 * 
 * This structure is intentionally opaque to the infrastructure.
 *
 * @see org.cougaar.domain.planning.ldm.plan.Task
 * @see org.cougaar.domain.planning.ldm.plan.Workflow
 * @see org.cougaar.domain.planning.ldm.plan.PlanElement
 **/
public interface Annotation 
  extends Serializable
{
}
