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

/** AlertParameter interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AlertParameter.java,v 1.2 2001-04-05 19:27:12 mthome Exp $
 *
 * BOZO - Use of AlertParameter is not clearly defined. Object will probably change
 * when we attempt to actually use it.
 **/

public interface AlertParameter extends java.io.Serializable {

  /**
   * An object whose contents would be meaningful to a UI user who must 
   * respond to the Alert that this AlertParameter is part of.
   **/
  Object getParameter();

  /**
   * A description of the AlertParameter for display in the UI to tell
   * a user what and why he is seeing it.
   **/
  String getDescription();

  /**
   * The answer to the question posed by this AlertParameter. This method
   * would be used by the UI to fill in the user's response, if any.
   **/
  Object getResponse();

  /**
   * Should this parameter be visible. Invisible parameters simply
   * carry information needed to handle the alert when it is
   * acknowledged.
   **/
  boolean isVisible();

  /**
   * Should this parameter be editable. Uneditable parameters simply
   * supply additional information to the operator, but the operator
   * can't change them.
   **/
  boolean isEditable();
}
