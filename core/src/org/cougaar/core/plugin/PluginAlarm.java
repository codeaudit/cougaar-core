/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.BlackboardService;

/**
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
