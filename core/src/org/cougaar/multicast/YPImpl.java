/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
package org.cougaar.multicast;

import java.util.ArrayList;
import java.util.List;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.ClusterIdentifier;

/**
 * Implementation of YellowPages. 
 * Provides lookup of <code>MessageAddress</code>es 
 * for a two-agent test case. 
 **/

public class YPImpl implements YP
{ 
  List mas = new ArrayList();
  
  public YPImpl(){
   mas.add(new ClusterIdentifier("c1")); }

  public YPImpl(List mas) {
    this.mas = mas;
  }
  
  /**
   * Gets message addresses of all interesting sensors in multicast.
   *
   * @param mt a <code>MessageType</code>
   * @return a list of <code>ClusterIdentifier</code>s
   **/  
  public List getDestinations(MessageType mt) {

    // check for naming, and return all interested sensor Message Addresses
    
    if( mt.toString().equals("cluster1"))
      return mas;
    else 
      return null;
    // for testing we return a static list of some names  
  }
  
}
