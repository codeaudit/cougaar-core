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

/** a logging Service
 **/

public interface LoggingService extends Service {

  public static final int DEBUG   = 1;
  public static final int INFO    = 2;
  public static final int WARNING = 3;
  public static final int ERROR   = 4;
  public static final int FATAL   = 5;

  public void debug(String s);
  public void debug(String s, Exception e);
  public void debug(String s, String sourceClass, String sourceMethod);
  public void debug(String s, Exception e, String sourceClass, String sourceMethod);

  public void info(String s);
  public void info(String s, Exception e);
  public void info(String s, String sourceClass, String sourceMethod);
  public void info(String s, Exception e, String sourceClass, String sourceMethod);

  public void warning(String s);
  public void warning(String s, Exception e);
  public void warning(String s, String sourceClass, String sourceMethod);
  public void warning(String s, Exception e, String sourceClass, String sourceMethod);

  public void error(String s);
  public void error(String s, Exception e);
  public void error(String s, String sourceClass, String sourceMethod);
  public void error(String s, Exception e, String sourceClass, String sourceMethod);

  public void fatal(String s);
  public void fatal(String s, Exception e);
  public void fatal(String s, String sourceClass, String sourceMethod);
  public void fatal(String s, Exception e, String sourceClass, String sourceMethod);

  /* These exist as checks you can put around logging messages to avoid costly String creations */
  public boolean isDebugEnabled();
  public boolean isInfoEnabled();
  public boolean isWarningEnabled();

  public void log(int level, String s);
  public void log(int level, String s, Exception e);
  public void log(int level, String s, String sourceClass, String sourceMethod);
  public void log(int level, String s, Exception e, String sourceClass, String sourceMethod);

  /* if condition is true then this will be logged as an Error (default) otherwise it will be ignored*/
  public void assert(boolean condition, String s);
  public void assert(boolean condition, String s, int level);
  public void assert(boolean condition, String s, String sourceClass, String sourceMethod);
  public void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level);

}




