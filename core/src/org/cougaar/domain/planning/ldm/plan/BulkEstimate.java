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

package org.cougaar.domain.planning.ldm.plan;

import java.util.List;

/** BulkEstimate Interface
  * A BulkEstimate is similar to but not a subclass of PlanElement.
  * A BulkEstimate allows for a PlugIn to specify a Task with a collection
  * of Preference sets and get back a collection of AllocationResults.
  * Each AllocationResult will represent the results of allocating the Task
  * with one of the Preference sets.
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: BulkEstimate.java,v 1.3 2001-08-22 20:14:10 mthome Exp $
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