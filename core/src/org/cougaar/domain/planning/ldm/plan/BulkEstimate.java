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

import java.util.List;

/** BulkEstimate Interface
  * A BulkEstimate is similar to but not a subclass of PlanElement.
  * A BulkEstimate allows for a PlugIn to specify a Task with a collection
  * of Preference sets and get back a collection of AllocationResults.
  * Each AllocationResult will represent the results of allocating the Task
  * with one of the Preference sets.
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: BulkEstimate.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
  **/

public interface BulkEstimate {
	/** @return Task  The task to be allocated */
	Task getTask();
	
	/** @return List  The collection of preference sets.  Each set will be
	 * represented by a Preference Array.
	 */
	List getPreferenceSets();
	
	/** @return AllocationResult[]  The Array of AllocationResults. 
	 * Note that this collection will be changing until isComplete()
	 */
	AllocationResult[] getAllocationResults();
	
	/** @return boolean  Will be set to true once all of the AllocationResults
	 *  for each preference set have been gathered.
	 */
	boolean isComplete();
	
	/** @return double  The confidence rating of each AllocationResult that
	 * must be reached before the result is valid and the next preference set 
	 * can be tested.  The confidence rating should be between 0.0 and 1.0 with 
	 * 1.0 being the most complete of allocations.
	 */
	double getConfidenceRating();
	
}