/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.server;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.wp.WhitePagesMessage;

/**
 * This is the response from the server to the resolver.
 */
public class WPAnswer
extends WhitePagesMessage {
  private final Request req;
  private final Object result;
  private final long ttl;
  public WPAnswer(
      MessageAddress source,
      MessageAddress target,
      Request req,
      Object result,
      long ttl) {
    super(source, target);
    this.req = req;
    this.result = result;
    this.ttl = ttl;
    if (req == null) {
      throw new IllegalArgumentException("null request");
    }
  }
  public Request getRequest() { return req; }
  public Object getResult() { return result; }
  public long getTTL() { return ttl; }
  // renew, denied, etc
  public String toString() {
    return 
      "(WPAnswer"+
      " "+super.toString()+
      " req="+req+
      " result="+result+
      ")";
  }
}
