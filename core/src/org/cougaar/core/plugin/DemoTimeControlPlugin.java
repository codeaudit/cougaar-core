/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.plugin;

import java.text.*;
import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.util.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.agent.service.alarm.*;

/**
 * Servlet plugin that implements a simple controller for setting the
 * demo (aka "execution") time for the local node.
 * <p>
 * One plugin paramter if present and Boolean <tt>true</tt> will cause the plugin to 
 * print the system and demo time every 10 secs (as measured by system and demo time)
 * <p>
 * @see org.cougaar.core.service.DemoControlService
 */
public class DemoTimeControlPlugin extends ComponentPlugin {

  private boolean debug = false;

  long lastScenarioTime = 0;
  long lastRealTime = 0;

  ServletService servletService;
  DemoControlService demoControlService;
  LoggingService loggingService;

  DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private String printTime(String pfx) {
    long scenarioNow = alarmService.currentTimeMillis();
    long realNow = System.currentTimeMillis();
    long scenarioDelta = scenarioNow - lastScenarioTime;
    long realDelta = realNow - lastRealTime;

    String ret =
      "\n"
        + pfx
        + "Time at "
        + this.getAgentIdentifier().getAddress()
        + ".  Scenario Time is "
        + scenarioNow
        + "["
        + scenarioDelta
        + "]"
        + "{"
        + fmt.format(new Date(scenarioNow))
        + "}"
        + " Real Time is "
        + realNow
        + "["
        + realDelta
        + "]"
        + "{"
        + fmt.format(new Date(realNow))
        + "}"
        + "\nOffset = "
        + (scenarioNow - realNow);

    System.err.println(ret);
    lastScenarioTime = scenarioNow;
    lastRealTime = realNow;
    return ret;
  }

  private class MyAlarm implements Alarm {
    long exp;
    boolean realTime;
    public MyAlarm(long delta, boolean realTime) {
      if (realTime)
        this.exp = System.currentTimeMillis() + delta;
      else
        this.exp = alarmService.currentTimeMillis() + delta;

      this.realTime = realTime;
    }
    public long getExpirationTime() {
      return exp;
    }
    public void expire() {
      printTime(realTime ? "Real " : "Demo ");
      if (realTime)
        alarmService.addRealTimeAlarm(new MyAlarm(10000, true));
      else
        alarmService.addAlarm(new MyAlarm(10000, false));

    }
    public String toString() {
      return "<" + exp + ">";
    }
    public boolean cancel() {
      return false;
    }
    /**
     * @see org.cougaar.core.agent.service.alarm.Alarm#hasExpired()
     */
    public boolean hasExpired() {
      return false;
    }

  }

  /**
   * @see org.cougaar.core.blackboard.BlackboardClientComponent#setupSubscriptions()
   */
  protected void setupSubscriptions() {
    Collection params = this.getParameters();
    if (params.contains("true"))
      debug = true;
    if (debug) {
      System.err.println(
        "timeControl plugin loaded at scenario time:" + alarmService.currentTimeMillis());
      printTime("setup");
    }

    try {
      servletService.register("/timeControl", new MyServlet());
    } catch (Exception ex) {
      System.err.println("Unable to register timeControl servlet");
      ex.printStackTrace();
    }

  }

  /**
   * @see org.cougaar.core.blackboard.BlackboardClientComponent#execute()
   */
  protected void execute() {
    if (debug) {
      printTime("execute");
      alarmService.addRealTimeAlarm(new MyAlarm(10000, true));
      alarmService.addRealTimeAlarm(new MyAlarm(10000, false));
    }
  }

  protected void updateTime(String advance, String rate) {
    if (loggingService.isDebugEnabled()) {
      loggingService.debug("Time to advance = "+advance+" New Rate = "+rate);
    }
    if ((advance == null) && (rate == null))
      return;

    long l_advance = 0;
    if (advance != null) {
      try {
        l_advance = Long.parseLong(advance);
      } catch (NumberFormatException nfe) {
        System.err.println("Bad advance");
        nfe.printStackTrace();
      }
    }
    double d_rate = 1.0;
    if (rate != null) {
      try {
        d_rate = Double.parseDouble(rate);
      } catch (NumberFormatException nfe) {
        System.err.println("Bad rate");
        nfe.printStackTrace();
      }
    }

    long newTime = alarmService.currentTimeMillis() + l_advance;
    long quantization = 1L;
    if (l_advance >= 86400000L) {
      // Quantize to nearest day if step is a multiple of one day
      if ((l_advance % 86400000L) == 0L) {
        quantization = 86400000L;
      }
    } else if ((l_advance % 3600000L) == 0L) {
      // Quantize to nearest hour if step is a multiple of one hour
      quantization = 3600000;
    }
    newTime = (newTime / quantization) * quantization;

    demoControlService.setNodeTime(newTime, d_rate);
  }

  protected void doit(HttpServletRequest req, HttpServletResponse res)
    throws IOException {

    updateTime(
      req.getParameter("timeAdvance"),
      req.getParameter("executionRate"));

    PrintWriter out = res.getWriter();
    out.println("<html><head></head><body>");

    out.println("<FORM METHOD=\"GET\" ACTION=\"timeControl\">");
    out.println("<table>");
    out.println(
      "<tr><td>Scenario Time</td><td>"
        + fmt.format(new Date(alarmService.currentTimeMillis()))
        + "</td></tr>");
    out.println(
      "<tr><td>Time Advance (millisecs)</td><td><input type=\"text\" name=\"timeAdvance\" size=10 value=\""
        + 0
        + "\"> <i>1 Day = 86400000</i></td></tr>");
    out.println(
      "<tr><td>Execution Rate</td><td><input type=\"text\" name=\"executionRate\" size=10 value=\""
        + demoControlService.getExecutionRate()
        + "\"> <i>(required)</i></td></tr>");
    out.println("</table>");
    out.println("<INPUT TYPE=\"submit\" Value=\"Submit\">");
    out.println("</FORM>");
    out.println("</body>");
    //    demoControlService.advanceTime(0, 2.0);
  }

  private class MyServlet extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
      doit(req, res);
    }
    public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
      doit(req, res);
    }
  }

  /**
   * Returns the servletService.
   * @return ServletService
   */
  public ServletService getServletService() {
    return servletService;
  }

  /**
   * Sets the servletService.
   * @param servletService The servletService to set
   */
  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  /**
   * Returns the demoControlService.
   * @return DemoControlService
   */
  public DemoControlService getDemoControlService() {
    return demoControlService;
  }

  /**
   * Sets the demoControlService.
   * @param demoControlService The demoControlService to set
   */
  public void setDemoControlService(DemoControlService demoControlService) {
    this.demoControlService = demoControlService;
  }

  /**
   * Returns the loggingService.
   * @return LoggingService
   */
  public LoggingService getLoggingService() {
    return loggingService;
  }

  /**
   * Sets the loggingService.
   * @param loggingService The loggingService to set
   */
  public void setLoggingService(LoggingService loggingService) {
    this.loggingService = loggingService;
  }

}
