/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log.log4j;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Appender;
import org.apache.log4j.Priority;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;

import org.cougaar.util.log.*;

/**
 * Package-private utility class for log4j utils.
 */
final class Util {

  private Util() {
    // just utility methods
  }

  // FIXME optimize to use "static final" arrays!!!
  // maybe alter the Logger constants to match log4j...

  /**
   * Private utility to change between int defined by Logger
   * and Priority class of log4j.
   * @param level An integer from Logger.
   */
  public static final Priority convertIntToPriority(int level) {
    switch (level) {
      case Logger.DEBUG : return Priority.toPriority(Priority.DEBUG_INT);
      case Logger.INFO  : return Priority.toPriority(Priority.INFO_INT);
      case Logger.WARN  : return Priority.toPriority(Priority.WARN_INT);
      case Logger.ERROR : return Priority.toPriority(Priority.ERROR_INT);
      case Logger.SHOUT : return ShoutPriority.SHOUT;
      case Logger.FATAL : return Priority.toPriority(Priority.FATAL_INT);
      default: 
                            return null;
    }
  }

  /**
   * Private utility to change between int defined by Logger
   * and Priority class of log4j.
   * @param level A log4j Priority
   */
  public static final int convertPriorityToInt(Priority level) {
    switch (level.toInt()) {
      case Priority.DEBUG_INT:      return Logger.DEBUG;
      case Priority.INFO_INT :      return Logger.INFO;
      case Priority.WARN_INT :      return Logger.WARN;
      case Priority.ERROR_INT:      return Logger.ERROR;
      case ShoutPriority.SHOUT_INT: return Logger.SHOUT;
      case Priority.FATAL_INT:      return Logger.FATAL;
      default: 
                               return 0;
    }
  }

  /**
   * Helper function to convert the interfaces outputType (See 
   * {@link LogTarget}) into a log4j equivilent
   * Appender class.
   *
   * @param outputType - The input type CONSOLE, FILE, or STREAM
   * from {@link LogTarget.CONSOLE LogTarget}
   * @return The log 4j Appender class corresponding to the CONSOLE
   */
  public static Class convertIntToAppenderType(int outputType) {
    switch (outputType) {
      case LogTarget.CONSOLE:
        return ConsoleAppender.class;
      case LogTarget.STREAM:
        return WriterAppender.class;
      case LogTarget.FILE:
        return FileAppender.class;
      default:
        return null;
    }
  }

  /**
   * Helper function to convert outputType and device into the 
   * actual appropriate Appender, used for adding output types.
   *
   * @param outputType - The output type either CONSOLE, FILE, or 
   * STREAM of logging output to be removed. 
   * See {@link LogTarget}.
   * @param outputDevice - The device associated with the 
   * particular APPENDER.
   */
  public static Appender convertIntToAppender(
      int outputType, 
      Object outputDevice) {
    switch (outputType) {
      case LogTarget.CONSOLE: 
        return new ConsoleAppender(new SimpleLayout());
      case LogTarget.STREAM: 
        if ((outputDevice != null) && 
            (outputDevice instanceof OutputStream)) {
          WriterAppender appender = 
            new WriterAppender(
                new SimpleLayout(), 
                (OutputStream) outputDevice);
          appender.setName(Integer.toString(outputDevice.hashCode()));
        } else {
          //Report Error
        }
        break;
      case LogTarget.FILE: 
        if ((outputDevice != null) && 
            (outputDevice instanceof String)) {
          try {
            return new FileAppender(
                new SimpleLayout(), (String) outputDevice, true);
          } catch (IOException e) {
            //Report Error
          }
        } else {
          //Report Error
        }
        break;
      default:
        //Report Error
        return null;
    }
    return null;
  }
}
