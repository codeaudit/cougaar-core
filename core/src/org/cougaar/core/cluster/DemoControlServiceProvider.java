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

public class DemoControlServiceProvider implements ServiceProvider {
  private ClusterServesPlugIn cluster;

  public DemoControlServiceProvider(ClusterServesPlugIn cluster) {
    this.cluster = cluster;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (DemoControlService.class.isAssignableFrom(serviceClass)) {
      return new DemoControlServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private final class DemoControlServiceImpl implements DemoControlService {
    public void setTime(long time) { cluster.setTime(time); }
    public void setTime(long time, boolean foo) {cluster.setTime(time,foo);}
    public void setTimeRate(double rate) {cluster.setTimeRate(rate); }
    public void advanceTime(long period) {cluster.advanceTime(period); }
    public void advanceTime(long period, boolean foo) {cluster.advanceTime(period,foo); }
    public void advanceTime(long period, double rate) {cluster.advanceTime(period,rate); }
    public void advanceTime(ExecutionTimer.Change[] changes) {cluster.advanceTime(changes); }
    public double getExecutionRate() { return cluster.getExecutionRate(); }
  }
}
