/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
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

public interface AlarmService extends Service {
  long currentTimeMillis();
  void addAlarm(Alarm alarm);
  void addRealTimeAlarm(Alarm alarm);
}
