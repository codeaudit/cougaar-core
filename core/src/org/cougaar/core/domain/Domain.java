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

package org.cougaar.core.domain;

import java.util.Set;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * Describe an COUGAAR "Pluggable Domain Package" which consists of
 * a set of domain-specific objects as represented by a 
 * Factory class, and a set of LogicProviders.
 */
public interface Domain {

  /** returns the domain name, which must be unique */
  String getDomainName();

  /** returns the XPlan instance for the domain */
  XPlan getXPlan();

  /** returns the Factory for this Domain */
  Factory getFactory();

  /** invoke the MessageLogicProviders for this domain */
  void invokeMessageLogicProviders(DirectiveMessage message);

  /** invoke the EnvelopeLogicProviders for this domain */
  void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                    boolean isPersistenceEnvelope);

  /** invoke the RestartLogicProviders for this domain */
  void invokeRestartLogicProviders(MessageAddress cid);

  /**
   * invoke the ABAChangeLogicProviders for this domain.
   *
   * @param communities the set of communities with potiential
   * changes. If null, all communities may have changed.
   */
  void invokeABAChangeLogicProviders(Set communities);

}
