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

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.FileAppender;
import org.cougaar.core.component.*;
import java.util.Enumeration;

/** a Service for controlling logging
 **/

public class LoggingProviderOutputTypeImpl implements LoggingOutputType {

    private String nodeName;
    private int    outputType;
    private String outputDevice;

    public LoggingProviderOutputTypeImpl(String aNodeName,
					 int    anOutputType,
					 String anOutputDevice) {
      nodeName = aNodeName;
      outputType = anOutputType;
      outputDevice = anOutputDevice;
    }
    
    public LoggingProviderOutputTypeImpl(String aNodeName,
					 Appender appender) {
	this(aNodeName,0,null);
	
	if(appender instanceof FileAppender) {
	    outputType = FILE;
	    outputDevice = ((FileAppender) appender).getFile();
	}
	else if(appender instanceof ConsoleAppender) {
	    outputType = CONSOLE;
	    outputDevice = null;
	}
	else if(appender.getClass() ==  WriterAppender.class) {
	    outputType = STREAM;
	    outputDevice = appender.getName();
	}
	else {
	    throw new RuntimeException("Unknown Appender type");
	}
    }
    
    public String getNode() {return nodeName;}
    public int getOutputType() {return outputType;}
    public String getOutputDevice() {return outputDevice;}
    
}




