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
 * A controller for a single Logger, used to set the logging levels 
 * and output destinations (console, files, etc).
 * <p>
 * This API is a bit kludgy -- it needs to be refactored into
 * a more "readable" API, including a refactoring of 
 * LogTarget. For now I'm more concerned with the Logger 
 * API, since it will be used by all developers, than this 
 * LoggerController API.
 */
public interface LoggerController {

  /**
   * Special "static" method to get a named logger-controller.
   * <p>
   * This should be moved to a different interface...
   */
  public LoggerController getLoggerController(String name);

  /**
   * Special "static" method to get the names of all loggers.
   * <p>
   * This should be moved to a different interface...
   */
  public Enumeration getAllLoggerNames();

  /**
   * Get the logging level.
   *
   * @return a Logger level constant (DEBUG, INFO, etc)
   *
   * @see Logger
   */
  public int getLoggingLevel();

  /**
   * Set the logging level.
   *
   * @param a Logger level constant (DEBUG, INFO, etc)
   *
   * @see Logger
   */
  public void setLoggingLevel(int level);

  /**
   * Get an array of all LogTargets for this logger.
   *
   * return an array of {@link LogTarget} representing all
   * the various logging destinations.
   */
  public LogTarget[] getLogTargets();

  /**
   * Add a logging destination.
   *
   * @param outputType The LogTarget constant (CONSOLE, FILE, or STREAM).
   * @param outputDevice The device associated with the particular output
   * type being added.  Null for Console, filename for FILE, the actual
   * output stream object for STREAM.
   *
   */
  public void addLogTarget(
      int outputType, Object outputDevice);

  /**
   * Add a console output type to this logger.
   * <p>
   * This is equivalent to:<pre>
   *   addLogTarget(LogTarget.CONSOLE, null);</pre>
   */
  public void addConsole();

  /**
   * Remove a logging output type.
   *
   * @param outputType The LogTarget constant (CONSOLE, FILE, or STREAM) of
   * logging output to be removed. See constants above.
   * @param outputDevice The device associated with the particular output
   * type being removed.  Null for Console, filename for FILE, the actual
   * output stream object for STREAM.
   */
  public boolean removeLogTarget(
      int outputType, Object outputDevice);

  /**
   * Remove a logging output type.
   * 
   * This method is usually used in conjunction with 
   * {@link #getLogTargets()} to
   * iterate through list to remove items.
   *
   * @param outputType The LogTarget constant (CONSOLE, FILE, or STREAM) of
   * logging output to be removed. See constants above.
   * @param deviceString - The device associated with the particular output
   * type being removed.  Null for Console, filename for FILE,
   * the String identifier name associated with the output stream.
   */
  public boolean removeLogTarget(
      int outputType, String deviceString);

}
