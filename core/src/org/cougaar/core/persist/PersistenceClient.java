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

package org.cougaar.core.persist;

import java.util.List;

public interface PersistenceClient {
  /**
   * Get the PersistenceIdentity of the client. A PersistenceIdentity
   * uniquely identifies a PersistenceClient regardless of restarts.
   * It is used as the key to retrieving the persisted state of the
   * client after rehydration.
   * @return the PersistenceIdentity of the client.
   **/
  PersistenceIdentity getPersistenceIdentity();

  /**
   * Get a list of things to persist. If an object in the list is an
   * Envelope, the objects in the EnvelopeTuples in the Envelope are
   * persisted. If an object in the list is an EnvelopeTuple, the
   * object(s) in the EnvelopeTuple are persisted. In either case,
   * rehydration will return all such objects inside a
   * PersistenceEnvelope. Persisted Envelopes and EnvelopeTuples obey
   * add/remove semantics across incremental snapshots. If a remove
   * tuple appears, the rehydrated state will not include the object.
   * All other types of objects are simple persisted with no
   * add/remove semantics.
   **/
  List getPersistenceData();
}
