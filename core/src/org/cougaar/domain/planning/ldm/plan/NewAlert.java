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

/** NewAlert interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewAlert.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface NewAlert extends Alert {

  /**
   * Text to be displayed by UI to explain the Alert to the user.
   **/
  void setAlertText(String alertText);

  /**
   * Parameters that contain objects to be acted upon, or chosen from among
   **/
  void setAlertParameters(AlertParameter[] param);
  
  /**
   * Indicates whether the Alert has been acted upon
   **/
  void setAcknowledged(boolean ack);

  /**
   * Indicates Alert severity
   * Should be one of the values defined in the Alert interface.
   */
  void setSeverity(int severity);

  /**
   * Indicates Alert type. 
   * BOZO - I presume this means the type of activity which generated the alert - 
   * transportation, ... Valid types should be defined within the Alert interface.
   */
  void setType(int type);

  /**
   * Indicates whether UI user is required to take action on this alert
   **/
  void setOperatorResponseRequired(boolean required);

  /**
   * The answer to the Alert. The AlertParameters can also have responses
   **/
  void setOperatorResponse(Object response);

}
