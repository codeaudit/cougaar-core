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

import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.PlanElementForAssessor;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;

/** PEforCollections Interface -- represents
  * setter methods for the NotificationLP to set the 
  * Reported Result for PlanElements
  * @author       ALPINE <alpine-software@bbn.com>
  * @version      $Id: PEforCollections.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
  **/

public interface PEforCollections extends PlanElementForAssessor {
  
  boolean shouldDoNotification();

  void setNotification(boolean v);

}
