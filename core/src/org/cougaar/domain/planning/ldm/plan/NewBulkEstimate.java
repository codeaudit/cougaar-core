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

/** NewBulkEstimate Interface
  * Provides setters for pieces of the object that will change.
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: NewBulkEstimate.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
  **/

public interface NewBulkEstimate extends BulkEstimate {	
	
	/** @param allresults  The complete Array of AllocationResults. */
	void setAllocationResults(AllocationResult[] allresults);
	
	/** set a single AllocationResult
		* @param index  The position of the result in the overall result array.
		* This position should correspond to the preference set position.
		* @param aresult
		*/
	void setSingleResult(int index, AllocationResult aresult);
	
	/** @param complete  Should be set to true once all of the AllocationResults
	 *  for each preference set have been gathered.
	 */
	void setIsComplete(boolean complete);
	
}