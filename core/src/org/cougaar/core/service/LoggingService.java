/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.service;

import org.cougaar.core.component.*;

/**
 * Logging Service is a simple logging service definition. An logging
 * service implementation can be acquired from the LoggingServiceProvider
 * which is managed by the ServiceProvider. This logging service uses
 * five basic levels of log information although others can be added. 
 * Right now there is a sixth 'legacy' level which can be used to ease
 * any transition to this service.
 */

public interface LoggingService extends Service {

  int DEBUG   = 1;
  int INFO    = 2;
  int LEGACY  = 3;
  int WARNING = 4;
  int ERROR   = 5;
  int FATAL   = 6;

  /**
   * Records a Debug level log message.
   * @param s Debug message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void debug(String s);
  void debug(String s, Exception e);
  void debug(String s, String sourceClass, String sourceMethod);
  void debug(String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * Records a Info level log message.
   * @param s Info message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void info(String s);
  void info(String s, Exception e);
  void info(String s, String sourceClass, String sourceMethod);
  void info(String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * Records a Warning level log message.
   * @param s Warning message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void warning(String s);
  void warning(String s, Exception e);
  void warning(String s, String sourceClass, String sourceMethod);
  void warning(String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * Records a Error level log message.
   * @param s Error message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void error(String s);
  void error(String s, Exception e);
  void error(String s, String sourceClass, String sourceMethod);
  void error(String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * Records a Fatal level log message.
   * @param s Fatal message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void fatal(String s);
  void fatal(String s, Exception e);
  void fatal(String s, String sourceClass, String sourceMethod);
  void fatal(String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * Mode query messages exist and should be used when a log message consists
   * a string that is costly to create. Surrounding log messages with these
   * queries will prevent a performance hit created by potentially complex
   * calls and extensive string creation.
   */
  boolean isDebugEnabled();
  boolean isInfoEnabled();
  boolean isWarningEnabled();

  /**
   * Records a log message of arbitrary priority level.
   * @param level An integer describing the log level as defined in 
   *              LoggingService
   * @param s Log message
   * @param e An exception that has been generated
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   */ 
  void log(int level, String s);
  void log(int level, String s, Exception e);
  void log(int level, String s, String sourceClass, String sourceMethod);
  void log(int level, String s, Exception e, String sourceClass, String sourceMethod);

  /**
   * These assets can be used to attach a conditional to a log message. If the 
   * conditional passed in is false a log message will be generated. Unless 
   * a specific log level is passed in it will default to an error level log
   * message. If the conditional is true no further action will be taken.
   * @param condition A boolean conditional which if false causes a log message
   *                  to be generated and does nothing if true.
   * @param s Log message
   * @param sourceClass If you want class and method information added to 
   *                    the log message you can add the class name.
   * @param sourceMethod If you want class and method information added to 
   *                     the log message you can add the method.
   * @param level An integer describing the log level as defined in 
   *              LoggingService
   */
  void assert(boolean condition, String s);
  void assert(boolean condition, String s, int level);
  void assert(boolean condition, String s, String sourceClass, String sourceMethod);
  void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level);

}




