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

import java.io.Serializable;

/**
 * The creation time and most recent modification time for a
 * UniqueObject in the local blackboard.
 * <p>
 * This class is immutable.
 * 
 * @see TimestampSubscription
 */
public final class TimestampEntry implements java.io.Serializable {

  /**
   * The "unknown time" is used when the time was not 
   * recorded.
   */
  public static final long UNKNOWN_TIME = -1;

  private long creationTime;
  private final long lastModTime;

  public TimestampEntry(long creationTime, long lastModTime) {
    this.creationTime = creationTime;
    this.lastModTime = lastModTime;
  }

  /**
   * Get the creation time.
   */
  public long getCreationTime() { return creationTime; }

  /**
   * Get the most recent modification time, or the creation
   * time if the object has never been modified.
   */
  public long getModificationTime() { return lastModTime; }

  /**
   * Package-private modifier for the creation time -- for
   * infrastructure use only!.
   * <p>
   * This is only used before the instance is released
   * to the clients, after which the instance is no longer
   * modified and presents an immutable API.
   */
  void private_setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  public String toString() {
    return "("+creationTime+" + "+(lastModTime-creationTime)+")";
  }

  private static final long serialVersionUID = -1209831829038126385L;
}
