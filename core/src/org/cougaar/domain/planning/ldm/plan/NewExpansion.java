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

import java.util.List;

/** NewExpansion Interface
   * Allows access to the single sub-task's allocation result information
   * used to aggregate this Expansion's latest reported allocationresult
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewExpansion.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
   **/

public interface NewExpansion extends Expansion {
  
  /** Called by an Expander PlugIn to get the latest copy of the allocationresults
   *  for each subtask.
   *  Information is stored in a List which contains a SubTaskResult for each subtask.  
   *  Each of the SubTaskResult objects contain the following information:
   *  Task - the subtask, boolean - whether the result changed,
   *  AllocationResult - the result used by the aggregator for this sub-task.
   *  The boolean indicates whether the AllocationResult changed since the 
   *  last time the collection was cleared by the plugin (which should be
   *  the last time the plugin looked at the list).
   *  NOTE!!! This accessor should only be called once in a plugin execute cycle as
   *  the list will be cleared as soon as this accessor is called.  The information container
   *  in the return list should be directly related to an update of the reportedAllocationResult
   *  slot on the Expansion that the plugin woke up on.
   *  @return List
   */
  List getSubTaskResults();
 
}