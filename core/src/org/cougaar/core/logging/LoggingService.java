/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.logging;

import org.cougaar.core.component.*;

/** a logging Service
 **/

public interface LoggingService extends Service {

  int DEBUG   = 1;
  int INFO    = 2;
  int LEGACY  = 3;
  int WARNING = 4;
  int ERROR   = 5;
  int FATAL   = 6;

  void debug(String s);
  void debug(String s, Exception e);
  void debug(String s, String sourceClass, String sourceMethod);
  void debug(String s, Exception e, String sourceClass, String sourceMethod);

  void info(String s);
  void info(String s, Exception e);
  void info(String s, String sourceClass, String sourceMethod);
  void info(String s, Exception e, String sourceClass, String sourceMethod);

  void warning(String s);
  void warning(String s, Exception e);
  void warning(String s, String sourceClass, String sourceMethod);
  void warning(String s, Exception e, String sourceClass, String sourceMethod);

  void error(String s);
  void error(String s, Exception e);
  void error(String s, String sourceClass, String sourceMethod);
  void error(String s, Exception e, String sourceClass, String sourceMethod);

  void fatal(String s);
  void fatal(String s, Exception e);
  void fatal(String s, String sourceClass, String sourceMethod);
  void fatal(String s, Exception e, String sourceClass, String sourceMethod);

  /* These exist as checks you can put around logging messages to avoid costly String creations */
  boolean isDebugEnabled();
  boolean isInfoEnabled();
  boolean isWarningEnabled();

  void log(int level, String s);
  void log(int level, String s, Exception e);
  void log(int level, String s, String sourceClass, String sourceMethod);
  void log(int level, String s, Exception e, String sourceClass, String sourceMethod);

  /* if condition is true then this will be logged as an Error (default) otherwise it will be ignored*/
  void assert(boolean condition, String s);
  void assert(boolean condition, String s, int level);
  void assert(boolean condition, String s, String sourceClass, String sourceMethod);
  void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level);

}




