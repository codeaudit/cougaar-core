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

/** a Service for controlling COUGAAR demonstration facilities 
 * from loaded components. 
 **/

public interface DemoControlService extends Service {
  void setTime(long time);
  void setTime(long time, boolean foo);
  void setTimeRate(double rate);
  void advanceTime(long period);
  void advanceTime(long period, boolean foo);
  void advanceTime(long period, double rate);
  void advanceTime(ExecutionTimer.Change[] changes);
  double getExecutionRate();
}
