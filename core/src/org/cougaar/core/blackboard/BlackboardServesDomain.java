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

package org.cougaar.core.blackboard;

import java.util.Collection;
import java.util.Enumeration;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * See "root" domain plan.
 */
public interface BlackboardServesDomain
{
  /** Apply predicate against the entire "Blackboard".
   * User provided predicate
   **/
  Enumeration searchBlackboard(UnaryPredicate predicate);

  /** Add Object to the Blackboard Collection
   * (All subscribers will be notified)
   **/
  void add(Object o);

  /** Removed Object to the Blackboard Collection
   * (All subscribers will be notified)
   **/
  void remove(Object o);

  /** Change Object to the Blackboard Collection
   * (All subscribers will be notified)
   **/
  void change(Object o, Collection changes);

  /**
   * Alias for sendDirective(dir, null);
   **/
  void sendDirective(Directive dir);

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if clusters crash.
   **/
  void sendDirective(Directive dir, Collection changeReports);

  PublishHistory getHistory();

  /**
   * Get ABA translation status.
   * @return an ABATranslation giving translations of an ABA.
   * Returns null if the translations are unchanged.
   * @param aba the ABA to translate
   **/
  ABATranslation getABATranslation(AttributeBasedAddress aba);
}
