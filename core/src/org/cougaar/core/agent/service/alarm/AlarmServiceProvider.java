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

package org.cougaar.core.agent.service.alarm;

import org.cougaar.core.service.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

import org.cougaar.core.component.*;

/** a Service for getting at Alarm information
 **/

public class AlarmServiceProvider implements ServiceProvider {
  private ClusterServesPlugin cluster;

  public AlarmServiceProvider(ClusterServesPlugin cluster) {
    this.cluster = cluster;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (AlarmService.class.isAssignableFrom(serviceClass)) {
      return new AlarmServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private final class AlarmServiceImpl implements AlarmService {
    public long currentTimeMillis() {
      return cluster.currentTimeMillis();
    }
    public void addAlarm(Alarm alarm) {
      cluster.addAlarm(alarm);
    }
    public void addRealTimeAlarm(Alarm alarm) {
      cluster.addRealTimeAlarm(alarm);
    }
  }
}
