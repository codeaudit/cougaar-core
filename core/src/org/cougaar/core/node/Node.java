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

import org.cougaar.core.service.*;
import org.cougaar.core.mts.*;

import org.cougaar.core.thread.ThreadServiceProvider;

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
import org.cougaar.bootstrap.Bootstrapper;

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
 *   org.cougaar.core.node.StandardSecurityComponent for sample implementation.
 *  
 * @property org.cougaar.core.servlet.enable
 *   Used to enable ServletService; defaults to "true".
 *
 * </pre>
 */
public class Node extends ContainerSupport
implements ClusterManagementServesCluster, ContainerAPI, ServiceRevokedListener
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
  // @deprecated
  static public void main(String[] args){
    if (PropertyParser.getBoolean("org.cougaar.useBootstrapper", true)) {
      System.err.println("Warning! org.cougaar.useBootstrapper is deprecated.  Invoke Bootstrapper directly.");
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  

  /** The real entry-point for Node, generally invoked via 
   * the bootstrapper.
   * @see org.cougaar.bootstrap.Bootstrapper
   **/
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
            "  -Dname=value        set configuration property.\n"+
            "  -help, --help, -?   display this help and exit.\n"+
            "  -version, --version output version information and exit.\n"+
            "  -info, --info       output terse version information and exit.\n\n"+
            "See <http://www.cougaar.org> for further help and bug reports.\n");
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
      //System.err.println("Set name to "+validateJars);
    }

    String name = (String) myArgs.get(ArgTableIfc.NAME_KEY);
    if (name != null) {
      System.setProperty("org.cougaar.node.name", name);
      //System.err.println("Set name to "+name);
    }

    String config = (String) myArgs.get(ArgTableIfc.CONFIG_KEY);
    if (config != null) {
      System.setProperty("org.cougaar.config", config);
      //System.err.println("Set config to "+config);
    }

    String cs = (String) myArgs.get(ArgTableIfc.CS_KEY);
    if (cs != null && cs.length()>0) {
      System.setProperty("org.cougaar.config.server", cs);
      //System.err.println("Using ConfigurationServer at "+cs);
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

    // sb is our service broker (for *all* agents)
    ServiceBroker sb = getServiceBroker();

    // we need to make this available here so others can get it.
    sb.addService(NodeIdentificationService.class,
		  new NodeIdentificationServiceProvider(nid));

    //
    // construct the NodeAgent and hook it in.
    // 

    BinderFactory ambf = new AgentManagerBinderFactory();
    if (!attachBinderFactory(ambf)) {
      throw new Error("Failed to load the AgentManagerBinderFactory in Node");
    }

    // create the AgentManager on our own for now
    agentManager = new AgentManager();
    super.add(agentManager);

    //
    // construct the NodeAgent and then hand off control
    // 
    List naParams = new ArrayList(2);
    naParams.add(getServiceBroker());
    naParams.add(agentManager);
    ComponentDescription naDesc = 
      new ComponentDescription(
          name,
          "Node.AgentManager.Agent",
          "org.cougaar.core.node.NodeAgent",
          null,  //codebase
          naParams,
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH);
    agentManager.add(naDesc);

    // may need to wait for the NodeAgent to come all the way up.

  }

  public MessageAddress getMessageAddress() {
    return myNodeIdentity_;
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
      // NOOP
    }
  }

  private static class NodeServiceBroker extends ServiceBrokerSupport {}

}

