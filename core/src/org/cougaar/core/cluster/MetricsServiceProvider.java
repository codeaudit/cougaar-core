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

/** a Service for getting at Metrics information
 **/

public class MetricsServiceProvider implements ServiceProvider {
  private ClusterServesPlugIn cluster;

  public MetricsServiceProvider(ClusterServesPlugIn cluster) {
    this.cluster = cluster;
  }

  public Object getService(Services services, Object requestor, Class serviceClass) {
    if (MetricsService.class.isAssignableFrom(serviceClass)) {
      return new MetricsServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(Services services, Object requestor, Class serviceClass, Object service) {
  }

  private final class MetricsServiceImpl implements MetricsService {
    public MetricsSnapshot getMetricsSnapshot() {
      return cluster.getMetricsSnapshot();
    }
  }
}
