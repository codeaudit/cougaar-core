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
 
package org.cougaar.core.blackboard;

import org.cougaar.core.mts.MessageAddress;

/**
 * Directive interface
 * Directive is the highest-level message which is directly relevant to
 * real operations.  There are no direct implementations of Directive,
 * but there are several subclasses with implementations.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public interface NewDirective extends Directive {
  /*
  	*	Depricated because it is inherited from the base interface Message
   * @param asource - Set the MessageAddress of the originator of this message
   */
  void setSource(MessageAddress asource);
  
  /*
   * @param adestination - Set the MessageAddress of the receiver of this message
   */
  void setDestination(MessageAddress adestination);
  
 }
