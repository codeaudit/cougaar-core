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

/** NewAlertParameter interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewAlertParameter.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
 *
 * BOZO - Use of AlertParameter is not clearly defined. Object will probably change
 * when we attempt to actually use it.
 **/

public interface NewAlertParameter extends AlertParameter {


  /**
   * An object whose contents would be meaningful to a UI user who must 
   * respond to the Alert that this AlertParameter is part of.
   **/
  void setParameter(Object param);

  /**
   * A description of the AlertParameter for display in the UI to tell
   * a user what and why he is seeing it.
   **/
  void setDescription(String description);

 
  /**
   * The answer to the question posed by this AlertParameter. This method
   * would be used by the UI to fill in the user's response, if any.
   **/
  void setResponse(Object responseToAlert);

  /**
   * The answer to the isEditable question
   **/
  void setEditable(boolean newEditable);

  /**
   * The answer to the isVisible question
   **/
  void setVisible(boolean newVisible);
}
