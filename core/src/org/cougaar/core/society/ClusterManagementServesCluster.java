/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageTransportException;
import org.cougaar.core.society.MessageTransportServer;

/** 
 * Services provided to Clusters by ClusterManagement.
 **/

public interface ClusterManagementServesCluster {
  
  /**
   * Provide software component instantiation service to cluster.
   * Generally, calls something like Beans.instantiate(null, className)
   * to find a class and then create a new instance of it. Note that unlike
   * Beans.instantiate(), this method does not accept a classloader as
   * an argument - ClusterManagement reserves the right to completely
   * control which classloader(s) are used.
   * @param className Fully qualified name of a class or bean (serialized
   * instance).
   * @exception ClassNotFoundException Thrown when there is a problem
   * instantiating the bean.
   **/
  Object instantiateBean(String className) throws ClassNotFoundException;

  /**
   * Provide software component instantiation service to cluster.
   * Generally, calls something like Beans.instantiate(classLoader, className)
   * to find a class and then create a new instance of it.
   * @param classLoader which classLoader to use.
   * @param className Fully qualified name of a class or bean (serialized
   * instance).
   * @exception ClassNotFoundException Thrown when there is a problem
   * instantiating the bean.
   **/
  Object instantiateBean(ClassLoader classLoader, String className) throws ClassNotFoundException;
  
  /**
   *   This method is resposible for accepting any Object for logging and passing it to
   *   the LogWriter.
   *   <p><PRE>
   *   PRE CONDITION:    Log Writer created and running under its own thread
   *   POST CONDITION:   Object passed to the LogWriter Thread
   *   INVARIANCE:
   *   </PRE>
   *   @param Object The object to write to the log file
   **/
  void logEvent( Object anEvent );
    
  /**
   * Send a Message to another entity on behalf of the (calling) Cluster.
   *
   * @param message Message to send
   * @exception MessageTransportException Raised when message only when message is malformed.
   * Transmission errors are handled by ClusterManagement via other means.
   **/
  void sendMessage(Message message) throws MessageTransportException;

  /**
   * The MessageTransportServer for the Node in which this Cluster resides.
   **/
  MessageTransportServer getMessageTransportServer();

  /**
   * The name of this ClusterManager (Node).
   **/
  String getName();
}
