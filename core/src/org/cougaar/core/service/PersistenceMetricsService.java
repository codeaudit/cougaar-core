/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.service;

public interface PersistenceMetricsService {
  interface Metric {
    long getStartTime();
    long getEndTime();
    long getSize();
    long getCpuTime();
    boolean isFull();
    String getName();
    Class getPersistencePluginClass();
    String getPersistencePluginName();
    int getPersistencePluginParamCount();
    String getPersistencePluginParam(int i);
  }

  /**
   * Designates that averaging should include only full
   * snapshots
   **/
  static final int FULL = 1;

  /**
   * Designates that averaging should include only delta
   * snapshots
   **/
  static final int DELTA = 2;

  /**
   * Designates that averaging should include all
   * snapshots
   **/
  static final int ALL = 3;

  static final int MAX_METRICS = 100;

  /**
   * Get all retained metrics. The maximum number retained is
   * currently a constant MAX_METRICS
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be included.
   **/
  Metric[] getAll(int which);

  /**
   * Get the average of all metrics ever generated including the ones
   * that have been dropped due to exceeding MAX_METRICS
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be averaged.
   **/
  Metric getAverage(int which);

  /**
   * Get the count of all persistence snapshots taken (the denominator
   * of the average).
   * @return the count of all persistence snapshots taken.
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be counted.
   **/
  int getCount(int which);
}
