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


/**
 * Defines a community spokesagent.  Each spokesagent is associated with one
 * or more community roles.
 */
public class SpokesagentDescriptor {

  // Instance variables
  private String identifier;
  private String[] roles;

  /**
   * Constructor.
   */
  public SpokesagentDescriptor (String identifier,
                                   String[] roles) {
    this.identifier = identifier;
    this.roles = roles;
  }

  /**
   * Returns spokesagent identifier.
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Returns roles satisfied by spokesagent.
   */
  public String[] getRoles() {
    return roles;
  }

}