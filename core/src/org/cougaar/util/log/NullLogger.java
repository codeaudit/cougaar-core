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
 * Logger where all "is*()" methods return false, and
 * all "log()" methods are ignored.
 *
 * @see Logger
 */
public class NullLogger implements Logger {

  private static final NullLogger SINGLETON = new NullLogger();

  public static NullLogger getInstance() {
    return SINGLETON;
  }

  private NullLogger() {
    // use "getInstance()"
  }

  // all "is*()" methods return false:

  public boolean isEnabledFor(int level) {
    return false;
  }
  public boolean isDebugEnabled() { 
    return false;
  }
  public boolean isInfoEnabled() { 
    return false;
  }
  public boolean isWarnEnabled() { 
    return false;
  }
  public boolean isErrorEnabled() { 
    return false;
  }
  public boolean isFatalEnabled() { 
    return false;
  }

  // all other methods are empty:

  public void log(int level, String message) { 
  }
  public void log(int level, String message, Throwable t) { 
  }
  public void debug(String message) { 
  }
  public void debug(String message, Throwable t) { 
  }
  public void info(String message) { 
  }
  public void info(String message, Throwable t) { 
  }
  public void warn(String message) { 
  }
  public void warn(String message, Throwable t) { 
  }
  public void error(String message) { 
  }
  public void error(String message, Throwable t) { 
  }
  public void fatal(String message) { 
  }
  public void fatal(String message, Throwable t) { 
  }

  public String toString() {
    return "null-logger";
  }
}
