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

import org.apache.log4j.Category;
import org.apache.log4j.Priority;

import org.cougaar.util.log.*;

/**
 * Package-private log4j implementation of logger.
 * <p>
 * This is an log4j based implementation of Logger. One
 * important note is that when this instance is created it
 * creates a log4j Category based on the class passed in. If
 * subclasses use the same LoggerImpl as the superclass
 * they will have the same log4j Category. This may possibly
 * cause confusion. To avoid this each object can get its own
 * instance or use its own "classname:method" name.
 *
 * @see LoggerFactory
 */
class LoggerImpl implements Logger {

  // log4j category, which does the real work...
  private final Category cat;

  /**
   * Constructor which uses the specified name to form a 
   * log4j Category.
   *
   * @param requestor Object requesting this service.
   */
  public LoggerImpl(String name) {
    cat = Category.getInstance(name);
  }

  /**
   * @see Logger.isEnabledFor see interface for notes
   */
  public boolean isEnabledFor(int level) {
    if (level > WARN) {
      return true;
    } else {
      Priority p = Util.convertIntToPriority(level);
      return cat.isEnabledFor(p);
    }
  }

  //
  // "log(level, ..)" methods:
  //

  public void log(int level, String message) {
    Priority p = Util.convertIntToPriority(level);
    cat.log(p, message);
  }

  public void log(int level, String message, Throwable t) {
    Priority p = Util.convertIntToPriority(level);
    cat.log(p, message, t);
  }

  public void log(
      int level, String message, 
      String sourceClass, String sourceMethod) {
    log(level, message, null, sourceClass, sourceMethod);
  }

  public void log(
      int level, String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    Priority p = Util.convertIntToPriority(level);
    if ((level > WARN) ||
        (cat.isEnabledFor(p))) {
      if ((sourceClass == null) ||
          (sourceMethod == null)) {
        cat.log(p, message, t);
      } else {
        String tmp = sourceClass+"."+sourceMethod+": "+message;
        cat.log(p, tmp, t);
      }
    }
  }

  //
  // specific "isEnabledFor(..)" shorthand methods:
  //

  public boolean isDebugEnabled() {
    return cat.isDebugEnabled();
  }
  public boolean isInfoEnabled() {
    return cat.isInfoEnabled();
  }
  public boolean isWarnEnabled() {
    return cat.isEnabledFor(Priority.WARN);
  }
  public boolean isErrorEnabled() {
    return true;
  }
  public boolean isFatalEnabled() {
    return true;
  }

  //
  // specific "level" shorthand methods:
  //

  /**
   * Equivalent to "log(DEBUG, ..)".
   */ 
  public void debug(String message) {
    cat.debug(message);
  }
  public void debug(String message, Throwable t) {
    cat.debug(message, t);
  }
  public void debug(
      String message, 
      String sourceClass, String sourceMethod) {
    debug(message, null, sourceClass, sourceMethod);
  }
  public void debug(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    if ((sourceClass == null) ||
        (sourceMethod == null)) {
      cat.debug(message, t);
    } else if (isDebugEnabled()) {
      String tmp = sourceClass+"."+sourceMethod+": "+message;
      cat.debug(tmp, t);
    } else {
    }
  }

  /**
   * Equivalent to "log(INFO, ..)".
   */ 
  public void info(String message) {
    cat.info(message);
  }
  public void info(String message, Throwable t) {
    cat.info(message, t);
  }
  public void info(
      String message, 
      String sourceClass, String sourceMethod) {
    info(message, null, sourceClass, sourceMethod);
  }
  public void info(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    if ((sourceClass == null) ||
        (sourceMethod == null)) {
      cat.info(message, t);
    } else if (isDebugEnabled()) {
      String tmp = sourceClass+"."+sourceMethod+": "+message;
      cat.info(tmp, t);
    } else {
    }
  }

  /**
   * Equivalent to "log(WARN, ..)".
   */ 
  public void warn(String message) {
    cat.warn(message);
  }
  public void warn(String message, Throwable t) {
    cat.warn(message, t);
  }
  public void warn(
      String message, 
      String sourceClass, String sourceMethod) {
    warn(message, null, sourceClass, sourceMethod);
  }
  public void warn(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    if ((sourceClass == null) ||
        (sourceMethod == null)) {
      cat.warn(message, t);
    } else if (isDebugEnabled()) {
      String tmp = sourceClass+"."+sourceMethod+": "+message;
      cat.warn(tmp, t);
    } else {
    }
  }

  /**
   * Equivalent to "log(ERROR, ..)".
   */ 
  public void error(String message) {
    cat.error(message);
  }
  public void error(String message, Throwable t) {
    cat.error(message, t);
  }
  public void error(
      String message, 
      String sourceClass, String sourceMethod) {
    error(message, null, sourceClass, sourceMethod);
  }
  public void error(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    if ((sourceClass == null) ||
        (sourceMethod == null)) {
      cat.error(message, t);
    } else {
      String tmp = sourceClass+"."+sourceMethod+": "+message;
      cat.error(tmp, t);
    }
  }

  /**
   * Equivalent to "log(FATAL, ..)".
   */ 
  public void fatal(String message) {
    cat.fatal(message);
  }
  public void fatal(String message, Throwable t) {
    cat.fatal(message, t);
  }
  public void fatal(
      String message, 
      String sourceClass, String sourceMethod) {
    fatal(message, null, sourceClass, sourceMethod);
  }
  public void fatal(
      String message, Throwable t, 
      String sourceClass, String sourceMethod) {
    if ((sourceClass == null) ||
        (sourceMethod == null)) {
      cat.fatal(message, t);
    } else {
      String tmp = sourceClass+"."+sourceMethod+": "+message;
      cat.fatal(tmp, t);
    }
  }

  public String toString() {
    return "logger \""+cat+"\"";
  }
}
