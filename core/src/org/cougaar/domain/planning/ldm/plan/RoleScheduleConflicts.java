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

/** RoleScheduleConflicts Interface -- represents
  * setter methods for the planelments being monitored by logic providers
  * for possible rolechedule and available schedule conflicts.  Setters are
  * only called by the INFRASTRUCTURE!!! 
  * @author       ALPINE <alpine-software@bbn.com>
  * @version      $Id: RoleScheduleConflicts.java,v 1.2 2001-04-05 19:27:19 mthome Exp $
  **/

public interface RoleScheduleConflicts {
  
  void setPotentialConflict(boolean conflict);
  void setAssetAvailabilityConflict(boolean availconflict);
  void setCheckConflicts(boolean check);
  boolean checkConflicts();

}