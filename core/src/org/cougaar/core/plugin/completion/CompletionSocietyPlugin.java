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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.SortedMap;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.servlet.ServletService;

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
  /** Length of inactive period required before time is advanced **/
  private static final long DEFAULT_NORMAL_TIME_ADVANCE_DELAY = 5000L;
  /** Length of inactive period required before time is advanced for the first time **/
  private static final long DEFAULT_INITIAL_TIME_ADVANCE_DELAY = 150000L;
  /** Length of time after time is advanced before another advance is allowed. **/
  private static final long DEFAULT_ADVANCE_TIME_ADVANCE_DELAY = 20000L;
  /** How much to advance time **/
  private static final long DEFAULT_TIME_STEP = 86400000L;
  private static final Class[] stringArgType = {String.class};


  private long NORMAL_TIME_ADVANCE_DELAY = DEFAULT_NORMAL_TIME_ADVANCE_DELAY;
  private long INITIAL_TIME_ADVANCE_DELAY = DEFAULT_INITIAL_TIME_ADVANCE_DELAY;
  private long ADVANCE_TIME_ADVANCE_DELAY = DEFAULT_ADVANCE_TIME_ADVANCE_DELAY;
  private long TIME_STEP = DEFAULT_TIME_STEP;
  private int advancementSteps = 0;
  private boolean persistenceNeeded = false;
  private static final Class[] requiredServices = {
    ServletService.class
  };

  /**
   * Subclasses should provide an array of CompletionActions to be
   * handled. Each action will be invoked whenever the laggard
   * status changes until the action returns true. After an action
   * returns true, the next action is invoked until it returns true.
   * This continues until all completion actions have returned true.
   * After that, the standard time-advance action is invoked.
   **/
  public interface CompletionAction {
    boolean checkCompletion(boolean haveLaggard);
  }

  private long timeToAdvanceTime = Long.MIN_VALUE; // Time to advance time (maybe)
  private long tomorrow = Long.MIN_VALUE;	// Demo time must
                                                // exceed this before
                                                // time step
  private CompletionAction[] completionActions = null;

  private int nextCompletionAction = 0;

  private LaggardFilter filter = new LaggardFilter();

  private ServletService servletService = null;

  public CompletionSocietyPlugin() {
    super(requiredServices);
  }

  protected boolean haveServices() {
    if (servletService != null) return true;
    if (super.haveServices()) {
      servletService = (ServletService)
        getServiceBroker().getService(this, ServletService.class, null);
      try {
        servletService.register("/completionControl", new CompletionControlServlet());
      } catch (Exception e) {
        logger.error("Failed to register completionControl servlet", e);
      }
      return true;
    }
    return false;
  }

  protected abstract CompletionAction[] getCompletionActions();

  protected Set getTargetNames() {
    return topologyReaderService.getAll(TopologyReaderService.NODE);
  }

  protected void handleNewLaggard(Laggard worstLaggard) {
    boolean haveLaggard = worstLaggard != null && worstLaggard.isLaggard();
    if (logger.isInfoEnabled() && filter.filter(worstLaggard)) {
      logger.info(getClusterIdentifier() + ": new worst " + worstLaggard);
    }
    if (completionActions == null) {
      completionActions = getCompletionActions();
    }
    if (nextCompletionAction < completionActions.length) {
      try {
        if (completionActions[nextCompletionAction].checkCompletion(haveLaggard)) {
          if (logger.isInfoEnabled()) {
            logger.info("Completed " + completionActions[nextCompletionAction]);
          }
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
    if (advancementSteps <= 0) {
      if (persistenceNeeded) {
        setPersistenceNeeded();
        persistenceNeeded = false;
      }
      return;
    }
    long demoNow = alarmService.currentTimeMillis();
    if (tomorrow == Long.MIN_VALUE) {
      tomorrow = (demoNow / TIME_STEP) * TIME_STEP; // Beginning of today
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
        long newTomorrow = ((demoNow / TIME_STEP) + 1) * TIME_STEP;
        advancementSteps -= (int) ((newTomorrow - tomorrow) / TIME_STEP);
        tomorrow = newTomorrow;
        persistenceNeeded = true;
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

  private class CompletionControlServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, false);
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, true);
    }
    protected void doPostOrGet(HttpServletRequest request, HttpServletResponse response, boolean doUpdate)
      throws IOException
    {
      PrintWriter out = response.getWriter();
      if (!"Submit".equals(request.getParameter("submit"))) {
        doUpdate = false;
      }
      response.setContentType("text/html");
      out.println("<html>\n  <head>\n    <title>Completion Control</title>\n  </head>");
      out.println("  <body>\n    <h1>Completion Control Servlet</h1>"
                  + "    <form action=\"completionControl\" method=\"post\">");
      out.println("      <table>");
      out.println("        <tr><td>Scenario Time</td><td>"
                  + formatDate(alarmService.currentTimeMillis())
                  + "</td></tr>");
      INITIAL_TIME_ADVANCE_DELAY =
        handleField("initialDelay",
                    "Initial Time Advance Delay",
                    "The duration of the quiet period required before the first time advance",
                    new Long(INITIAL_TIME_ADVANCE_DELAY),
                    request, out, doUpdate).longValue();
      NORMAL_TIME_ADVANCE_DELAY =
        handleField("normalDelay",
                    "Normal Time Advance Delay",
                    "The duration of the quiet period required before the normal time advances",
                    new Long(NORMAL_TIME_ADVANCE_DELAY),
                    request, out, doUpdate).longValue();
      ADVANCE_TIME_ADVANCE_DELAY =
        handleField("advanceDelay",
                    "Advance Time Delay",
                    "The delay after a time advance before the next time advance is allowed",
                    new Long(ADVANCE_TIME_ADVANCE_DELAY),
                    request, out, doUpdate).longValue();
      TIME_STEP =
        handleField("timeStep",
                    "Advance Time Step",
                    "The size of the time step for each advancement",
                    new Long(TIME_STEP),
                    request, out, doUpdate).longValue();
      advancementSteps =
        handleField("nSteps",
                    "Number of Time Steps to Advance",
                    "Specifies how many steps of advancement should be performed",
                    new Integer(advancementSteps),
                    request, out, doUpdate).intValue();
      out.println("      </table>");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Submit\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      out.println("    </form>\n  </body>\n</html>");
    }

    private Number handleField(String name, String label, String description, Number currentValue,
                               HttpServletRequest request, PrintWriter out, boolean doUpdate)
    {
      Number newValue = createValue(currentValue, request.getParameter(name));
      if (doUpdate && newValue != null && !newValue.equals(currentValue)) {
        currentValue = newValue;
      }
      out.println("        <tr><td>" + label + "</td><td>"
                  + "<input name=\"" + name + "\" type=\"text\" value=\""
                  + currentValue.toString() + "\"></td></tr>");
      return currentValue;
    }

    private Number createValue(Number currentValue, String v) {
      try {
        Constructor constructor = currentValue.getClass().getConstructor(stringArgType);
        Object[] args = {v};
        return (Number) constructor.newInstance(args);
      } catch (Exception e) {
        return currentValue;
      }
    }
  }
}
      
