/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.naming.NamingException;
import org.cougaar.bootstrap.Bootstrapper;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderFactorySupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentFactory;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.LogTarget; // inlined

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
 * </pre>
 */
public class Node extends ContainerSupport
implements ContainerAPI, ServiceRevokedListener
{
  public static final String INSERTION_POINT = "Node";
  private MessageAddress myNodeIdentity_ = null;

  public String getIdentifier() {
    return 
      ((myNodeIdentity_ != null) ? 
       myNodeIdentity_.getAddress() :
       null);
  }

  public void setMessageAddress(MessageAddress aMessageAddress) {
    myNodeIdentity_ = aMessageAddress;
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
      Logging.getLogger(Node.class).warn(
          "-Dorg.cougaar.useBootstrapper is deprecated.  Invoke Bootstrapper directly.");
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
    // convert any command-line args to System Properties
    if (!setSystemProperties(args)) {
      return; // must be "--help"
    }

    // display the version info
    printVersion(true);

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
   * The only non "-D" command line arguments are:<pre>
   *   -n ARG         equivalent to "-Dorg.cougaar.node.name=ARG"
   *   -c             ignored, ancient "clear database" switch
   *   --?version     display version information and exit
   *   --?info        display terse version information and exit
   *   --?help        display usage help and exit
   *   <i>other</i>   display error message and exit
   * </pre>
   * <p>
   * Also supported are post-classname "-D" command-line properties:
   *    "java .. classname -Darg .." 
   * which will override the usual "java -Darg .. classname .."
   * properties.  For example, "java -Dx=y classname -Dx=z" is
   * equivalent to "java -Dx=z classname".  This can be useful when 
   * writing scripts that simply append properties to a command-line.
   * <p>
   * @return false if node should exit
   */
  private static boolean setSystemProperties(String[] args) {
    // separate the args into "-D" properties and normal arguments
    for (int i = 0; i < args.length; i++) {
      String argi = args[i];
      if (argi.startsWith("-D")) {
        // add a "late" system property
        int sepIdx = argi.indexOf('=');
        if (sepIdx < 0) {
          System.setProperty(argi.substring(2), "");
        } else {
          System.setProperty(argi.substring(2, sepIdx), argi.substring(sepIdx+1));
        }
      } else if (argi.equals("-n")) {
        // old "-n node" pattern
        String name = args[++i];
        System.setProperty("org.cougaar.node.name", name);
        Logging.getLogger(Node.class).info(
            "Set node name to "+name+
            "\nThe command line format \"-n "+name+"\" has been deprecated"+
            "\nPlease use \"-Dorg.cougaar.node.name="+name+"\"");
      } else if (argi.equals("-c")) {
        // ignore
        Logging.getLogger(Node.class).info(
            "Ignoring unused command-line argument \"-c\"");
      } else {
        // some form of exit
        if (argi.equals("-version") || argi.equals("--version")) {
          printVersion(true);
        } else if (argi.equals("-info") || argi.equals("--info")) {
          printVersion(false);
        } else if (argi.equals("-help") || argi.equals("--help")) {
          System.out.print(
              "Usage: java [JVM_OPTIONS] [-D..] "+
              Node.class.getName()+" [-D..] [ARGS]"+
              "\nA Node manages and executes Cougaar agents.\n"+
              "\n  -Dname=value        set configuration property."+
              "\n  -version, --version output version information and exit."+
              "\n  -info, --info       output terse version information and exit."+
              "\n  -help, --help       display this help and exit.\n"+
              "\nSee <http://www.cougaar.org> for further help and bug reports.\n");
        } else {
          System.err.println(
              "Node: unrecognized option `"+argi+"'"+
              "\nTry `Node --help' for more information.");
        }
        return false;
      }
    }
    return true;
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

      Logging.getLogger(Node.class).info("Using certificate from: "+myCertFile);

      java.security.cert.Certificate myCert = null;
      myCert = getAlpineCertificate(myCertFile);
      for (int i=0; i<files.length; i++) {
        String filename = files[i];

        File f = new File(
            installpath+File.separatorChar+
            jarSubdirectory+File.separatorChar+filename);

        Logging.getLogger(Node.class).info("Testing Plugin jar "+f);
      
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
    //System.out.println("Searching for plugin jars in directory: " + d.toString());
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
    return INSERTION_POINT;
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
  protected String findHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException uhe) {
      return null;
    }
  }

  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  /**
   *    This method provides the initialization of a Node.
   *    <p>
   *    @exception UnknownHostException IF the host can not be determined
   **/  
  protected void initNode() {
    // get the node name
    String name = System.getProperty("org.cougaar.node.name");
    if (name == null) {
      name = findHostName();
      if (name == null) {
        throw new IllegalArgumentException("Node name not specified");
      }
    }

    // set the node name
    MessageAddress nid = MessageAddress.getMessageAddress(name);
    setMessageAddress(nid);

    BinderFactory ambf = new AgentManagerBinderFactory();
    if (!attachBinderFactory(ambf)) {
      throw new Error("Failed to load the AgentManagerBinderFactory in Node");
    }

    // sb is our service broker (for *all* agents)
    ServiceBroker sb = getServiceBroker();

    // we need to make this available here so others can get it.
    sb.addService(NodeIdentificationService.class,
		  new NodeIdentificationServiceProvider(nid));

    // maybe provide the DBInitializerService, depending upon the
    // "expermentId" system property
    ComponentDescription dbInitDesc = 
      new ComponentDescription(
          "db-init",
          Node.INSERTION_POINT+".Init",
          "org.cougaar.core.node.DBInitializerServiceComponent",
          null,  //codebase
          null,  //params
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH);
    add(dbInitDesc);

    // we need the initializerservice so that AgentManager can load external binders
    ComponentDescription compInitDesc = 
      new ComponentDescription(
          "component-init",
          Node.INSERTION_POINT+".Init",
          "org.cougaar.core.node.ComponentInitializerServiceComponent",
          null,  //codebase
          null,  //params
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH);
    add(compInitDesc);

    //
    // construct the NodeAgent and hook it in.
    // 

    // create the AgentManager on our own for now
    AgentManager agentManager = new AgentManager();
    add(agentManager);

    //
    // construct the NodeAgent and then hand off control
    // 
    List naParams = new ArrayList(3);
    naParams.add(nid);
    naParams.add(getServiceBroker());
    naParams.add(agentManager);
    ComponentDescription naDesc = 
      new ComponentDescription(
          name,
          Agent.INSERTION_POINT,
          NodeAgent.class.getName(),
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

    Logging.getLogger(Node.class).info("COUGAAR "+version+" "+buildTime+
          " "+repositoryTag+" "+repositoryModified+" "+repositoryTime);


    if (!(fullFormat)) {
      System.out.println(
          "COUGAAR\t"+version+"\t"+buildTime+
          "\t"+repositoryTag+"\t"+repositoryModified+"\t"+repositoryTime);
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

  private static class AgentManagerBinderFactory 
    extends BinderFactorySupport {
      public Binder getBinder(Object child) {
        return new AgentManagerBinder(this, child);
      }
      private static class AgentManagerBinder
        extends BinderSupport 
        implements BindingSite
        {
          public AgentManagerBinder(BinderFactory parentInterface, Object child) {
            super(parentInterface, child);
          }
          protected BindingSite getBinderProxy() {
            // do the right thing later
            return this;
          }
        }
    }

  private class NodeProxy implements ContainerAPI {
    public boolean remove(Object o) {return true;}
    public ServiceBroker getServiceBroker() {
      return Node.this.getServiceBroker();
    }
    public void requestStop() {}
  }

  private static class NodeServiceBroker extends ServiceBrokerSupport {}

}

