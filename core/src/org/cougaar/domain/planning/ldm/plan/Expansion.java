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

/** Expansion Interface
 * An Expansion is a kind of PlanElement that
 * represents a Workflow that is the result
 * of an expansion of a Task.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Expansion.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface Expansion extends PlanElement {
	
    /** Returns the Workflow created by the expansion
      * of the Task
      * @return Workflow
      **/
  Workflow getWorkflow();
}
