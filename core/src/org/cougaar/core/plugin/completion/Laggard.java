/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.completion;

import java.io.Serializable;
import java.text.DecimalFormat;
import org.cougaar.core.mts.MessageAddress;

class Laggard implements Comparable, Serializable {
  private long timestamp = System.currentTimeMillis();
  private MessageAddress agent;
  private double blackboardCompletion;
  private double cpuConsumption;
  private boolean isLaggard;
//   private Map verbCounts = new HashMap();
  Laggard(MessageAddress me,
          double blackboardCompletion,
          double cpuConsumption,
          boolean isLaggard)
  {
    this.agent = me;
    this.blackboardCompletion= blackboardCompletion;
    this.cpuConsumption = cpuConsumption;
    this.isLaggard = isLaggard;
  }

  Laggard(MessageAddress me, Laggard oldLaggard) {
    agent = me;
    if (oldLaggard != null) {
      blackboardCompletion = oldLaggard.blackboardCompletion;
      cpuConsumption = oldLaggard.cpuConsumption;
      isLaggard = oldLaggard.isLaggard;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public MessageAddress getAgent() {
    return agent;
  }

  public double getBlackboardCompletion() {
    return blackboardCompletion;
  }

  public double getCPUConsumption() {
    return cpuConsumption;
  }

  public boolean isLaggard() {
    return isLaggard;
  }

//   public Map getVerbCounts() {
//     return verbCounts;
//   }

  public int compareTo(Object o) {
    Laggard that = (Laggard) o;
    if (isLaggard()) {
      if (!that.isLaggard()) return -1;
    } else {
      if (that.isLaggard()) return 1;
      return 1;
    }
    double diff = (this.blackboardCompletion - that.blackboardCompletion +
                   that.cpuConsumption - this.cpuConsumption);
    if (diff < 0.0) return -1;
    if (diff > 0.0) return 1;
    return this.agent.toString().compareTo(that.agent.toString());
  }

  private static final DecimalFormat format = new DecimalFormat("0.00");

  public String toString() {
    return
      "Laggard("
      + agent + ","
      + isLaggard + ","
      + format.format(blackboardCompletion) + ","
      + format.format(cpuConsumption) + ")@"
      + CompletionSourcePlugin.formatDate(timestamp);
  }
}
