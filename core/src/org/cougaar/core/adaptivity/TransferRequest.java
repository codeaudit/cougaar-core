/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;


/** 
 * published to the blackboard by a plugin in order
 * to send an object to a destination by name
 * This will be picked up by a logic provider which will
 * create a real message and call the Message Transport Service
 * to send it.
 */
public class TransferRequest {

  private String destination;
  private Object contents;

  /**
   * Constructor
   * @param String 'to' destination/agent
   * @param Object contents of the message
  public TransferRequest(String to,  Object messageContents){
    destination = to;
    contents = messageContents;
  }

  /**
   * The destination agent.
   * @return String identifying the agent
   */
  public String getDestination() {
    return destination;
  }
  /**
   * The message content.
   * @return Object containing the contents of the message.
   */
  public Object getContents(){
    return contents;
  }

}


