/* 
 * <copyright>
 * Copyright 2004 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.plugin.deletion;

import org.cougaar.util.UnaryPredicate;

/**
 * @author RTomlinson
 *
 * A DeletionPolicy controls the timing of the deletion of a blackboard object.
 * A predicate checks for the applicability of the policy to the object. The
 * priority disambiguates the case where multiple policies might apply. The
 * deletionDelay supplies an additional delay before the object is actually
 * deleted
 */
public interface DeletionPolicy {
  /**
   * A value signifying that the policy has no defined delay. If the applicable
   * policy has such a value, deletion does not occur.
   */
  long NO_DELETION_DELAY = Long.MIN_VALUE;
  
  /**
   * A priority guaranteed to be less than any defined priority. Only the
   * default policy has NO_PRIORITY. Real policies should have a minimum of
   * MIN_PRIORITY.
   */
  int NO_PRIORITY = Integer.MIN_VALUE;
  
  /**
   * The minimum allowable priority for any non-default policy.
   */
  int MIN_PRIORITY = Integer.MIN_VALUE + 1;
  
  /**
   * The maximum allowable priority for any policy.
   */
  int MAX_PRIORITY = Integer.MAX_VALUE;
  
  /**
   * Get the name of this policy. The name is arbitrary and only used in user
   * interfaces, if any.
   * @return the name of the policy.
   */
  String getName();
  
  /**
   * Get the predicate for this policy. The predicate should return true when
   * applied to objects to which this policy applies.
   * @return the predicate selecting objects for which this policy applies.
   */
  UnaryPredicate getPredicate();
  
  /**
   * Get the additional delay (in scenario time) that should be inserted before
   * a blackboard object is deleted.
   * @return the number of milliseconds of delay before deletion.
   */
  long getDeletionDelay();
  
  /**
   * Get the priority of this policy over other deletion policies. When multiple
   * policies match an object the highest priority policy is used.
   * @return the priority of this policy.
   */
  int getPriority();
}
