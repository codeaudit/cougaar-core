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

import org.cougaar.util.log.Logger;

/**
 * Interface for objects that require special persistence
 * and rehydration handling.
 * <p>
 * This is typically used as a hack for odd blackboard objects that
 * can't [de]serialize correctly, or unusual domains.  <b>AVOID</b>
 * this interface unless you absolutely must use it!
 */
public interface ActivePersistenceObject {

  /**
   * Confirm that this object, not published to the blackboard
   * but reachable from a blackboard object, should be
   * persisted.
   * <p>
   * For example, if object X has a pointer to object Y,
   * and only X is on the blackboard, then Y is "weakly reachable".
   * If Y implements the ActivePersistenceObject interface, Y will be 
   * asked to confirm if Y should be persisted.
   * <p>
   * The default for objects that don't implement the
   * ActivePersistenceObject interface is "false" (ie. persist
   * all reachable objects).
   *
   * @return true if this object should <b>not</b> be persisted
   */
  boolean skipUnpublishedPersist(Logger logger);

  /**
   * Validate an object that has just been persistence
   * deserialized.
   * <p>
   * This occurs prior to the "postRehydration" validation,
   * but is very similar.  The difference is that this
   * check is done as the objects are being deserialized,
   * instead of after all of them have been deserialized.
   */
  void checkRehydration(Logger logger);

  /**
   * Fix an object once rehydration has completed.
   * <p>
   * This is used as a last-minute cleanup, in case the
   * object requires special deserialization work.
   */
  void postRehydration(Logger logger);
}
