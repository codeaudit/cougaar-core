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

package org.cougaar.planning.ldm.plan;

/** AlertParameter interface
 * @author  ALPINE <alpine-software@bbn.com>
 *
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
