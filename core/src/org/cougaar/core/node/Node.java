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

package org.cougaar.core.node;

import org.cougaar.core.security.*;
import org.cougaar.core.service.*;
import org.cougaar.core.mts.*;

import org.cougaar.core.thread.ThreadServiceProvider;

import org.cougaar.core.qos.monitor.QosMonitorService;
import org.cougaar.core.qos.monitor.QosMonitorServiceProvider;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.MetricsServiceProvider;

import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageWatcherService;

import org.cougaar.core.agent.ClusterServesClusterManagement;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.service.NamingService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;


import org.cougaar.core.component.*;

import java.io.Serializable;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.*;
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
import org.cougaar.core.agent.ClusterInitializedMessage;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.*;
import org.cougaar.util.*;

import org.cougaar.core.component.*;
import java.beans.Beans;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.LogTarget;
import org.cougaar.core.security.bootstrap.SystemProperties;

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
 *    <tt>java [props] org.cougaar.core.node.Node [props] [-help]</tt>
 * where the "props" are "-D" System Properties, such as:
 *    "-Dorg.cougaar.node.name=NAME" -- name of the Node
 * See the documentation below for Node-specific properties, and 
 * the external documentation for a list of all COUGAAR properties.
 * </pre>
 *
 * <pre>
 * @property org.cougaar.node.name
 *   The (required) name for this Node.
 * @property org.cougaar.useBootstrapper
 *   If <em>true</em>, will launch using the Bootstrapper to find jar files.
 *   Defaults to <em>true</em>.
 * @property org.cougaar.filename
 *   The file name (.ini) for starting this Node, which defaults to 
 *   (<em>org.cougaar.node.name</em>+".ini") if both <em>org.cougaar.filename</em>
 *   and </em>org.cougaar.experiment.id</em> are not specified.  If this property 
 *   is specified then <em>org.cougaar.experiment.id</em> must not be specified.
 * @property org.cougaar.experiment.id
 *   The experiment identifier for running this Node; see 
 *   <em>org.cougaar.filename</em> for details.
 * @property org.cougaar.install.path
 *   The <em>base</em> path for finding jar and configuration files.
 * @property org.cougaar.validate.jars
 *   If <em>true</em>, will check the certificates on the 
 *   (<em>org.cougaar.install.path</em>+"/plugin/*.jar") files.  Defaults to 
 *   <em>false</em>.
 * @property org.cougaar.security.certificate
 *   The path of the <em>org.cougaar.install.path</em> for finding the 
 *   <em>org.cougaar.validate.jars</em> certificates.
 *
 * @property org.cougaar.config
 *   Only used by Node to transfer a command-line "-config X" to
 *   the corresponding <em>org.cougaar.config=X</em> property.
 * @property org.cougaar.config.server
 *   Only used by Node to transfer a command-line "-cs X" to
 *   the corresponding <em>org.cougaar.config.server=X</em> property.
 * @property org.cougaar.name.server
 *   Only used by Node to transfer a command-line "-ns X" to
 *   the corresponding <em>org.cougaar.name.server=X</em> property.
 * @property org.cougaar.name.server.port
 *   Only used by Node to transfer a command-line "-port X" to
 *   the corresponding <em>org.cougaar.name.server.port=X</em> property.
 * 
 * @property org.cougaar.core.security.Component
 *   The class to use for the security domain Component.  If not set, no
 *   security manager will be used.  A security manager must implement
 *   the interface org.cougaar.core.security.SecurityComponent.  If set and
 *   not found or not loaded properly, Node will refuse to run.  See 
 *   org.cougaar.core.security.StandardSecurityComponent for sample implementation.
 *  
 * @property org.cougaar.core.servlet.enable
 *   Used to enable ServletService; defaults to "true".
 *
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
    if (PropertyParser.getBoolean("org.cougaar.useBootstrapper", true)) {
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  

  static public void launch(String[] args) {

    // check for "-version" and "-help"
    for (int i = 0; i < args.length; i++) {
      String argi = args[i];
      if (argi.equals("-version") ||
          argi.equals("--version")) {
        printVersion(true);
        return;
      }
      if (argi.equals("-info") ||
          argi.equals("--info")) {
        printVersion(false);
        return;
      }
      if (argi.equals("-help") ||
          argi.equals("--help")) {
        System.out.print(
            "Usage: java [JVM_OPTIONS] [-D..] "+
            Node.class.getName()+" [-D..] [ARGS]\n"+
            "A Node manages and executes Cougaar agents.\n\n"+
            "  -D.. \t NAME=VALUE configuration properties.\n"+
            "  -help, --help, -? \t display this help and exit.\n"+
            "  -version, --version \t output version information and exit.\n\n"+
            "  -info, --info \t output terse version information and exit.\n\n"+
            "See <http://www.cougaar.org> for further details.\n\n"+
            "Report bugs to <cougaar@cougaar.org>.\n");
        return;
      }
    }

    // display the version info
    printVersion(true);

    // convert any command-line args to System Properties
    setSystemProperties(args);

    // check for valid plugin jars
    boolean validateJars = PropertyParser.getBoolean("org.cougaar.validate.jars", false);
    if (validateJars) {
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
    List revisedArgs = new ArrayList();

    // separate the args into "-D" properties and normal arguments
    for (int i = 0; i < args.length; i++) {
      String argi = args[i];
      if (argi.startsWith("-D")) {
        // transfer a "late" system property
        int sepIdx = argi.indexOf('=');
        if (sepIdx < 0) {
          System.setProperty(argi.substring(2), "");
        } else {
          System.setProperty(argi.substring(2, sepIdx), argi.substring(sepIdx+1));
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
      System.setProperty("org.cougaar.validate.jars", validateJars);
      System.err.println("Set name to "+validateJars);
    }

    String name = (String) myArgs.get(ArgTableIfc.NAME_KEY);
    if (name != null) {
      System.setProperty("org.cougaar.node.name", name);
      System.err.println("Set name to "+name);
    }

    String config = (String) myArgs.get(ArgTableIfc.CONFIG_KEY);
    if (config != null) {
      System.setProperty("org.cougaar.config", config);
      System.err.println("Set config to "+config);
    }

    String cs = (String) myArgs.get(ArgTableIfc.CS_KEY);
    if (cs != null && cs.length()>0) {
      System.setProperty("org.cougaar.config.server", cs);
      System.err.println("Using ConfigurationServer at "+cs);
    }

    String ns = (String) myArgs.get(ArgTableIfc.NS_KEY);
    if (ns != null && ns.length()>0) {
      System.setProperty("org.cougaar.name.server", ns);
      System.err.println("Using NameServer at "+ns);
    }

    String port = (String) myArgs.get(ArgTableIfc.PORT_KEY);
    if (port != null && port.length()>0) {
      System.setProperty("org.cougaar.name.server.port", port);
      System.err.println("Using NameServer on port " + port);
    }

    String filename = (String) myArgs.get(ArgTableIfc.FILE_KEY);
    if (filename != null) {
      System.setProperty("org.cougaar.filename", filename);
      System.err.println("Using file "+filename);
    }

    String experimentId = 
      (String) myArgs.get(ArgTableIfc.EXPERIMENT_ID_KEY);
    if (experimentId != null) {
      System.setProperty("org.cougaar.experiment.id", experimentId);
      System.err.println("Using experiment ID "+experimentId);
    }
  }


  /** Returns true if all plugin jars are signed, else false **/
  private static boolean validatePluginJarsByStream() {
    String jarSubdirectory = "plugins";
    String installpath = System.getProperty("org.cougaar.install.path");
    String defaultCertPath = "configs" + File.separatorChar + "common"
      + File.separatorChar + "alpcertfile.cer";

    String certPath = System.getProperty("org.cougaar.security.certificate",
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

  public void serviceRevoked(ServiceRevokedEvent e) {}

  public void addStreamToRootLogging(OutputStream logStream) {
    ServiceBroker sb = getServiceBroker();
    LoggingControlService lcs = (LoggingControlService)
      sb.getService(this, LoggingControlService.class, this);
    LoggerController lc = lcs.getLoggerController("root");
    lc.addLogTarget(LogTarget.STREAM, logStream);
  }

  public boolean removeStreamFromRootLogging(OutputStream logStream) {
    ServiceBroker sb = getServiceBroker();
    LoggingControlService lcs = (LoggingControlService)
      sb.getService(this, LoggingControlService.class, this);
    LoggerController lc = lcs.getLoggerController("root");
    return lc.removeLogTarget(LogTarget.STREAM, logStream);
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

    {
      //String smn = System.getProperty(SecurityComponent.SMC_PROP);
      String smn = System.getProperty(SecurityComponent.SMC_PROP, "org.cougaar.core.security.StandardSecurityComponent");
      if (smn != null) {
        try {
          Class smc = Class.forName(smn);
          if (SecurityComponent.class.isAssignableFrom(smc)) {
            ComponentDescription smcd = 
              new ComponentDescription(
                                       getIdentifier()+"SecurityComponent",
                                       "Node.SecurityComponent",
                                       smn,
                                       null,  //codebase
                                       null,  //parameters
                                       null,  //certificate
                                       null,  //lease
                                       null); //policy
            super.add(smcd);
          } else {
            System.err.println("Error: SecurityComponent specified as "+smn+" which is not an instance of SecurityComponent");
            System.exit(1);
          }
        } catch (Exception e) {
          System.err.println("Error: Could not load SecurityComponent "+smn+": "+e);
          e.printStackTrace();
          System.exit(1);
        }
      }
    }

    ServiceBroker sb = getServiceBroker();

    ThreadServiceProvider tsp = new ThreadServiceProvider(sb, "Node " + name);
    tsp.provideServices(sb);


    agentManager = new AgentManager();
    super.add(agentManager);
  
    sb.addService(NodeIdentificationService.class,
		  new NodeIdentificationServiceProvider(nid));
    
    sb.addService(NamingService.class,
                  new NamingServiceProvider(SystemProperties.getSystemPropertiesWithPrefix("javax.naming.")));

    LoggingServiceProvider loggingServiceProvider = 
      new LoggingServiceProvider(SystemProperties.getSystemPropertiesWithPrefix("org.cougaar.core.logging."));
    sb.addService(LoggingService.class,
		  loggingServiceProvider);
    sb.addService(LoggingControlService.class,
		  loggingServiceProvider);
    

    NodeIdentifier id = getNodeIdentifier();
    MetricsServiceProvider msp = new MetricsServiceProvider(sb, id);
    sb.addService(MetricsService.class, msp);
    sb.addService(MetricsUpdateService.class, msp);


    //add the vm metrics
    sb.addService(NodeMetricsService.class,
                  new NodeMetricsServiceProvider(new NodeMetricsProxy()));

    ServiceProvider sp;
    if (filename != null) {
      sp = new FileInitializerServiceProvider();
    } else {
      sp = new DBInitializerServiceProvider(experimentId);
    }
    sb.addService(InitializerService.class, sp);
    InitializerService is = (InitializerService) sb.getService(this, InitializerService.class, null);

    ComponentDescription[] agentDescs =
      is.getComponentDescriptions(name, "Node.AgentManager");
    sb.releaseService(this, InitializerService.class, is);


    // Set up MTS and QOS service provides.
    //
    // NB: The order is important for now - MTS *must* be created
    // first.
    initTransport();  

    // register for external control by the AppServer
    //   -- disabled for now --

    // start up the NodeTrust component
    String ntc = new String(getIdentifier()+"NodeTrust");
    ComponentDescription ntcdesc = 
      new ComponentDescription(
                                ntc,
                                "Node.NodeTrust",
                                "org.cougaar.core.node.NodeTrustComponent",
                                null,  //codebase
                                null,  //parameters
                                null,  //certificate
                                null,  //lease
                                null); //policy
    super.add(ntcdesc);

    String enableServlets = 
      System.getProperty("org.cougaar.core.servlet.enable");
    if ((enableServlets == null) ||
        (enableServlets.equalsIgnoreCase("true"))) {
      // start up the Node-level ServletService component
      ComponentDescription nsscDesc = 
        new ComponentDescription(
            (getIdentifier()+"ServletService"),
            "Node.NodeServletService",
            "org.cougaar.lib.web.service.RootServletServiceComponent",
            null,  //codebase
            null,  //parameters
            null,  //certificate
            null,  //lease
            null); //policy
      super.add(nsscDesc);
    }

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

    initQos(mtsp);
    

  }


  private void initQos (MessageTransportServiceProvider mtsp) {
    String name = getIdentifier();
    QosMonitorServiceProvider qmsp = new QosMonitorServiceProvider(name, mtsp);
    add(qmsp);
    getServiceBroker().addService(QosMonitorService.class, qmsp);
  }


  // Need this so that when the AgentManager creates a cluster, the cluster
  // gets properly hooked up with the externalnodeactionlistener and
  // gets put into the Node's running list of clusters.
  // This is an artifact of moving the cluster creation to AgentManager and
  // should probably be cleanup up further.

  private void registerCluster(ClusterServesClusterManagement cluster) {
    // notify the remote AppServer controller
    //  -- disabled for now --
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
    //System.err.print("Creating Clusters:");
    for (int i = 0; i < nDescs; i++) {
      ComponentDescription desc = descs[i];
      try {
        //Let the agentmanager create the cluster
        agentManager.add(desc);
      } catch (Exception e) {
        System.err.println("Exception creating component ("+desc+"): " + e);
        e.printStackTrace();
      }
    }
  }

  public MessageAddress getMessageAddress() {
    return myNodeIdentity_;
  }

  public void receiveMessage(final Message m) {
    try {
      if (m instanceof ComponentMessage) {
        ComponentMessage cm = (ComponentMessage)m;
        int operation = cm.getOperation();
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
      } else if (m instanceof AgentManagementMessage) {
        // run in a separate thread (in case the source is local)
        Runnable r = new Runnable() {
          public void run() {
            if (m instanceof MoveAgentMessage) {
              MoveAgentMessage mam = (MoveAgentMessage) m;
              agentManager.moveAgent(
                  mam.getAgentIdentifier(), 
                  mam.getNodeIdentifier());
            } else if (m instanceof CloneAgentMessage) {
              CloneAgentMessage cam = (CloneAgentMessage) m;
              agentManager.cloneAgent(
                  cam.getAgentIdentifier(), 
                  cam.getNodeIdentifier(),
                  cam.getCloneAgentIdentifier(),
                  cam.getCloneBlackboard());
            } else {
              // ignore
            }
          }
        };
        Thread t = new Thread(r, m.toString());
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

  private static void printVersion(boolean fullFormat) {
    String version = null;
    long buildTime = -1;
    String repositoryTag = null;
    boolean repositoryModified = false;
    long repositoryTime = -1;
    try {
      Class vc = Class.forName("org.cougaar.Version");
      Field vf = vc.getField("version");
      Field bf = vc.getField("buildTime");
      version = (String) vf.get(null);
      buildTime = bf.getLong(null);
      Field tf = vc.getField("repositoryTag");
      Field rmf = vc.getField("repositoryModified");
      Field rtf = vc.getField("repositoryTime");
      repositoryTag = (String) tf.get(null);
      repositoryModified = rmf.getBoolean(null);
      repositoryTime = rtf.getLong(null);
    } catch (Exception e) {}

    if (!(fullFormat)) {
      System.out.print(
          "COUGAAR\t"+version+"\t"+buildTime+
          "\t"+repositoryTag+"\t"+repositoryModified+"\t"+repositoryTime+"\n");
      return;
    } 

    synchronized (System.out) {
      System.out.print("COUGAAR ");
      if (version == null) {
        System.out.println("(unknown version)");
      } else {
        System.out.println(
            version+" built on "+
            ((buildTime > 0) ? 
             ((new Date(buildTime)).toString()) : 
             "(unknown time)"));
      }
      System.out.println(
          "Repository: "+
          ((repositoryTag != null) ? 
           (repositoryTag + 
            (repositoryModified ? " (modified)" : "")) :
           "(unknown tag)")+
          " on "+
          ((repositoryTime > 0) ? 
           ((new Date(repositoryTime)).toString()) :
           "(unknown time)"));
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

