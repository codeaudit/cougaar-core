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

/** NewAlert interface
 * @author  ALPINE <alpine-software@bbn.com>
 *
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
