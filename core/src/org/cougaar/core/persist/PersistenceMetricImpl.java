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

import org.cougaar.core.service.PersistenceMetricsService;

public class PersistenceMetricImpl implements PersistenceMetricsService.Metric {
  private String name;
  private long startTime, endTime, cpuTime, size;
  private boolean full;
  private PersistencePlugin plugin;
  private int count;

  PersistenceMetricImpl(String name,
                        long startTime, long endTime, long cpuTime,
                        long size, boolean full, PersistencePlugin plugin)
  {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.cpuTime = cpuTime;
    this.size = size;
    this.full = full;
    this.plugin = plugin;
    this.count = 1;
  }

  PersistenceMetricImpl() {
  }

  void average(PersistenceMetricsService.Metric metric) {
    startTime += metric.getStartTime();
    endTime += metric.getEndTime();
    cpuTime += metric.getCpuTime();
    size += metric.getSize();
    count += 1;
  }

  public long getStartTime() {
    return count == 0 ? startTime : startTime / count;
  }

  public long getEndTime() {
    return count == 0 ? endTime : endTime / count;
  }

  public long getSize() {
    return count == 0 ? size : size / count;
  }

  public long getCpuTime() {
    return count == 0 ? cpuTime : cpuTime / count;
  }

  public boolean isFull() {
    return full;
  }

  public String getName() {
    return name;
  }

  public Class getPersistencePluginClass() {
    return plugin.getClass();
  }

  public String getPersistencePluginName() {
    return plugin.getName();
  }

  public int getPersistencePluginParamCount() {
    return plugin.getParamCount();
  }

  public String getPersistencePluginParam(int i) {
    return plugin.getParam(i);
  }

  public int getCount() {
    return count;
  }

  public String toString() {
    return "Persisted "
      + (full ? "full" : "delta")
      + name
      + ", "
      + size
      +" bytes in "
      + (endTime - startTime) + " ms"
      + ((cpuTime > 0L) ? (" using " + cpuTime) : "")
      + " ms cpu";
  }
}

