/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.examples.component;

import org.cougaar.core.component.*;

import java.util.*;
import java.net.URL;

/** Simple callback-based Test service for Component model demo.
 * Implements a simple alarm-based callback mechanism.
 **/
public interface TestAlarmService 
  extends Service
{
  /** request that the client (who must implement the Client interface)
   * be woken after millis milliseconds have elapsed.
   **/
  void wakeAfterDelay(long millis);

  public static interface Client {
    /** called by the alarm thread to wake the client **/
    void wake();
  }
}

