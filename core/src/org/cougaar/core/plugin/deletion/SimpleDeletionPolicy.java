/*
 * Created on Dec 10, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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

