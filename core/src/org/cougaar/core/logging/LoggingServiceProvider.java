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

import java.util.Enumeration;
import java.util.Vector;
import java.util.HashSet;

import org.cougaar.core.component.*;

import org.cougaar.core.cluster.ClusterServesPlugIn;

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

/**
 * LoggingServiceProvider is a ServiceProvider which provides two 
 * services. It provides a LoggingService which allows an object to
 * log messages. It also provides a LoggingControlService which 
 * determines how those messages will be displayed and controls what
 * level of messages are displayed. The point of this is to abstract
 * away how log information is displayed when writting log statements,
 * but to also allow those who are interested in this display to 
 * have fine control over it. Logging display tools will be the 
 * primary users of LoggingControlService, but it may be that some
 * plugins will want to adjust logging levels when certain conditions
 * are detected i.e. once an error is detected you may want to to log
 * more detailed information.
 * <p>
 * LoggingServiceProvider currently uses log4j as a logging utility 
 * and was built with log4j in mind. Note that JSR47, the logging
 * utility soon to be shipped with java by Sun, is very similar in
 * structure to log4j. LoggingServiceProvider should be the only class
 * that is dependant on the lo4j jar, and switching to another logging
 * utility should consist of rewritting only this class.
 **/

public class LoggingServiceProvider implements ServiceProvider {
  private ClusterServesPlugIn cluster;

  /**
   * LoggingServiceProvider creates the ServiceProvider. This should
   * be done be the component that wishes to provide the service and
   * should be handled by the ServiceBroker. LoggingServiceProvider
   * takes in the enviroment variables and could easily use them to 
   * adjust the way logging is done. This means of control exists in
   * addition to PSP bsed contol. Currently environment variables are
   * ignored and a default logging setup is used.
   * @param env The environment variables which can be used to configure
   *            how logging is performed.
   */

  public LoggingServiceProvider(Hashtable env) {
    BasicConfigurator.configure();
  }

  /**
   * Implementation of ServiceProvider method. The requestor is used
   * for log4j category purposes and the service class can be either
   * LoggingService of LoggingControlService.
   * @param sb The ServiceBroker controlling this service
   * @param requestor The object requesting the service used to mark
   *                  the object category
   * @param serviceClass The service requested. It will be either
   *                     LoggingService or LoggingControlService.
   */

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (LoggingService.class.isAssignableFrom(serviceClass)) {
      return new LoggingServiceImpl(requestor);
    } else if (LoggingControlService.class.isAssignableFrom(serviceClass)) {
      return new LoggingControlServiceImpl();
    } else {
      return null;
    }
  }

  /**
   * Implementation of ServiceProvider abstract method. Currently does
   * nothing because no resources need to be released.
   * @param sbThe ServiceBroker controlling this service
   * @param requestor The object requesting the service used to mark
   *                  the object category
   * @param serviceClass The service requested. It will be either
   *                     LoggingService or LoggingControlService.
   * @param service The actual service being released
   */

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  /**
   * Private utility to change between int defined by LoggingService
   * and Priority class of log4j.
   * @param level An integer from LoggingService.
   */
  private Priority convertIntToPriority(int level) {
    switch (level) {
    case LoggingService.DEBUG   : return Priority.toPriority(Priority.DEBUG_INT);
    case LoggingService.INFO    : return Priority.toPriority(Priority.INFO_INT);
    case LoggingService.LEGACY  : return Priority.toPriority(Priority.INFO_INT);
    case LoggingService.WARNING : return Priority.toPriority(Priority.WARN_INT);
    case LoggingService.ERROR   : return Priority.toPriority(Priority.ERROR_INT);
    case LoggingService.FATAL   : return Priority.toPriority(Priority.FATAL_INT);
    default: 
      return null;
    }
  }
  /**
   * Private utility to change between int defined by LoggingService
   * and Priority class of log4j.
   * @param level A log4j Priority
   */
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

  /**
   * This is an log4j based implementation of LoggingService. One
   * important note is that when this ServiceImpl is created it
   * creates a log4j Category based on the class passed in. If 
   * subclasses use the same LoggingServiceImpl as the superclass
   * they will have the same log4j Category. This may possibly
   * cause confusion. To avoid this each object can get its own
   * instance of LogginServiceImpl or the class and method name
   * can be passed in.
   */
  private class LoggingServiceImpl implements LoggingService {
    Category log4jCategory;

    /** 
     * Constructor which uses the object requesting the service
     * to form a log4j Category. See notes above.
     * @param requestor Object requesting this service.
     */
    public LoggingServiceImpl(Object requestor) {
      log4jCategory = Category.getInstance(requestor.getClass());
    }

    /**
     * Records a Debug level log message.
     * @param s Debug message
     * @param e An exception that has been generated
     * @param sourceClass If you want class and method information added to 
     *                    the log message you can add the class name.
     * @param sourceMethod If you want class and method information added to 
     *                     the log message you can add the method.
     */ 
    public void debug(String s) { log4jCategory.debug(s); }
    public void debug(String s, Exception e) { log4jCategory.debug(s,e); }
    public void debug(String s, String sourceClass, String sourceMethod) { 
      if (isDebugEnabled()) 
	log4jCategory.debug(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void debug(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isDebugEnabled()) {
	if (e == null) {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.debug(s); 
	  } else {
	    log4jCategory.debug(sourceClass+"."+sourceMethod+": "+s);
	  }
	} else {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.debug(s,e);
	  } else {
	    log4jCategory.debug(sourceClass+"."+sourceMethod+": "+s,e); 
	  }
	}
      }
    }

    /**
     * Records a Info level log message.
     * @param s Info message
     * @param e An exception that has been generated
     * @param sourceClass If you want class and method information added to 
     *                    the log message you can add the class name.
     * @param sourceMethod If you want class and method information added to 
     *                     the log message you can add the method.
     */ 
    public void info(String s) { log4jCategory.info(s); }
    public void info(String s, Exception e) { log4jCategory.info(s,e); }
    public void info(String s, String sourceClass, String sourceMethod) { 
      if (isInfoEnabled()) 
	log4jCategory.info(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void info(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isInfoEnabled()) {
	if (e == null) {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.info(s); 
	  } else {
	    log4jCategory.info(sourceClass+"."+sourceMethod+": "+s);
	  }
	} else {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.info(s,e);
	  } else {
	    log4jCategory.info(sourceClass+"."+sourceMethod+": "+s,e); 
	  }
	}
      }
    }

    /**
     * Records a Warning level log message.
     * @param s Warning message
     * @param e An exception that has been generated
     * @param sourceClass If you want class and method information added to 
     *                    the log message you can add the class name.
     * @param sourceMethod If you want class and method information added to 
     *                     the log message you can add the method.
     */ 
    public void warning(String s) { log4jCategory.warn(s); }
    public void warning(String s, Exception e) { log4jCategory.warn(s,e); }
    public void warning(String s, String sourceClass, String sourceMethod) { 
      if (isWarningEnabled()) 
	log4jCategory.warn(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void warning(String s, Exception e, String sourceClass, String sourceMethod) {
      if (isWarningEnabled()) {
	if (e == null) {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.warn(s); 
	  } else {
	    log4jCategory.warn(sourceClass+"."+sourceMethod+": "+s);
	  }
	} else {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.warn(s,e);
	  } else {
	    log4jCategory.warn(sourceClass+"."+sourceMethod+": "+s,e); 
	  }
	}
      }
    }
    
    /**
     * Records a Error level log message.
     * @param s Error message
     * @param e An exception that has been generated
     * @param sourceClass If you want class and method information added to 
     *                    the log message you can add the class name.
     * @param sourceMethod If you want class and method information added to 
     *                     the log message you can add the method.
     */ 
    public void error(String s) { log4jCategory.error(s); }
    public void error(String s, Exception e) { log4jCategory.error(s,e); }
    public void error(String s, String sourceClass, String sourceMethod) { 
      log4jCategory.error(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void error(String s, Exception e, String sourceClass, String sourceMethod) {
      if (e == null) {
	if (sourceClass == null || sourceMethod == null) {
	  log4jCategory.error(s); 
	} else {
	  log4jCategory.error(sourceClass+"."+sourceMethod+": "+s);
	}
      } else {
	if (sourceClass == null || sourceMethod == null) {
	  log4jCategory.error(s,e);
	} else {
	  log4jCategory.error(sourceClass+"."+sourceMethod+": "+s,e); 
	}
      }
    }

    /**
     * Records a Fatal level log message.
     * @param s Fatal message
     * @param e An exception that has been generated
     * @param sourceClass If you want class and method information added to 
     *                    the log message you can add the class name.
     * @param sourceMethod If you want class and method information added to 
     *                     the log message you can add the method.
     */ 
    public void fatal(String s) { log4jCategory.fatal(s); }
    public void fatal(String s, Exception e) { log4jCategory.fatal(s,e); }
    public void fatal(String s, String sourceClass, String sourceMethod) { 
      log4jCategory.fatal(sourceClass+"."+sourceMethod+": "+s); 
    }
    public void fatal(String s, Exception e, String sourceClass, String sourceMethod) {
      if (e == null) {
	if (sourceClass == null || sourceMethod == null) {
	  log4jCategory.fatal(s); 
	} else {
	  log4jCategory.fatal(sourceClass+"."+sourceMethod+": "+s);
	}
      } else {
	if (sourceClass == null || sourceMethod == null) {
	  log4jCategory.fatal(s,e);
	} else {
	  log4jCategory.fatal(sourceClass+"."+sourceMethod+": "+s,e); 
	}
      }
    }
   
    /**
     * Mode query messages exist and should be used when a log message consists
     * a string that is costly to create. Surrounding log messages with these
     * queries will prevent a performance hit created by potentially complex
     * calls and extensive string creation.
     */
    public boolean isDebugEnabled() { return log4jCategory.isDebugEnabled(); }
    public boolean isInfoEnabled() { return log4jCategory.isInfoEnabled(); }
    public boolean isWarningEnabled() { return log4jCategory.isEnabledFor(Priority.WARN); }

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
      if (level > WARNING || log4jCategory.isEnabledFor(convertIntToPriority(level))) { 
	Priority p = convertIntToPriority(level);
	if (e == null) {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.log(p,s); 
	  } else {
	    log4jCategory.log(p,sourceClass+"."+sourceMethod+": "+s);
	  }
	} else {
	  if (sourceClass == null || sourceMethod == null) {
	    log4jCategory.log(p,s,e);
	  } else {
	    log4jCategory.log(p,sourceClass+"."+sourceMethod+": "+s,e); 
	  }
	}
      }
    }

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
    public void assert(boolean condition, String s) {
      if (!condition) error(s);
    }    
    public void assert(boolean condition, String s, int level) {
      if (!condition) log(level,s);
    }
    public void assert(boolean condition, String s, String sourceClass, String sourceMethod) {
      if (!condition) error(s,sourceClass,sourceMethod);
    }
    public void assert(boolean condition, String s, String sourceClass, String sourceMethod, int level) {
      if (!condition) log(level,s,sourceClass,sourceMethod);
    }

  }
  
  private class LoggingControlServiceImpl implements LoggingControlService {

    public int getLoggingLevel(String node) {
	if(node.equals("root")) {
	    return convertPriorityToInt(Category.getRoot().getChainedPriority());
	} else {
	    return convertPriorityToInt(Category.getInstance(node).getChainedPriority());
	}
    }
    public void setLoggingLevel(String node, int level) {
	if(node.equals("root")) {
	    Category.getRoot().setPriority(convertIntToPriority(level));
	}
	else {
	    Category.getInstance(node).setPriority(convertIntToPriority(level));
	}
    }

    public Enumeration getAllLoggingNodes() {
	HashSet s = new HashSet();
	Enumeration cats = Category.getCurrentCategories();
	while(cats.hasMoreElements()) {
	    Category cat = (Category) cats.nextElement();
	    while(cat != null) {
		Enumeration appenders = cat.getAllAppenders();
		if(appenders.hasMoreElements())
		    s.add(cat.getName());
		if(cat == cat.getRoot()) {
		    cat = null;
		}
		else {
		    cat = cat.getRoot();
		}
	    }		
	}	
	return (new Vector(s)).elements();
    }

      public LoggingOutputType[] getOutputTypes(String node) {
	  Enumeration appenders=null;
	  int loggingLevel;
	  if(node.equals("root")) {
	      appenders = Category.getRoot().getAllAppenders();
	      loggingLevel = convertPriorityToInt(Category.getRoot().getChainedPriority());
	  }
	  else {
	      appenders = Category.getInstance(node).getAllAppenders();
	      loggingLevel = convertPriorityToInt(Category.getInstance(node).getChainedPriority());
	  }
	  Vector outputs = new Vector();

	  int i;

	  while(appenders.hasMoreElements()){
	      outputs.addElement(
	       new LoggingProviderOutputTypeImpl(node,
						 loggingLevel,
						 (Appender) appenders.nextElement()));
	  }

	  LoggingOutputType[] lots = new LoggingOutputType[outputs.size()];

	  for(i=0; i < outputs.size() ; i++ ) {
	      lots[i] = (LoggingOutputType) outputs.elementAt(i);
	  }

	  System.out.println("LoggingServiceProvider::Node " + node + " has " + lots.length + " or " + i + " Appenders.");

	  return lots;      
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



