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

import org.cougaar.core.service.*;

import org.cougaar.core.component.*;
import java.util.Enumeration;

/** a Service for controlling logging
 ** This service provides the basic facilites to retrieve,
 ** and set the control aspects of logging.   Get all the 
 ** nodes with output types, get the output types for a given
 ** node, add an output file at a particular node, 
 ** or get a logging level.   They all
 ** enable one to query and control the output of the logger:
 ** where its directed to, how much logging is done etc.
 ** The concept of a node is any level in the heirarchy
 ** where there is an output.   The top level is "root"
 ** obviously below that is org.cougaar... etc.
 **  
 **/

public interface LoggingControlService extends Service {

  int CONSOLE = 1;
  int STREAM  = 2;
  int FILE    = 3;

    /**
     ** For a given Node, what output logging level is the node set to.
     ** @param node - string name of the place in the heirarchy the 
     ** user wishes to query about what the logging level is set to.
     ** @return The level number corresponding to DEBUG,INFO,WARNING,ERROR,
     ** FATAL
     ** @see LoggingService
     ** 
     **/
  public int getLoggingLevel(String node);

    /**
     ** Set the logging level for a given node - a node does not necessarily
     ** have to have an output type, but it makes more sense.
     ** @param node - String name of place in heirarchy to set the logging 
     ** level of.
     ** @param level - The level number corresponding to DEBUG,INFO,
     ** WARNING,ERROR, and FATAL @see LoggingService
     **
     **/
  public void setLoggingLevel(String node, int level);
  //public void setLoggingLevel(String node, int level, boolean recursiveSet);

    /**
     **
     ** Get all the logging nodes in the heirarchy that have some form
     ** of logging output on them.
     **
     ** @return Enumeration of all the logging nodes (Strings).
     **/
  public Enumeration getAllLoggingNodes();

    /**
     ** For a given node in the heirarcy get all the logging output types 
     ** for that node.  You could have both a console, and two logging files
     ** a given node for example.
     **
     ** return an array of {@link LoggingOutputType} representing all 
     ** the various logging outputs at this node.
     **/
  public LoggingOutputType[] getOutputTypes(String node);


    /**
     ** Add a logging output type at a particular node in the heirarchy
     **
     ** @param node - the node in the heirarchy to attach this output type.
     ** @param outputType - The output type either CONSOLE,FILE, or STREAM. See
     ** constants above.
     ** @param outputDevice - The device associated with the particular output 
     ** type being added.  Null for Console, filename for FILE, the actual 
     ** output stream object for STREAM.
     **
     **/
  public void addOutputType(String node, int outputType, Object outputDevice);

    /**
     ** Add a logging console output type at a particular node in the heirarchy
     **
     ** @param node - the node in the heirarchy to attach this console logging output.
     **/
  public void addConsole(String node);

    /**
     ** Remove a logging output type known at a particular node 
     ** in the heirarchy
     **
     ** @param node - the node in the heirarchy this existing output type 
     ** should be removed from.
     ** @param outputType - The output type either CONSOLE,FILE, or STREAM of
     ** logging output to be removed. See constants above.
     ** @param outputDevice - The device associated with the particular output 
     ** type being removed.  Null for Console, filename for FILE, the actual 
     ** output stream object for STREAM.
     **
     **/
  public boolean removeOutputType(String node, int outputType, Object outputDevice);

    /**
     ** Remove a logging output type known at a particular node 
     ** in the heirarchy.  Method is ususually used in conjunction
     ** with {@link LoggingControlService#getOutputTypes()} 
     ** to iterate through list to remove items.
     **
     ** @param node - the node in the heirarchy this existing output type 
     ** should be removed from.
     ** @param outputType - The output type either CONSOLE,FILE, or STREAM of
     ** logging output to be removed. See constants above.
     ** @param outputDevice - The device associated with the particular output 
     ** type being removed.  Null for Console, filename for FILE, 
     ** the String identifier name associated with the output stream. 
     **
     **/
  public boolean removeOutputType(String node, int outputType, String outputDevice);

}






