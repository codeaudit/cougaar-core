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

import org.cougaar.domain.planning.ldm.plan.Task;

/** AllocationforCollections Interface -- represents
  * setter methods for the RemoteClusterAllocationLP and the Rescind
  * LP to keep track of what tasks were to sent out. 
  * @author       ALPINE <alpine-software@bbn.com>
  * @version      $Id: AllocationforCollections.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
  **/

public interface AllocationforCollections extends PEforCollections {
  
  Task getAllocationTask();
  void setAllocationTask(Task t);

}