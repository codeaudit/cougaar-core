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
 * A simple implementation of DeletionPolicy providing a fixed set of values
 * established when constructed. Setters are protected an may be overridden in
 * subclasses.
 */
public class SimpleDeletionPolicy implements DeletionPolicy {
  private String name;
  private UnaryPredicate predicate;
  private long deletionDelay;
  private int priority;

  private static class DefaultDeletionPolicyPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return true;
    }
  };

  public SimpleDeletionPolicy(
    String name,
    UnaryPredicate predicate,
    long deletionDelay,
    int priority) {
    setName(name);
    setPredicate(predicate);
    setDeletionDelay(deletionDelay);
    setPriority(priority);
  }
  
  /**
   * Package access constructor create a default policy (having NO_PRIORITY
   * priority).
   * @param deletionDelay
   */
  SimpleDeletionPolicy(long deletionDelay) {
    this(
      "Default deletion policy",
      new DefaultDeletionPolicyPredicate(),
      deletionDelay,
      NO_PRIORITY);
  }
  
  public static boolean isDefaultDeletionPolicy(DeletionPolicy policy) {
    if (policy instanceof SimpleDeletionPolicy) {
      return ((SimpleDeletionPolicy) policy).isDefaultDeletionPolicy();
    }
    return false;
  }

  private boolean isDefaultDeletionPolicy() {
    return getPredicate() instanceof DefaultDeletionPolicyPredicate;
  }

  protected void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  protected void setPredicate(UnaryPredicate predicate) {
    this.predicate = predicate;
  }

  public UnaryPredicate getPredicate() {
    return predicate;
  }

  protected void setPriority(int priority) {
    if (priority < MIN_PRIORITY
      && !this.isDefaultDeletionPolicy()) {
      throw new IllegalArgumentException("Invalid priority");
    }
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  protected void setDeletionDelay(long deletionDelay) {
    this.deletionDelay = deletionDelay;
  }

  public long getDeletionDelay() {
    return deletionDelay;
  }
}

