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

import org.cougaar.core.blackboard.Publishable;


/** CommunityResponse Interface
  * A CommunityResponse is a direct reply to a CommunityRequest published
  * by a client.  The CommunityResponse will identify the result of the request
  * in a response code.  If the request generated an object (such as a
  * community roster) this generated object is also accessed via the
  * CommunityResponse.
  **/

public interface CommunityResponse extends Publishable {

  // Result codes
  int ERROR   = 0;
  int SUCCESS = 1;
  int FAIL    = 2;
  int GRANT   = 3;
  int DENY    = 4;


  /**
   * The getResultCode method returns the status of the CommunityRequest.
   * @return Verb  Returns the ResultCode of the CommunityRequest
   **/
  int getResultCode();


  /**
   * The getResultMessage method returns any detailed message that may
   * be associated with the response.  For instance, if the result code
   * is ERROR, this method will return an error message to assist in debugging.
   * @return  Returns a message with details associated with the result code
   **/
  String getResultMessage();


  /**
   * The getResponseObject method returns any object that may have been
   * generated by the CommunityService request.
   * @return  Returns request specific object
   **/
  Object getResponseObject();

}
