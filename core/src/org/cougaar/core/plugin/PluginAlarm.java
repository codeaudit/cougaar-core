/*
 * Created on Dec 10, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.cougaar.core.plugin;

import java.util.Date;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.log.Logger;

/**
 * @author RTomlinson
 *
 * Extracted from ServiceUserPlugin. This class must be sub-classed to provide
 * access to a plugin's current blackboard.
 */
public abstract class PluginAlarm implements Alarm {
  private long expirationTime;
  private boolean expired = false;

  /**
   * Construct an alarm to expire
   * @param delay
   */
  public PluginAlarm(long delay) {
    expirationTime = System.currentTimeMillis() + delay;
  }

  public long getExpirationTime() {
    return expirationTime;
  }

  protected abstract BlackboardService getBlackboardService();

  public synchronized void expire() {
    if (!expired) {
      expired = true;
      BlackboardService bb = getBlackboardService();
      if (bb != null) {
        bb.signalClientActivity();
      } else {
        // No blackboard
        // Possibly a left over alarm from a plugin that has terminated.
      }
    }
  }
  public synchronized boolean hasExpired() {
    return expired;
  }
  
  public synchronized boolean cancel() {
    boolean was = expired;
    expired = true;
    return was;
  }
}
