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

import org.cougaar.domain.planning.ldm.plan.AlertParameter;
import org.cougaar.domain.planning.ldm.plan.NewAlertParameter;

/** AlertParameter Implementation
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AlertParameterImpl.java,v 1.2 2001-04-05 19:27:12 mthome Exp $
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

