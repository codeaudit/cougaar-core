/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.logging;

import org.cougaar.core.service.*;

/** 
 * This class is to make binding of the LoggingServiceImpl
 * easier.
 * @see LoggingService
 * @see LoggingServiceProvider
 **/

public class LoggingServiceAdapter implements LoggingService {
  private LoggingService LSImpl;

  LoggingServiceAdapter(LoggingService loggingService) {
    LSImpl = loggingService; 
  }

  public void debug(String s) { 
    LSImpl.debug(s,null,null,null); }
  public void debug(String s, Exception e) { 
    LSImpl.debug(s,e,null,null); }
  public void debug(String s, String sourceClass, String sourceMethod) { 
    LSImpl.debug(s,null,sourceClass,sourceMethod); }
  public void debug(String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.debug(s,e,sourceClass,sourceMethod); }
  
  public void info(String s) { 
    LSImpl.info(s,null,null,null); }
  public void info(String s, Exception e) { 
    LSImpl.info(s,e,null,null); }
  public void info(String s, String sourceClass, String sourceMethod) { 
    LSImpl.info(s,null,sourceClass,sourceMethod); }
  public void info(String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.info(s,e,sourceClass,sourceMethod); }

  public void warning(String s) { 
    LSImpl.warning(s,null,null,null); }
  public void warning(String s, Exception e) { 
    LSImpl.warning(s,e,null,null); }
  public void warning(String s, String sourceClass, String sourceMethod) { 
    LSImpl.warning(s,null,sourceClass,sourceMethod); }
  public void warning(String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.warning(s,e,sourceClass,sourceMethod); }

  public void error(String s) { 
    LSImpl.error(s,null,null,null); }
  public void error(String s, Exception e) { 
    LSImpl.error(s,e,null,null); }
  public void error(String s, String sourceClass, String sourceMethod) { 
    LSImpl.error(s,null,sourceClass,sourceMethod); }
  public void error(String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.error(s,e,sourceClass,sourceMethod); }

  public void fatal(String s) { 
    LSImpl.fatal(s,null,null,null); }
  public void fatal(String s, Exception e) { 
    LSImpl.fatal(s,e,null,null); }
  public void fatal(String s, String sourceClass, String sourceMethod) { 
    LSImpl.fatal(s,null,sourceClass,sourceMethod); }
  public void fatal(String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.fatal(s,e,sourceClass,sourceMethod); }
   
  public boolean isDebugEnabled() { return LSImpl.isDebugEnabled(); }
  public boolean isInfoEnabled() { return LSImpl.isInfoEnabled(); }
  public boolean isWarningEnabled() { return LSImpl.isWarningEnabled(); }

  public void log(int level, String s) { 
    LSImpl.log(level,s,null,null,null); }
  public void log(int level, String s, Exception e) { 
    LSImpl.log(level,s,e,null,null); }
  public void log(int level, String s, String sourceClass, String sourceMethod) { 
    LSImpl.log(level,s,null,sourceClass,sourceMethod); }
  public void log(int level, String s, Exception e, String sourceClass, String sourceMethod) { 
    LSImpl.log(level,s,e,sourceClass,sourceMethod); }
   
  public void assert(boolean condition, String s) { 
    LSImpl.assert(condition,s,null,null,LoggingService.ERROR); }
  public void assert(boolean condition, String s, int level) {
    LSImpl.assert(condition,s,null,null,level); }
  public void assert(boolean condition, String s, String sourceClass, String sourceMethod) {
    LSImpl.assert(condition,s,sourceClass,sourceMethod,LoggingService.ERROR); }
  public void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level) {
    LSImpl.assert(condition,s,sourceClass,sourceMethod,level); }
}



