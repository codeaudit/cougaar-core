/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.component.*;

/** a Service for getting at Alarm information
 **/

public class AlarmServiceProvider implements ServiceProvider {
  private ClusterServesPlugIn cluster;

  public AlarmServiceProvider(ClusterServesPlugIn cluster) {
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
