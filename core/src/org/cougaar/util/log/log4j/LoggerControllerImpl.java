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

import java.io.OutputStream;

import java.util.Enumeration;
import java.util.Vector;
import java.util.HashSet;

import org.apache.log4j.Category;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Priority;

import org.cougaar.util.log.*;

/**
 * Package-private log4j implementation of a logger-controller.
 * <p>
 * Note that log4j uses static methods to control its logging,
 * such as "Category.*" methods.
 */
class LoggerControllerImpl implements LoggerController {

  private String name;
  private Category cat;

  public LoggerControllerImpl() {
    // must get a logger-controller!
  }

  public LoggerControllerImpl(String name) {
    this.name = name;
    if (name.equals("root")) {
      this.cat = Category.getRoot();
    } else {
      this.cat = Category.getInstance(name);
    }
  }

  /**
   * Special "static" method to get a named logger.
   * <p>
   * This should be moved to a different interface...
   */
  public LoggerController getLoggerController(String name) {
    return new LoggerControllerImpl(name);
  }

  /**
   * Special "static" method to get the names of all loggers.
   * <p>
   * This should be moved to a different interface...
   */
  public Enumeration getAllLoggerNames() {
    HashSet s = new HashSet();
    Enumeration cats = Category.getCurrentCategories();
    while (cats.hasMoreElements()) {
      Category c = (Category) cats.nextElement();
      while (c != null) {
        Enumeration appenders = c.getAllAppenders();
        if (appenders.hasMoreElements()) {
          s.add(c.getName());
        }
        Category rootC = c.getRoot();
        if (c == rootC) {
          c = null;
        } else {
          c = rootC;
        }
      }               
    }
    Vector v = new Vector(s);
    return v.elements();
  }

  /**
   * Get the logging level.
   *
   * @return a Logger level constant (DEBUG, INFO, etc)
   *
   * @see Logger
   */
  public int getLoggingLevel() {
    Priority p = cat.getChainedPriority();
    return Util.convertPriorityToInt(p);
  }

  /**
   * Set the logging level.
   *
   * @param a Logger level constant (DEBUG, INFO, etc)
   *
   * @see Logger
   */
  public void setLoggingLevel(int level) {
    Priority p = Util.convertIntToPriority(level);
    cat.setPriority(p);
  }

  /**
   * Get an array of all LogTargets for this logger.
   *
   * return an array of {@link LogTarget} representing all
   * the various logging destinations.
   */
  public LogTarget[] getLogTargets() {
    Priority p = cat.getChainedPriority();
    int loggingLevel = Util.convertPriorityToInt(p);
    Enumeration appenders = cat.getAllAppenders();

    Vector outputs = new Vector();
    while (appenders.hasMoreElements()) {
      Appender a = (Appender) appenders.nextElement();
      int outputType;
      String outputDevice;
      if (a instanceof FileAppender) {
        outputType = LogTarget.FILE;
        outputDevice = ((FileAppender) a).getFile();
      } else if(a instanceof ConsoleAppender) {
        outputType = LogTarget.CONSOLE;
        outputDevice = null;
      } else if(a.getClass() ==  WriterAppender.class) {
        outputType = LogTarget.STREAM;
        outputDevice = a.getName();
      } else {
        throw new RuntimeException("Unknown Appender type");
      }
      LogTarget lt = 
        new LogTarget(
            this.name, outputType, outputDevice, loggingLevel);
      outputs.addElement(lt);
    }

    int n = outputs.size();
    LogTarget[] lta = new LogTarget[n];
    for (int i = 0; i < n; i++ ) {
      lta[i] = (LogTarget) outputs.elementAt(i);
    }

    return lta;      
  }

  /**
   * Add a logging destination.
   *
   * @param outputType The output type either CONSOLE, FILE, or STREAM. See
   * constants above.
   * @param outputDevice The device associated with the particular output
   * type being added.  Null for Console, filename for FILE, the actual
   * output stream object for STREAM.
   *
   */
  public void addLogTarget(
      int outputType, Object outputDevice) {
    Appender a = Util.convertIntToAppender(outputType, outputDevice);
    cat.addAppender(a);
  }

  /**
   * Add a console output type to this logger.
   * <p>
   * This is equivalent to:<pre>
   *   addLogTarget(LogTarget.CONSOLE, null);</pre>
   */
  public void addConsole() {
    addLogTarget(LogTarget.CONSOLE, null);
  }

  /**
   * Remove a logging output type.
   *
   * @param outputType The output type either CONSOLE, FILE, or STREAM of
   * logging output to be removed. See constants above.
   * @param outputDevice The device associated with the particular output
   * type being removed.  Null for Console, filename for FILE, the actual
   * output stream object for STREAM.
   */
  public boolean removeLogTarget(
      int outputType, Object outputDevice) {
    String deviceString;
    if (outputType == LogTarget.FILE) {
      deviceString = (String) outputDevice;
    } else if (outputType == LogTarget.STREAM) {
      // FIXME this use of hashcode is fishy:
      int id = ((OutputStream) outputDevice).hashCode();
      deviceString = Integer.toString(id);
    } else {
      deviceString = null;
    }
    return removeLogTarget(outputType, deviceString);
  }

  /**
   * Remove a logging output type.
   * 
   * This method is usually used in conjunction with 
   * {@link LoggerController#getLogTargets()} to
   * iterate through list to remove items.
   *
   * @param outputType - The output type either CONSOLE, FILE, or STREAM of
   * logging output to be removed. See constants above.
   * @param deviceString - The device associated with the particular output
   * type being removed.  Null for Console, filename for FILE,
   * the String identifier name associated with the output stream.
   */
  public boolean removeLogTarget(
      int outputType, String deviceString) {
    Enumeration appenders = cat.getAllAppenders();

    Appender matchingAppender = null;
    Class findClass = Util.convertIntToAppenderType(outputType);
    while (appenders.hasMoreElements()) {
      Appender appender = (Appender) appenders.nextElement();
      if (appender.getClass() == findClass) {
        if ((appender instanceof ConsoleAppender) ||
            ((appender instanceof FileAppender) && 
             (((FileAppender) appender).getFile().equals(deviceString)))) {
          matchingAppender = appender;
        } else if (appender.getName().equals(deviceString)) {
          matchingAppender = appender;
        }
      }
    }

    if (matchingAppender != null) {
      cat.removeAppender(matchingAppender);
      return true;
    } else {
      return false;
    }
  }
}
