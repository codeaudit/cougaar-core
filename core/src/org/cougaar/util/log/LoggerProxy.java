/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log;

/** 
 * This class simply wraps a Logger and proxies it.
 * <p>
 * This can be used to filter out Logger requests.
 *
 * @see Logger
 */
public class LoggerProxy implements Logger {

  protected Logger l;

  public LoggerProxy(Logger l) {
    this.l = l; 
  }

  public boolean isEnabledFor(int level) {
    return l.isEnabledFor(level); 
  }

  public void log(
      int level, String message) { 
    l.log(level, message);
  }
  public void log(
      int level, String message, Throwable t) { 
    l.log(level, message, t);
  }
  public void log(
      int level, String message, 
      String sourceClass, String sourceMethod) { 
    l.log(level, message, sourceClass, sourceMethod); 
  }
  public void log(
      int level, String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.log(level, message, t, sourceClass, sourceMethod); 
  }

  public boolean isDebugEnabled() { 
    return l.isDebugEnabled(); 
  }
  public boolean isInfoEnabled() { 
    return l.isInfoEnabled(); 
  }
  public boolean isWarnEnabled() { 
    return l.isWarnEnabled(); 
  }
  public boolean isErrorEnabled() { 
    return l.isErrorEnabled(); 
  }
  public boolean isFatalEnabled() { 
    return l.isFatalEnabled(); 
  }

  public void debug(
      String message) { 
    l.debug(message);
  }
  public void debug(
      String message, Throwable t) { 
    l.debug(message, t);
  }
  public void debug(
      String message, 
      String sourceClass, String sourceMethod) { 
    l.debug(message, sourceClass, sourceMethod); 
  }
  public void debug(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.debug(message, t, sourceClass, sourceMethod); 
  }

  public void info(
      String message) { 
    l.info(message);
  }
  public void info(
      String message, Throwable t) { 
    l.info(message, t);
  }
  public void info(
      String message, 
      String sourceClass, String sourceMethod) { 
    l.info(message, sourceClass, sourceMethod); 
  }
  public void info(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.info(message, t, sourceClass, sourceMethod); 
  }

  public void warn(
      String message) { 
    l.warn(message);
  }
  public void warn(
      String message, Throwable t) { 
    l.warn(message, t);
  }
  public void warn(
      String message, 
      String sourceClass, String sourceMethod) { 
    l.warn(message, sourceClass, sourceMethod); 
  }
  public void warn(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.warn(message, t, sourceClass, sourceMethod); 
  }

  public void error(
      String message) { 
    l.error(message);
  }
  public void error(
      String message, Throwable t) { 
    l.error(message, t);
  }
  public void error(
      String message, 
      String sourceClass, String sourceMethod) { 
    l.error(message, sourceClass, sourceMethod); 
  }
  public void error(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.error(message, t, sourceClass, sourceMethod); 
  }

  public void fatal(
      String message) { 
    l.fatal(message);
  }
  public void fatal(
      String message, Throwable t) { 
    l.fatal(message, t);
  }
  public void fatal(
      String message, 
      String sourceClass, String sourceMethod) { 
    l.fatal(message, sourceClass, sourceMethod); 
  }
  public void fatal(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) { 
    l.fatal(message, t, sourceClass, sourceMethod); 
  }

  public String toString() {
    return l.toString();
  }
}
