/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

package org.cougaar.core.service.community;


/**
 * Response to request performed by Community Manger.
 **/
public interface CommunityResponse {

  public static final int UNDEFINED  = 0;
  public static final int FAIL       = 1;
  public static final int SUCCESS    = 2;
  public static final int TIMEOUT    = 3;

  /**
   * Status code identifying request result.
   * @return  Status code
   */
  public int getStatus();

  /**
   * Request result as a string.
   * @return String representation of result code.
   */
  public String getStatusAsString();

  /**
   * Get request specific content.  For most requests this will be a
   * org.cougaar.core.service.community.Community instance.  The SearchCommunity
   * request returns a set of org.cougaar.core.service.community.Entity
   * objects.
   */
  public Object getContent();

}
