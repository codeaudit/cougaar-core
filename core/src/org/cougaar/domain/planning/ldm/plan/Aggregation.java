/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

/** Aggregation Interface
 * An Aggregation is a kind of PlanElement that
 * merges multiple tasks into a single task.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Aggregation.java,v 1.2 2001-04-05 19:27:12 mthome Exp $
 **/

public interface Aggregation extends PlanElement {
	
    /** Returns the Composition created by the aggregations of the task.
      * @see org.cougaar.domain.planning.ldm.plan.Composition
      * @return Composition
      **/
   Composition getComposition();
}
