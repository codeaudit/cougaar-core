/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.component.*;

import org.apache.log4j.Category;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.BasicConfigurator;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.OutputStream;
import java.io.IOException;

/** a Service for getting at Logging information
 **/

public class LoggingServiceProvider implements ServiceProvider {
  private ClusterServesPlugIn cluster;

  public LoggingServiceProvider(Hashtable env) {
    BasicConfigurator.configure();
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (LoggingService.class.isAssignableFrom(serviceClass)) {
      return new LoggingServiceImpl(requestor);
    } else if (LoggingControlService.class.isAssignableFrom(serviceClass)) {
      return new LoggingControlServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private Priority convertIntToPriority(int level) {
    switch (level) {
    case LoggingService.DEBUG   : return Priority.toPriority(Priority.DEBUG_INT);
    case LoggingService.INFO    : return Priority.toPriority(Priority.INFO_INT);
    case LoggingService.WARNING : return Priority.toPriority(Priority.WARN_INT);
    case LoggingService.ERROR   : return Priority.toPriority(Priority.ERROR_INT);
    case LoggingService.FATAL   : return Priority.toPriority(Priority.FATAL_INT);
    default: 
      return null;
    }
  }

  private int convertPriorityToInt(Priority level) {
    switch (level.toInt()) {
    case Priority.DEBUG_INT: return LoggingService.DEBUG;
    case Priority.INFO_INT : return LoggingService.INFO;
    case Priority.WARN_INT : return LoggingService.WARNING;
    case Priority.ERROR_INT: return LoggingService.ERROR;
    case Priority.FATAL_INT: return LoggingService.FATAL;
    default: 
      return 0;
    }
  }

  private class LoggingServiceImpl implements LoggingService {
    Category log4jCategory;

    public LoggingServiceImpl(Object requestor) {
      log4jCategory = Category.getInstance(requestor.getClass());
    }

    public void debug(String s) { log4jCategory.debug(s); }
    public void debug(String s, Exception e) { log4jCategory.debug(s,e); }
    public void debug(String s, String sourceClass, String sourceMethod) { 
      if (isDebugEnabled()) 
	log4jCategory.debug(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void debug(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isDebugEnabled()) 
	log4jCategory.debug(sourceClass+"."+sourceMethod+": "+s,e); 
    }

    public void info(String s) { log4jCategory.info(s); }
    public void info(String s, Exception e) { log4jCategory.info(s,e); }
    public void info(String s, String sourceClass, String sourceMethod) { 
      if (isInfoEnabled()) 
	log4jCategory.info(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void info(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isInfoEnabled()) 
	log4jCategory.info(sourceClass+"."+sourceMethod+": "+s,e); 
    }

    public void warning(String s) { log4jCategory.warn(s); }
    public void warning(String s, Exception e) { log4jCategory.warn(s,e); }
    public void warning(String s, String sourceClass, String sourceMethod) { 
      if (isWarningEnabled()) 
	log4jCategory.warn(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void warning(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isWarningEnabled()) 
	log4jCategory.warn(sourceClass+"."+sourceMethod+": "+s,e); 
    }

    public void error(String s) { log4jCategory.error(s); }
    public void error(String s, Exception e) { log4jCategory.error(s,e); }
    public void error(String s, String sourceClass, String sourceMethod) { 
      log4jCategory.error(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void error(String s, Exception e, String sourceClass, String sourceMethod) {
      log4jCategory.error(sourceClass+"."+sourceMethod+": "+s,e); 
    }

    public void fatal(String s) { log4jCategory.fatal(s); }
    public void fatal(String s, Exception e) { log4jCategory.fatal(s,e); }
    public void fatal(String s, String sourceClass, String sourceMethod) { 
      log4jCategory.fatal(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void fatal(String s, Exception e, String sourceClass, String sourceMethod) {
      log4jCategory.fatal(sourceClass+"."+sourceMethod+": "+s,e); 
    }
   
    /* These exist as checks you can put around logging messages to avoid costly String creations */
    public boolean isDebugEnabled() { return log4jCategory.isDebugEnabled(); }
    public boolean isInfoEnabled() { return log4jCategory.isInfoEnabled(); }
    public boolean isWarningEnabled() { return log4jCategory.isEnabledFor(Priority.WARN); }

    public void log(int level, String s) { 
      log4jCategory.log(convertIntToPriority(level),s); 
    }
    public void log(int level, String s, Exception e) { 
      log4jCategory.log(convertIntToPriority(level),s,e); 
    }
    public void log(int level, String s, String sourceClass, String sourceMethod) { 
      if (level > WARNING || log4jCategory.isEnabledFor(convertIntToPriority(level))) 
	log4jCategory.log(convertIntToPriority(level),sourceClass+"."+sourceMethod+": "+s); 
    }
    public void log(int level, String s, Exception e, String sourceClass, String sourceMethod) {
      if (level > WARNING || log4jCategory.isEnabledFor(convertIntToPriority(level))) 
	log4jCategory.log(convertIntToPriority(level),sourceClass+"."+sourceMethod+": "+s,e); 
    }

    /* if condition is true then this will be logged as an Error (default) otherwise it will be ignored*/
    public void assert(boolean condition, String s) {
      if (condition) error(s);
    }    
    public void assert(boolean condition, String s, int level) {
      if (condition) log(level,s);
    }
    public void assert(boolean condition, String s, String sourceClass, String sourceMethod) {
      if (condition) error(s,sourceClass,sourceMethod);
    }
    public void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level) {
      if (condition) log(level,s,sourceClass,sourceMethod);
    }

  }
  
  private class LoggingControlServiceImpl implements LoggingControlService {

    public int getLoggingLevel(String node) {
      return convertPriorityToInt(Category.getInstance(node).getChainedPriority());
    }
    public void setLoggingLevel(String node, int level) {
      Category.getInstance(node).setPriority(convertIntToPriority(level));
    }

    public Enumeration getAllLoggingNodes() {
	Vector v = new Vector();
	Enumeration cats = Category.getCurrentCategories();
	while(cats.hasMoreElements()) {
	    Category cat = (Category) cats.nextElement();
	    v.addElement(cat.getName());
	}	
	return v.elements();
    }

      public LoggingOutputType[] getOutputTypes(String node) {
	  Enumeration appenders = Category.getInstance(node).getAllAppenders();
	  Vector outputs = new Vector(10);

	  while(appenders.hasMoreElements()){
	      outputs.addElement(new LoggingProviderOutputTypeImpl(node,(Appender) appenders.nextElement()));
	  }

	  return (LoggingOutputType[]) outputs.toArray();	      
      }

    public void addOutputType(String node, int outputType) {
      Category.getInstance(node).addAppender(convertIntToAppender(outputType,null));
    }
    public void addOutputType(String node, int outputType, Object outputDevice) {
      Category.getInstance(node).addAppender(convertIntToAppender(outputType,outputDevice));
    }

   public boolean removeOutputType(String node, int outputType, Object outputDevice) {
       String deviceString=null;
       if(outputType == FILE){
	   deviceString = (String) outputDevice;
       }
       else if(outputType == STREAM) {
	   deviceString = Integer.toString(((OutputStream) outputDevice).hashCode());
       }
       
       return removeOutputType(node,outputType, deviceString);
   }

   public boolean removeOutputType(String node, int outputType, String outputDevice) {
	Category cat = Category.getInstance(node);
	Enumeration appenders = cat.getAllAppenders();
	Appender matchingAppender=null;

	while(appenders.hasMoreElements()){
	    Appender appender = (Appender) appenders.nextElement();
	    if(appender.getClass() == convertIntToAppenderType(outputType)) {
		if((appender instanceof ConsoleAppender) ||
		   ((appender instanceof FileAppender) && (((FileAppender)appender).getFile().equals(outputDevice)))){
		    matchingAppender = appender;
		}
		else if(appender.getName().equals(outputDevice)) {
		    matchingAppender = appender;
		}
	    }
	}
	    
	if(matchingAppender!=null) {
	    cat.removeAppender(matchingAppender);
	    return true;
	}
	else {
	    return false;
	}
	
   }
	
      private Class convertIntToAppenderType(int outputType) {
	switch (outputType) {
	case LoggingControlService.CONSOLE:
	    return ConsoleAppender.class;
	case LoggingControlService.STREAM:
	    return WriterAppender.class;
	case LoggingControlService.FILE:
	    return FileAppender.class;
	default:
	    return null;
	}
    }

    private Appender convertIntToAppender(int outputType, Object outputDevice) {
      switch (outputType) {
      case LoggingControlService.CONSOLE: return new ConsoleAppender(new SimpleLayout());
      case LoggingControlService.STREAM: 
	if (outputDevice != null && outputDevice instanceof OutputStream) {
	  WriterAppender appender = new WriterAppender(new SimpleLayout(), (OutputStream)outputDevice);
	  appender.setName(Integer.toString(outputDevice.hashCode()));
	} else {
	  //Report Error
	}
	break;
      case LoggingControlService.FILE: 
	if (outputDevice != null && outputDevice instanceof String) {
	  try {
	    return new FileAppender(new SimpleLayout(), (String)outputDevice, true);
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

}



