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

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

/** Blackboard objects which implement this interface
 * will have the appropriate methods invoked when
 * publishAdd(), publishChange() or publishRemove().
 * Typically, these methods are used to maintain
 * some object state, check for well-formedness and/or
 * emit warnings about various problems.
 **/
public interface ActiveSubscriptionObject {
  /** called by Subscriber.publishAdd().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void addingToBlackboard(Subscriber subscriber);
  /** called by Subscriber.publishChange().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void changingInBlackboard(Subscriber subscriber);
  /** called by Subscriber.publishRemove().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void removingFromBlackboard(Subscriber subscriber);
}
