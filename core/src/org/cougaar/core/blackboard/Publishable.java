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


/** Objects marked as Publishable may be published directly to the Plan.
 * Provides hooks for additional Plan-related functionality.
 *
 * NOTE: at some point, <em>only</em> Publishable objects will be
 * admitted to the blackboard.  Initially, however, this requirement will
 * not be enforced: Publishing other objects will work, but Publishable
 * services will not be available.
 **/

public interface Publishable
{
  /**
   * Provide a hint to Persistence on how to handle this object. <p>
   *
   * Example?
   * <pre>
   * public class SometimesPersistableObject implements Publishable {
   *   public SometimesPersistableObject(..., boolean persistThisInstance) {
   *     this.persistThisInstance = persistThisInstance;
   *   }
   *   public boolean isPersistable() {
   *     return persistThisInstance;
   *   }
   * </pre>
   **/
  boolean isPersistable();
}

