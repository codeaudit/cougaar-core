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

import java.util.Set;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.TopologyReaderService;

/**
 * This plugin gathers and integrates completion information from
 * nodes in a society to determine the "completion" of the current
 * tasks. This plugin should be included only in one agent at the root
 * of the society such as NCA. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public abstract class CompletionSocietyPlugin extends CompletionSourcePlugin {
  private static final long NORMAL_TIME_ADVANCE_DELAY = 5000L;
  private static final long ADVANCE_TIME_ADVANCE_DELAY = 20000L;
  private static final long INITIAL_TIME_ADVANCE_DELAY = 240000L;
  private static final long TIME_STEP = 86400000L;

  /**
   * Subclasses should provide an array of CompletionActions to be
   * handled. Each action will be invoked whenever the laggard
   * status changes until the action returns true. After an action
   * returns true, the next action is invoked until it returns true.
   * This continues until all completion actions have returned true.
   * After that, the standard time-advance action is invoked.
   **/
  public interface CompletionAction {
    public boolean checkCompletion(boolean haveLaggard);
  }

  private long timeToAdvanceTime = Long.MIN_VALUE; // Time to advance time (maybe)
  private long tomorrow = Long.MIN_VALUE;	// Demo time must
                                                // exceed this before
                                                // time step
  private CompletionAction[] completionActions;
  private int nextCompletionAction = 0;

  private LaggardFilter filter = new LaggardFilter();

  protected CompletionAction[] getCompletionActions() {
    return new CompletionAction[0];
  }

  protected Set getTargetNames() {
    return topologyReaderService.getAll(TopologyReaderService.NODE);
  }

  protected void handleNewLaggard(Laggard worstLaggard) {
    boolean haveLaggard = worstLaggard != null && worstLaggard.isLaggard();
    if (logger.isInfoEnabled() && filter.filter(worstLaggard)) {
      logger.info(getClusterIdentifier() + ": new worst " + worstLaggard);
    }
    if (nextCompletionAction < completionActions.length) {
      try {
        if (completionActions[nextCompletionAction].checkCompletion(haveLaggard)) {
          nextCompletionAction++;
        }
      } catch (Exception e) {
        logger.error("Error executing completion action "
                     + completionActions[nextCompletionAction],
                     e);
        nextCompletionAction++;
      }
    } else {
      checkTimeAdvance(haveLaggard);
    }
  }

  /**
   * Check if conditions are right for advancing time. There must have
   * been no laggards for TIME_ADVANCE_DELAY milliseconds. Anytime we
   * have a laggard, the clock starts over. The clock is also reset
   * after advancing time to avoid advancing a second time before the
   * agents have had an opportunity to become laggards due to the
   * first time advance.
   **/
  protected void checkTimeAdvance(boolean haveLaggard) {
    long demoNow = alarmService.currentTimeMillis();
    if (tomorrow == Long.MIN_VALUE) {
      tomorrow = demoNow;
      timeToAdvanceTime = now + INITIAL_TIME_ADVANCE_DELAY;
    }

    if (haveLaggard) {
      timeToAdvanceTime = Math.max(timeToAdvanceTime, now + NORMAL_TIME_ADVANCE_DELAY);
    } else {
      long timeToGo = Math.max(timeToAdvanceTime - now,
                               tomorrow + NORMAL_TIME_ADVANCE_DELAY - demoNow);
      if (timeToGo > 0) {
        if (logger.isDebugEnabled()) logger.debug(timeToGo + " left");
      } else {
        tomorrow = ((demoNow / TIME_STEP) + 1) * TIME_STEP;
        try {
          demoControlService.setTime(tomorrow, true);
          if (logger.isInfoEnabled()) {
            logger.info("Advance time from "
                        + formatDate(demoNow)
                        + " to "
                        + formatDate(tomorrow));
          }
        } catch (RuntimeException re) {
          System.err.println(formatDate(now)
                             + ": Exception("
                             + re.getMessage()
                             + ") trying to advance time from "
                             + formatDate(demoNow)
                             + " to "
                             + formatDate(tomorrow));
        }
        timeToAdvanceTime = now + ADVANCE_TIME_ADVANCE_DELAY;
      }
    }
  }
}
      
