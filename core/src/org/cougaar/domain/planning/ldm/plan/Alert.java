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

import org.cougaar.core.society.UniqueObject;

/** Alert interface
 **/


public interface Alert extends UniqueObject {

  /**
   * Text to be displayed by UI to explain the Alert to the user.
   **/
  String getAlertText();

  /**
   * Parameters that contain objects to be acted upon, or chosen from among
   **/
  AlertParameter[] getAlertParameters();

  /**
   * Indicates whether the Alert has been acted upon
   **/
  boolean getAcknowledged();


  static public final int UNDEFINED_SEVERITY = -1;
  static public final int LOW_SEVERITY = 0;
  static public final int MEDIUM_SEVERITY = 1;
  static public final int HIGH_SEVERITY = 2;

  // MIN_SEVERITY and MAX_SEVERITY used to check for valid severity values.
  // Simple minded but works so long as we can keep a contiguous set of severities.
  static public final int MIN_SEVERITY = UNDEFINED_SEVERITY;
  static public final int MAX_SEVERITY = HIGH_SEVERITY;

  /**
   * Indicates Alert severity
   * Should be one of the values defined above.
   */
  int getSeverity();

  static public final int UNDEFINED_TYPE = -1;
  static public final int CONSUMPTION_DEVIATION_TYPE = 5;

  /**
   * Indicates Alert type. 
   * BOZO - I presume this means the type of activity which generated the alert - 
   * transportation, ... Valid types should be defined within the interface ala
   * severities but I haven't a clue what they might be.
   */
  int getType();

  /**
   * Indicates whether UI user is required to take action on this alert
   **/
  boolean getOperatorResponseRequired();


  /**
   * The answer to the Alert. The AlertParameters can also have responses
   **/
  Object getOperatorResponse();
}












