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

import org.cougaar.domain.planning.ldm.plan.AlertParameter;
import org.cougaar.domain.planning.ldm.plan.NewAlertParameter;

/** AlertParameter Implementation
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class AlertParameterImpl implements AlertParameter, NewAlertParameter {

  // Description of the parameter
  protected String myDescription;
  // Actual parameter
  protected Object myParameter;
  // Operator response
  protected Object myResponse;
  // Editable
  protected boolean editable = true;
  // Visible
  protected boolean visible = true;

  /**
   * Constructor - takes no arguments
   */
  public AlertParameterImpl() {
    myDescription = null;
    myParameter = null;
    myResponse = null;
  }

  /**
   * Constructor
   *
   * @param description String description for the parameter
   * @param parameter Object actual object associated with the parameter
   */
  public AlertParameterImpl(String description, Object parameter) {
    myDescription = description;
    myParameter = parameter;
  }

  /**
   * getParameter - return Object whose contents would be meaningful to a UI user 
   * who must respond to the Alert associated with this AlertParameter
   *
   * @return Object
   */
  public Object getParameter() {
    return myParameter;
  }

  /**
   * setParameter - set object whose contents would be meaningful to a UI user 
   * who must respond to the Alert associated with this AlertParameter.
   *
   * @param param Object
   */
  public void setParameter(Object param){
    myParameter = param;
  }

  /**
   * getDescription - returns a description of the AlertParameter for display in 
   * the UI to tell a user what and why he is seeing it.
   *
   * @return String
   */
  public String getDescription() {
    return myDescription;
  }

  /**
   * setDescription - sets a description of the AlertParameter for display in the
   * UI to tell a user what and why he is seeing it.
   *
   * @param paramDescription String
   */
  public void setDescription(String paramDescription) {
    myDescription = paramDescription;
  }

  /**
   * setResponse - saves the answer to the question posed by this AlertParameter. 
   * This method would be used by the UI to fill in the user's response, if any.
   *
   * @param Object response
   **/
  public void setResponse(Object response) {
    myResponse = response;
  }

  /**
   * getRespose - The answer to the question posed by this AlertParameter. This method
   * would be used by the UI to fill in the user's response, if any.
   * 
   * @return Object
   **/
  public Object getResponse() {
    return myResponse;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean newEditable) {
    editable = newEditable;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean newVisible) {
    visible = newVisible;
  }
}

