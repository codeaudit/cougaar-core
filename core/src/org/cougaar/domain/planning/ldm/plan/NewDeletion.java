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

import org.cougaar.core.society.UID;

import java.util.Enumeration;

/** NewDeletion Interface
 * provides setter methods to create a Deletion object
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewDeletion.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
 **/

public interface NewDeletion extends Deletion, NewDirective {
		
  /** 
   * Sets the task the deletion is in reference to.
   * @param uid The UID of the Task to be referenced in the Deletion. 
   **/
  void setTaskUID(UID uid);
}
