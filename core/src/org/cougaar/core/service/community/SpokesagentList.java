/*
 * <copyright>
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

package org.cougaar.core.service.community;

import java.util.Iterator;


/** SpokesagentList Interface
  * A SpokesagentList contains a list of SpokesagentDescriptor objects for a
  * specific community.
  **/

public interface SpokesagentList {


  /**
   * The getCommunityName method returns the name of the community that
   * this SpokesagentList represents.
   * @return Returns the community name
   **/
  String getCommunityName();


  /**
   * Tests for the existence of a spokesagent providing the specified role.
   * @return True if community currently has at least one spokesagent provding
   *         the specified role.
   */
  boolean hasSpokesagent(String role);


  /**
   * Returns an array of community Spokesagent names that provide the specified
   * role for the community.
   * @return Array of Spokesagent names
   */
  Iterator getSpokesagent(String role);


  /**
   * Returns an Iterator for all Spokesagent descriptors associated with this
   * community.
   * @return Iterator for SpokesagentDescriptors
   */
  Iterator getAllSpokesagents();

}