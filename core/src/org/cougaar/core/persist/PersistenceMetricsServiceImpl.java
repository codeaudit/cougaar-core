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
import java.util.ArrayList;
import org.cougaar.core.service.PersistenceMetricsService;

public class PersistenceMetricsServiceImpl implements PersistenceMetricsService {

  private PersistenceMetricImpl fullAverageMetric = new PersistenceMetricImpl();
  private PersistenceMetricImpl deltaAverageMetric = new PersistenceMetricImpl();
  private PersistenceMetricImpl allAverageMetric = new PersistenceMetricImpl();
  private int base = 0;
  private int size = 0;
  private Metric[] metrics = new Metric[MAX_METRICS];

  void addMetric(PersistenceMetricsService.Metric metric) {
    synchronized (metrics) {
      if (size >= metrics.length) {
        metrics[base++] = metric;
        if (base >= metrics.length) base = 0;
      } else {
        metrics[size++] = metric;
      }
      allAverageMetric.average(metric);
      if (metric.isFull()) {
        fullAverageMetric.average(metric);
      } else {
        deltaAverageMetric.average(metric);
      }
    }
  }

  /**
   * Get all retained metrics. The maximum number retained is
   * currently a constant MAX_METRICS
   **/
  public Metric[] getAll(int which) {
    synchronized (metrics) {
      if (which == ALL) {
        Metric[] result = new Metric[size];
        System.arraycopy(metrics, base, result, 0, size - base);
        System.arraycopy(metrics, 0, result, size - base, base);
        return result;
      } else {
        List result = new ArrayList(size);
        boolean wantFull = which == FULL;
        for (int i = 0; i < size; i++) {
          Metric metric = metrics[(base + i) % metrics.length];
          if (metric.isFull() == wantFull) {
            result.add(metric);
          }
        }
        return (Metric[]) result.toArray(new Metric[result.size()]);
      }
    }
  }

  /**
   * Get the average of all metrics ever generated including the ones
   * that have been dropped due to exceeding MAX_METRICS
   **/
  public Metric getAverage(int which) {
    switch (which) {
    case FULL:
      return fullAverageMetric;
    case DELTA:
      return deltaAverageMetric;
    case ALL:
      return allAverageMetric;
    }
    return null;
  }

  public int getCount(int which) {
    switch (which) {
    case FULL:
      return fullAverageMetric.getCount();
    case DELTA:
      return deltaAverageMetric.getCount();
    case ALL:
      return allAverageMetric.getCount();
    }
    return 01;
  }
}

