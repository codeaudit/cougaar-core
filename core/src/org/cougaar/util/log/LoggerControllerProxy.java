/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log;

import java.util.Enumeration;

/**
 * This class simply wraps a LoggerController and proxies it.
 *
 * @see LoggerController
 */
public class LoggerControllerProxy implements LoggerController {

  protected LoggerController lc;

  public LoggerControllerProxy(LoggerController lc) {
    this.lc = lc;
  }

  public LoggerController getLoggerController(String name) {
    return lc.getLoggerController(name);
  }
  public Enumeration getAllLoggerNames() {
    return lc.getAllLoggerNames();
  }
  public int getLoggingLevel() {
    return lc.getLoggingLevel();
  }
  public void setLoggingLevel(int level) {
    lc.setLoggingLevel(level);
  }
  public LogTarget[] getLogTargets() {
    return lc.getLogTargets();
  }
  public void addLogTarget(
      int outputType, Object outputDevice) {
    lc.addLogTarget(outputType, outputDevice);
  }
  public void addConsole(){
    lc.addConsole();
  }
  public boolean removeLogTarget(
      int outputType, Object outputDevice) {
    return lc.removeLogTarget(outputType, outputDevice);
  }
  public boolean removeLogTarget(
      int outputType, String deviceString){
    return lc.removeLogTarget(outputType, deviceString);
  }

  public String toString() {
    return lc.toString();
  }

}
