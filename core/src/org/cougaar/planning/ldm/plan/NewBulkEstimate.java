/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.planning.ldm.plan;

/** NewBulkEstimate Interface
  * Provides setters for pieces of the object that will change.
  * @author  ALPINE <alpine-software@bbn.com>
  *
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
