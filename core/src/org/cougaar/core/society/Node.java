/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.qos.monitor.QosMonitorService;
import org.cougaar.core.qos.monitor.QosMonitorServiceProvider;
import org.cougaar.core.qos.monitor.ResourceMonitorService;

import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportException;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.mts.MessageStatisticsService;
import org.cougaar.core.mts.MessageTransportServiceProvider;
import org.cougaar.core.mts.MessageWatcherService;
import org.cougaar.core.mts.AgentStatusService;

import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.naming.NamingService;

import org.cougaar.core.logging.LoggingService;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;


import org.cougaar.core.component.*;

import java.io.Serializable;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Field;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.security.*;
import java.security.cert.*;
import javax.naming.NamingException;

import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.cluster.ClusterInitializedMessage;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.agent.*;
import org.cougaar.util.*;

import org.cougaar.core.component.*;
import java.beans.Beans;

/**
 * This class is responsible for creating and maintaining a Node in the alp
 * system.  A Node refers to the actual Physical box or machine that this
 * contains the instatiated object of this class
 * <p>
 * This class is responsible to:
 * <ul>
 * <li>Manage the resources of the computer</li>
 * <li>Provide the message handling for all NodeMessages</li>
 * <li>Provide the highest level of Log file for the latest run</li>
 * <li>Provide validation services to ensure all access request are valid 
 *     before processing them.</li>
 * </ul>
 * <p><pre>
 * Usage:
 *    <tt>java [props] org.cougaar.core.society.Node [props] [-help]</tt>
 * where the "props" are "-D" System Properties, such as:
 *    "-Dorg.cougaar.node.name=NAME" -- name of the Node
 * See the documentation below for Node-specific properties, and 
 * the external documentation for a list of all COUGAAR properties.
 * </pre>
 *
 * <pre>
 * @property org.cougaar.node.name
 *   required name for this Node
 * @property org.cougaar.useBootstrapper
 *   if true, will launch using the Bootstrapper to find jar files.
 *   Defaults to true
 * @property org.cougaar.filename
 *   file name (.ini) for starting this Node, which defaults to 
 *   ("org.cougaar.node.name"+".ini") if both "org.cougaar.filename"
 *   and "org.cougaar.experiment.id" are not specified.  If this property 
 *   is specified then "org.cougaar.experiment.id" must not be specified
 * @property org.cougaar.experiment.id
 *   experiment identifier for running this Node; see 
 *   "org.cougaar.filename" for details
 * @property org.cougaar.install.path
 *   "base" path for finding jar and configuration files
 * @property org.cougaar.validate.jars
 *   if true, will check the certificates on the 
 *   ("org.cougaar.install.path"+"/plugin/*.jar") files.  Defaults to 
 *   false
 * @property org.cougaar.security.certificate
 *   path off of the "org.cougaar.install.path" for finding the 
 *   "org.cougaar.validate.jars" certificates
 * @property org.cougaar.control.host
 *   host address for the optional external (RMI) controller of this 
 *   Node; requires "org.cougaar.control.port".  Defaults to no 
 *   external control
 * @property org.cougaar.control.port
 *   port address for the "org.cougaar.control.host"; requires 
 *   "org.cougaar.control.host".  Defaults to no external control
 *
 * @property org.cougaar.config
 *   Only used by Node to transfer a command-line "-config X" to
 *   the corresponding "org.cougaar.config=X" property
 * @property org.cougaar.config.server
 *   Only used by Node to transfer a command-line "-cs X" to
 *   the corresponding "org.cougaar.config.server=X" property
 * @property org.cougaar.name.server
 *   Only used by Node to transfer a command-line "-ns X" to
 *   the corresponding "org.cougaar.name.server=X" property
 * @property org.cougaar.name.server.port
 *   Only used by Node to transfer a command-line "-port X" to
 *   the corresponding "org.cougaar.name.server.port=X" property
 * </pre>
 */
public class Node extends ContainerSupport
implements MessageTransportClient, ClusterManagementServesCluster, ContainerAPI, ServiceRevokedListener
{
  private NodeIdentifier myNodeIdentity_ = null;

  public String getIdentifier() {
    return 
      ((myNodeIdentity_ != null) ? 
       myNodeIdentity_.getAddress() :
       null);
  }

  public NodeIdentifier getNodeIdentifier() {
    return myNodeIdentity_;
  }

  public void setNodeIdentifier(NodeIdentifier aNodeIdentifier) {
    if (myNodeIdentity_ != null) {
      throw new RuntimeException(
          "Attempt to over-ride NodeIdentity detected.");
    }
    myNodeIdentity_ = aNodeIdentifier;
  }

  public String toString() {
    return "<Node "+getIdentifier()+">";
  }

  /**
   * Node entry point.
   * If org.cougaar.useBootstrapper is true, will search for installed jar files
   * in order to load all classes.  Otherwise, will rely solely on classpath.
   *
   * @see #launch(String[])
   **/
  static public void main(String[] args){
    if ("true".equals(System.getProperty("org.cougaar.useBootstrapper", "true"))) {
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  

  static public void launch(String[] args) {
    // display the version info
    printVersion();

    // convert any command-line args to System Properties
    setSystemProperties(args);

    // check for valid plugin jars
    String validateJars = System.getProperty("org.cougaar.validate.jars");
    if ((validateJars != null) &&
        ((validateJars.equalsIgnoreCase("true")) ||
         (validateJars.equals("")))) {
      // validate
      if (validatePluginJarsByStream()) {
        // validation succeeded
      } else {
        throw new RuntimeException(
          "Error!  Found unsigned jars in plugin directory!");
      }
    } else {
      // not validating
    }

    // try block to ensure we catch all exceptions and exit gracefully
    try{
      Node myNode = new Node();
      myNode.initNode();
      // done with our job... quietly finish.
    } catch (Throwable e) {
      System.out.println(
          "Caught an exception at the highest try block.  Exception is: " +
          e );
      e.printStackTrace();
    }
  }

  /**
   * Convert any command-line args to System Properties.
   * <p>
   * System properties are preferred, since it simplifies the
   * configuration to just a non-ordered Set of "-D" properties.  
   * <p>
   * Also supported are post-classname "-D" command-line properties:
   *    "java .. classname -Darg .." 
   * which will override the usual "java -Darg .. classname .."
   * properties.  For example, "java -Dx=y classname -Dx=z" is
   * equivalent to "java -Dx=z classname".  This can be useful when 
   * writing scripts that simply append properties to a command-line.
   * <p>
   * Long-term all non-"-D" arguments (<code>ArgTable</code>, "-n name", 
   * etc) will likely become deprecated, except maybe "-help".
   *
   * @see ArgTable
   */
  private static void setSystemProperties(String[] args) {
    java.util.Properties props = System.getProperties();
    List revisedArgs = new ArrayList();

    // separate the args into "-D" properties and normal arguments
    for (int i = 0; i < args.length; i++) {
      String argi = args[i];
      if (argi.startsWith("-D")) {
        // transfer a "late" system property
        int sepIdx = argi.indexOf('=');
        if (sepIdx < 0) {
          props.put(argi.substring(2), "");
        } else {
          props.put(argi.substring(2, sepIdx), argi.substring(sepIdx+1));
        }
      } else {
        // keep a non "-D" argument
        revisedArgs.add(argi);
      }
    }

    // parse the remaining command line arguments
    ArgTable myArgs = new ArgTable(revisedArgs);

    // transfer the command-line arguments to system properties

    String validateJars = 
      (String) myArgs.get(ArgTableIfc.SIGNED_PLUGIN_JARS);
    if (validateJars != null) {
      props.put("org.cougaar.validate.jars", validateJars);
      System.err.println("Set name to "+validateJars);
    }

    String name = (String) myArgs.get(ArgTableIfc.NAME_KEY);
    if (name != null) {
      props.put("org.cougaar.node.name", name);
      System.err.println("Set name to "+name);
    }

    String config = (String) myArgs.get(ArgTableIfc.CONFIG_KEY);
    if (config != null) {
      props.put("org.cougaar.config", config);
      System.err.println("Set config to "+config);
    }

    String cs = (String) myArgs.get(ArgTableIfc.CS_KEY);
    if (cs != null && cs.length()>0) {
      props.put("org.cougaar.config.server", cs);
      System.err.println("Using ConfigurationServer at "+cs);
    }

    String ns = (String) myArgs.get(ArgTableIfc.NS_KEY);
    if (ns != null && ns.length()>0) {
      props.put("org.cougaar.name.server", ns);
      System.err.println("Using NameServer at "+ns);
    }

    String port = (String) myArgs.get(ArgTableIfc.PORT_KEY);
    if (port != null && port.length()>0) {
      props.put("org.cougaar.name.server.port", port);
      System.err.println("Using NameServer on port " + port);
    }

    String filename = (String) myArgs.get(ArgTableIfc.FILE_KEY);
    if (filename != null) {
      props.put("org.cougaar.filename", filename);
      System.err.println("Using file "+filename);
    }

    String experimentId = 
      (String) myArgs.get(ArgTableIfc.EXPERIMENT_ID_KEY);
    if (experimentId != null) {
      props.put("org.cougaar.experiment.id", experimentId);
      System.err.println("Using experiment ID "+experimentId);
    }

    String controlHost = (String) myArgs.get(ArgTableIfc.CONTROL_KEY);
    if (controlHost != null) {
      props.put("org.cougaar.control.host", controlHost);
      System.err.println("Using control host "+controlHost);
    }

    String controlPort = (String) myArgs.get(ArgTableIfc.CONTROL_PORT_KEY);
    if (controlPort != null) {
      props.put("org.cougaar.control.port", controlPort);
      System.err.println("Using control port "+controlPort);
    }
  }


  /** Returns true if all plugin jars are signed, else false **/
  private static boolean validatePluginJarsByStream() {
    Properties props = System.getProperties();
    String jarSubdirectory = "plugins";
    String installpath = props.getProperty("org.cougaar.install.path");
    String defaultCertPath = "configs" + File.separatorChar + "common"
      + File.separatorChar + "alpcertfile.cer";

    String certPath = props.getProperty("org.cougaar.security.certificate",
        defaultCertPath);

    try{
      String files[] = searchForJars(jarSubdirectory, installpath);
      Vector cache = new Vector();
      File myCertFile = new File(installpath + File.separatorChar + certPath );

      System.out.println("Using Alp Certificate from: " + myCertFile);

      java.security.cert.Certificate myCert = null;
      myCert = getAlpineCertificate(myCertFile);
      for (int i=0; i<files.length; i++) {
        String filename = files[i];

        File f = new File(
            installpath+File.separatorChar+
            jarSubdirectory+File.separatorChar+filename);
        System.out.println("Testing Plugin Jar ("+
            f.toString() +")");
        FileInputStream fis = new FileInputStream(f);
        JarInputStream jis = new  JarInputStream(fis);
        try {
          JarEntry je=null;
          while (null != (je = jis.getNextJarEntry())) {
            cache.addElement(je);
          }
        } catch( IOException ioex ) {
          // silent
        }

        //
        // According to documentation -
        // certificates apparently are not valid until end/stream
        // reached -- hence
        // cache/then de-reference Certificates to be really sure.
        //
        Enumeration en = cache.elements();
        long sumUnsigned=0;
        long sumSigned=0;
        long sumMatchCerts=0;
        long sumUnMatchedCerts=0;
        while (en.hasMoreElements()) {
          JarEntry je = (JarEntry)en.nextElement();
          java.security.cert.Certificate[] certs = je.getCertificates();
          if (certs == null) {
            //  directories are unsigned -- so if jars contain
            //  directory hierarchy structures (ala packages)
            //  these will show up.
            sumUnsigned++;
            //System.out.println("Unsigned Klass:" + je.getName());
          } else {
            int len = certs.length;
            boolean anymatch = false;
            for (int j = 0; j < len; j++) {
              java.security.cert.Certificate cc = certs[j];
              if (cc.equals(myCert)) {
                anymatch = true; 
              }
            }
            if (anymatch) {
              sumMatchCerts++;
            } else {
              sumUnMatchedCerts++;
            }
            sumSigned++;
          }
          //System.out.println(""+  je.getName() +" certs=" + certs);
        }
        System.out.print(
            "        sigs:(" + sumUnsigned + "," + sumSigned + ")");
        if (sumSigned == 0) {
          System.out.println("...JAR FILE IS UNSIGNED.");
        } else {
          System.out.println("...JAR FILE IS SIGNED.");
        }

        System.out.print(
            "        alpcerts:(" +
            sumMatchCerts + "," + sumUnMatchedCerts + ")");
        if ((sumUnMatchedCerts == 0) && (sumMatchCerts > 0)) {
          System.out.println("...ALPINE CERTIFIED.");
        } else {
          System.out.println("...NOT ALPINE CERTIFIED.");
        }

        /// CRASH THE PARTY IF PLUGINS ARE UNSIGNED...
        if ((sumSigned == 0) || (sumMatchCerts == 0)) {
          return false;
        }
      }
    }catch (Exception ex) {
      ex.printStackTrace();
    }
    return true;
  }

  /** look for jar files in org.cougaar.install.path/subdir **/
  private static String[] searchForJars(String subdir, String installpath)
  {
    String[] files = new String[0];
    File d = new File(installpath+File.separatorChar+subdir);
    System.out.println(
        "Searching for plugin jars in directory: " + d.toString());
    if (d.exists() && d.isDirectory()) {
      files = d.list(
          new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return (name.endsWith(".jar") ||
                name.endsWith(".zip"));
              }
            }
          );
    }
    return files;
  }

  private static java.security.cert.Certificate getAlpineCertificate(
      File myCertFile) {

    java.security.cert.Certificate cert = null;
    try {
      FileInputStream fis = new FileInputStream(myCertFile);
      DataInputStream dis = new DataInputStream(fis);

      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      byte[] bytes = new byte[dis.available()];
      dis.readFully(bytes);
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

      while (bais.available() > 0) {
        cert = cf.generateCertificate(bais);
        //System.out.println(cert.toString());
      }
    } catch (Exception ex ) {
      ex.printStackTrace();
    }
    return cert;
  }

  // AgentManager component hook for now 
  private AgentManager agentManager;

  /**
   * Create the node, which is configured with System Properties.
   */
  public Node() {
    super();
  }

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node";
  }
  protected ServiceBroker specifyChildContext() {
    return new NodeServiceBroker();
  }
  protected ServiceBroker specifyChildServiceBroker() {
    // Nobody above us so we have a plain old ServiceBroker
    return new NodeServiceBroker();
  }
  protected ContainerAPI getContainerProxy() {
    return new NodeProxy();
  }

  public void serviceRevoked(ServiceRevokedEvent e) {}; 

  public void addStreamToRootLogging(OutputStream logStream) {
      ServiceBroker sb = getServiceBroker();
      LoggingControlService lcs=(LoggingControlService)sb.getService(this,LoggingControlService.class,this);
      lcs.addOutputType("root",lcs.STREAM,logStream);
  }

public boolean removeStreamFromRootLogging(OutputStream logStream) {
      ServiceBroker sb = getServiceBroker();
      LoggingControlService lcs=(LoggingControlService)sb.getService(this,LoggingControlService.class,this);
      return lcs.removeOutputType("root",lcs.STREAM,logStream);
  }

  /**
   *   @return String the string object containing the local DNS name
   */
  protected String findHostName() throws UnknownHostException {
     return InetAddress.getLocalHost().getHostName();
  }

  /** Refernece containing the Messenger **/
  private transient MessageTransportService theMessenger = null;

  /**
   * Post a message to the destination's message queue
   * @param aMessage The message object to post into the system.
   **/
  public final void sendMessage(Message aMessage) 
    throws MessageTransportException {
      theMessenger.sendMessage(aMessage);
    }

  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  /**
   *    This method provides the initialization of a Node.
   *    <p>
   *    @exception UnknownHostException IF the host can not be determined
   **/  
  protected void initNode()
    throws UnknownHostException, NamingException, IOException,
           SQLException, InitializerServiceException
  {
    // get the node name
    String name = System.getProperty("org.cougaar.node.name");
    if (name == null) {
      name = findHostName();
      if (name == null) {
        throw new IllegalArgumentException("Node name not specified");
      }
    }

    // get the start mode (file or experimentId)
    String filename = System.getProperty("org.cougaar.filename");
    String experimentId = System.getProperty("org.cougaar.experiment.id");
    if (filename == null) {
      if (experimentId == null) {
        // use the default "name.ini"
        filename = name + ".ini";
      } else {
        // use the filename
      }
    } else if (experimentId == null) {
      // use the experimentId
    } else {
      throw new IllegalArgumentException(
          "Both file name (-f) and experiment -X) specified. "+
          "Only one allowed.");
    }

    // set the node name
    NodeIdentifier nid = new NodeIdentifier(name);
    setNodeIdentifier(nid);

    // create the AgentManager on our own for now
    BinderFactory ambf = new AgentManagerBinderFactory();
    if (!attachBinderFactory(ambf)) {
      throw new RuntimeException(
          "Failed to load the AgentManagerBinderFactory in Node");
    }
    agentManager = new AgentManager();
    super.add(agentManager);
  
    ServiceBroker sb = getServiceBroker();

    sb.addService(NodeIdentificationService.class,
		  new NodeIdentificationServiceProvider(nid));
    
    sb.addService(NamingService.class,
                  new NamingServiceProvider(System.getProperties()));

    LoggingServiceProvider loggingServiceProvider = 
      new LoggingServiceProvider(System.getProperties());
    sb.addService(LoggingService.class,
		  loggingServiceProvider);
    sb.addService(LoggingControlService.class,
		  loggingServiceProvider);

    //add the vm metrics
    sb.addService(NodeMetricsService.class,
                  new NodeMetricsServiceProvider(new NodeMetricsProxy()));

    ServiceProvider sp;
    if (filename != null)
      sp = new FileInitializerServiceProvider();
    else
      sp = new DBInitializerServiceProvider(experimentId);
    sb.addService(InitializerService.class, sp);
    InitializerService is = (InitializerService) sb.getService(
        this, InitializerService.class, null);
    ComponentDescription[] agentDescs =
      is.getComponentDescriptions(name, "Node.AgentManager.Agent");
    sb.releaseService(this, InitializerService.class, is);

    // Set up MTS and QOS service provides.
    //
    // NB: The order is important for now - MTS *must* be created
    // first.
    initTransport();  
    initQos();

    registerExternalNodeController();

    // load the clusters
    //
    // once bulk-add ComponentMessages are implements this can
    //   be done with "this.receiveMessage(compMsg)"
    add(agentDescs);

    //mgmtLP = new MgmtLP(this); // MTMTMT turn off till RMI namespace works
  }


    // **** QUO *****
    // Change this to create (or find?) a MessageTransportManager as the
    // value of theMessenger.
  private void initTransport() {
    String name = getIdentifier();
    MessageTransportServiceProvider mtsp = 
	new MessageTransportServiceProvider(name);
    add(mtsp);
    ServiceBroker sb = getServiceBroker();
    sb.addService(MessageTransportService.class, mtsp);
    sb.addService(MessageStatisticsService.class, mtsp);
    sb.addService(MessageWatcherService.class, mtsp);
    sb.addService(AgentStatusService.class, mtsp);

    theMessenger = (MessageTransportService)
      getServiceBroker().getService(this, MessageTransportService.class, null);
    System.err.println("Started "+theMessenger);
    theMessenger.registerClient(this);
  }


  private void initQos() {
    String name = getIdentifier();
    QosMonitorServiceProvider qmsp = new QosMonitorServiceProvider(name);
    add(qmsp);
    getServiceBroker().addService(QosMonitorService.class, qmsp);
    getServiceBroker().addService(ResourceMonitorService.class, qmsp);
  }


  // external controller for this node
  private ExternalNodeController eController;

  /**
   * Create an <code>ExternalNodeController</code> for this Node and 
   * register it for external use.
   * <p>
   * Currently uses RMI, but could be modified to use another protocol
   * (e.g. HTTP server).
   */
  private void registerExternalNodeController() {
    // get the RMI registry host and port address
    String rmiHost = System.getProperty("org.cougaar.control.host");
    if (rmiHost == null) {
      // default to localhost
      try {
        rmiHost = findHostName();
      } catch (UnknownHostException e) {
      }
    } else if (rmiHost.length() <= 0) {
      rmiHost = null;
    }
    int rmiPort = -1;
    String srmiPort = System.getProperty("org.cougaar.control.port");
    if ((srmiPort != null) &&
        (srmiPort.length() > 0)) {
      try {
        rmiPort = Integer.parseInt(srmiPort);
      } catch (NumberFormatException e) {
      }
    }

    if ((rmiHost == null) ||
        (rmiPort <= 0)) {
      // don't register this Node
      System.err.println("Not registered for external RMI control");
      return;
    }

    try {
      //
      // add an RMI stub for this Node in the RMI space
      //
      // a better implementation is to lookup the external controller
      //   and register this node for external control, since it leaves
      //   the RMI space clean...
      //

      /*
      // create the local hook
      ExternalNodeController localENC = new ExternalNodeControllerImpl(this);
      
      // export to a remote hook
      ExternalNodeController remoteENC = 
        (ExternalNodeController)UnicastRemoteObject.exportObject(localENC);
        */
      ExternalNodeController remoteENC = new ExternalNodeControllerImpl(this);
      
      // use the NodeIdentifier's address as the binding name
      //  - this might need to be modified to incorporate the host name...
      String bindName = getIdentifier();

      // get the RMI registry
      Registry reg = LocateRegistry.getRegistry(rmiHost, rmiPort);      

      // make available
      reg.rebind(bindName, remoteENC);
      
      System.err.println(
          "Registered for external control as \""+bindName+"\""+
          " in RMI registry \""+rmiHost+":"+rmiPort+"\"");

      eController = remoteENC;

      // never unbind!  this leaves a mess...
      //reg.unbind(getIdentifier());
      //UnicastRemoteObject.unexportObject(remoteENC, true);
    } catch (Exception e) {
      System.err.println("Unable to register for external control:");
      e.printStackTrace();
    }
  }
  
 

  // Need this so that when the AgentManager creates a cluster, the cluster
  // gets properly hooked up with the externalnodeactionlistener and
  // gets put into the Node's running list of clusters.
  // This is an artifact of moving the cluster creation to AgentManager and
  // should probably be cleanup up further.

  private void registerCluster(ClusterServesClusterManagement cluster) {
    // get the (optional) external listener
    ExternalNodeActionListener eListener;
    try {
      eListener =
        ((eController != null) ? 
         eController.getExternalNodeActionListener() :
         null);
    } catch (Exception e) {
      eListener = null;
    }
    
    // notify the listener
    if (eListener != null) {
      if (cluster != null) {
        try {
          eListener.handleClusterAdd(eController, cluster.getClusterIdentifier());
        } catch (Exception e) {
          // lost listener?  should we kill this Node?
          System.err.println("Lost connection to external listener? "+e.getMessage());
          try {
            eController.setExternalNodeActionListener(null);
          } catch (Exception e2) {
          }
        }
      }
    }
  }


  // replace with Container's add, but keep this basic code
  private final void add(ComponentDescription desc) {
    // simply wrap as a single-element "bulk" operation
    ComponentDescription[] descs = new ComponentDescription[1];
    descs[0] = desc;
    add(descs);
  }
 
  /**
   * Add Clusters and their child Components (Plugins, etc) to this Node.
   * <p>
   * Note that this is a bulk operation, since the loading process is:<ol>
   *   <li>Create the empty clusters</li>
   *   <li>Add the Plugins and initialize the clusters</li>
   * </ol>
   * <p>
   */
  protected void add(ComponentDescription[] descs) {
    int nDescs = ((descs != null) ? descs.length : 0);
    System.err.print("Creating Clusters:");
    for (int i = 0; i < nDescs; i++) {
      try {
        ComponentDescription desc = descs[i];
        //Let the agentmanager create the cluster
        agentManager.add(desc);
      } catch (Exception e) {
        System.err.println("Exception creating clusters: " + e);
        e.printStackTrace();
      }
    }
  }

  public MessageAddress getMessageAddress() {
    return myNodeIdentity_;
  }

  public void receiveMessage(Message m) {
    try {
      if (m instanceof ComponentMessage) {
        ComponentMessage cm = (ComponentMessage)m;
        int operation = cm.getOperation();
        System.out.println("\n\ngot message: " +cm);
        if (operation == ComponentMessage.ADD) {
          // add
          ComponentDescription cd = cm.getComponentDescription();
          StateTuple st = 
            new StateTuple(
                cd,
                cm.getState());
          // should do "add(st)", but requires Node fixes
          //
          // for now we do this work-around:
          String ip = cd.getInsertionPoint();
          if (!("Node.AgentManager.Agent".equals(ip))) {
            throw new UnsupportedOperationException(
              "Only Agent ADD supported for now, not "+ip);
          }
          agentManager.add(st);
        } else {
          // not implemented yet!  will be okay once Node is a Container
          throw new UnsupportedOperationException(
            "Unsupported ComponentMessage: "+m);
        }
      } else if (m instanceof MoveAgentMessage) {
        MoveAgentMessage mam = (MoveAgentMessage)m;
        final ClusterIdentifier agentID = mam.getAgentIdentifier();
        final NodeIdentifier nodeID = mam.getNodeIdentifier();
        Runnable moveRunner = new Runnable() {
          public void run() {
            agentManager.moveAgent(agentID, nodeID);
          }
        };
        Thread t = 
          new Thread(
              moveRunner, 
              "MoveAgent "+agentID+" to "+nodeID);
        t.start();
      } else if (m.getTarget().equals(MessageAddress.SOCIETY)) {
        // we don't do anything with these. ignore it.
      } else {
        throw new UnsupportedOperationException(
            "Unsupported Message: "+
            ((m != null) ? m.getClass().getName() : "null"));
      }
    } catch (Exception e) {
      System.err.println("Node received invalid message: "+e.getMessage());
      e.printStackTrace();
    }
  }

  //
  // ClusterManagementServesCluster
  //

  public String getName() {
    return getIdentifier();
  }

  private static void printVersion() {
    String version = null;
    long buildtime = 0;
    try {
      Class vc = Class.forName("org.cougaar.Version");
      Field vf = vc.getField("version");
      Field bf = vc.getField("buildTime");
      version = (String) vf.get(null);
      buildtime = bf.getLong(null);
    } catch (Exception e) {}

    synchronized (System.err) {
      System.out.print("COUGAAR ");
      if (version == null) {
        System.out.println("(unknown version)");
      } else {
        System.out.println(version+" built on "+(new Date(buildtime)));
      }

      String vminfo = System.getProperty("java.vm.info");
      String vmv = System.getProperty("java.vm.version");
      System.out.println("VM: JDK "+vmv+" ("+vminfo+")");
      String os = System.getProperty("os.name");
      String osv = System.getProperty("os.version");
      System.out.println("OS: "+os+" ("+osv+")");
    }

  }

  public void requestStop() {}

  // 
  //support classes
  //

  // Children's view of the parent component Node - as accessed through 
  // the NodeForBinder interface.  Keeps the actual Node safe.
  private class NodeProxy implements NodeForBinder, BindingSite {
    // ClusterManagementServesCluster
   
    public String getName() {return Node.this.getName(); }
    public String getIdentifier() {
      return Node.this.getIdentifier();
    }
    public boolean remove(Object o) {return true;}
    // BindingSite
    public ServiceBroker getServiceBroker() {return Node.this.getServiceBroker(); }
    public void requestStop() {}
    // extra pieces
    public void registerCluster(ClusterServesClusterManagement cluster) {
      Node.this.registerCluster(cluster);
    }
  }

  private static class NodeServiceBroker extends ServiceBrokerSupport {}

  private class NodeMetricsProxy implements NodeMetricsService {
    /** Free Memory snapshot from the Java VM   **/
    public long getFreeMemory() {
      return Runtime.getRuntime().freeMemory();
    }
    /** Total memory snaphsot from the Java VM    */
    public long getTotalMemory() {
      return Runtime.getRuntime().totalMemory();
    }
    /** The number of active Threads in the main COUGAAR threadgroup **/
    public int getActiveThreadCount() {
      return Thread.currentThread().getThreadGroup().activeCount();
    }
  }

}

