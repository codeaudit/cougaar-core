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
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.cougaar.bootstrap.Bootstrapper;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.Configuration;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logging;

/**
 * The Node is the root component of the
 * <a href="http://www.cougaar.org">Cougaar Agent Architecture</a>.
 * <p>
 * Usage:<pre>
 *    <tt>java [props] org.cougaar.core.node.Node [props] [-help]</tt>
 * </pre> where the "props" are "-D" System Properties, such as:<pre>
 *    "-Dorg.cougaar.node.name=NAME" -- name of the Node
 * </pre>
 * <p>
 * A node refers to a per-JVM component that contains the Cougaar
 * agents and per-JVM services.  The primary job of the node is to:
 * <ul>
 *   <li>Provide the initial launch methods</li>
 *   <li>Initialize the system properties</li>
 *   <li>Create the root ServiceBroker for the component model</li>
 *   <li>Create the NodeIdentificationService</li>
 *   <li>Create the initial ComponentInitializerService</li>
 *   <li>Create the AgentManager</li>
 *   <li>Create the NodeAgent, which in turn creates the other
 *       agents for this node</li>
 * </ul>
 * <p>
 *
 * @property org.cougaar.node.name
 *   The (required) name for this Node.
 *
 * @property org.cougaar.core.node.InitializationComponent
 *   Used to specify which service component to use.  Can be passed
 *   in short hand (<em>DB</em>, <em>XML</em>, <em>File</em>) or as
 *   a fully specified class:
 *   <em>org.cougaar.core.node.DBComponentInitializerServiceComponent</em>
 * @property org.cougaar.filename
 *   The file name (.ini) for starting this Node, which defaults to 
 *   (<em>org.cougaar.node.name</em>+".ini") if both
 *   <em>org.cougaar.filename</em> and
 *   <em>org.cougaar.experiment.id</em> are not specified.  If this
 *   property is specified then <em>org.cougaar.experiment.id</em>
 *    must not be specified.
 * @property org.cougaar.experiment.id
 *   The experiment identifier for running this Node; see 
 *   <em>org.cougaar.filename</em> for details.
 *
 * @property org.cougaar.install.path
 *   The <em>base</em> path for finding jar and configuration files.
 * @property org.cougaar.validate.jars
 *   If <em>true</em>, will check the certificates on the 
 *   (<em>org.cougaar.install.path</em>+"/plugin/*.jar") files.
 *   Defaults to <em>false</em>.
 * @property org.cougaar.security.certificate
 *   The path of the <em>org.cougaar.install.path</em> for finding
 *   the <em>org.cougaar.validate.jars</em> certificates.
 *
 * @property org.cougaar.core.node.ignoreRehydratedAgentList
 *   Ignore the list of agents from the rehydrated state of the
 *   NodeAgent, if any. Defaults to false. Set to true to disable
 *   this feature and always use the list of agents from the
 *   ComponentInitializerService.
 * </pre>
 */
public class Node
extends ContainerSupport
{
  public static final String INSERTION_POINT = "Node";
  public static final String FILENAME_PROP = "org.cougaar.filename";
  public static final String EXPTID_PROP = "org.cougaar.experiment.id";
  public static final String INITIALIZER_PROP = 
    "org.cougaar.core.node.InitializationComponent";
  public static final String IGNORE_REHYDRATED_AGENT_LIST_PROP =
    "org.cougaar.core.node.ignoreRehydratedAgentList";
  public static final String NODE_AGENT_CLASSNAME_PROPERTY =
    "org.cougaar.core.node.classname";

  private MessageAddress myNodeIdentity_ = null;

  /**
   * Node entry point.
   * <p>
   * If org.cougaar.useBootstrapper is true, will search for
   * installed jar files in order to load all classes.  Otherwise,
   * will rely solely on classpath.
   *
   * @see #launch(String[])
   */
  // @deprecated
  public static void main(String[] args){
    if (PropertyParser.getBoolean(
          "org.cougaar.useBootstrapper", true)) {
      Logging.getLogger(Node.class).warn(
          "-Dorg.cougaar.useBootstrapper is deprecated."+
          "  Invoke Bootstrapper directly.");
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  /**
   * The real entry-point for Node, generally invoked via 
   * the bootstrapper.
   *
   * @see org.cougaar.bootstrap.Bootstrapper
   */
  public static void launch(String[] args) {
    // convert any command-line args to System Properties
    loadSystemProperties();
    if (!setSystemProperties(args)) {
      return; // must be "--help"
    }

    // display the version info
    printVersion(true);

    // check for valid plugin jars
    maybeValidateJars();

    // create root service broker and binding site
    final ServiceBroker rootsb = 
      new ServiceBrokerSupport() {
      };
    BindingSite rootbs = 
      new BindingSite() {
        public ServiceBroker getServiceBroker() {
          return rootsb;
        }
        public void requestStop() {
        }
      };

    // try block to ensure we catch all exceptions and exit gracefully
    try {
      Node myNode = new Node();
      BindingUtility.activate(myNode, rootbs, rootsb);
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

  /**
   * Parse and load system properties from a well-known file, as
   * defined by a system property
   * (default "$INSTALL/configs/common/system.properties")
   * it will only load properties which do not already have a value.
   * <p>
   * Property values are interpreted with Configuration.resolveValue to
   * do installation substitution.
   *
   * @property org.cougaar.core.node.properties
   * @note The property org.cougaar.core.node.properties must be
   *   defined as a standard java -D argument, as it is evaluated
   *   extremely early in the Node boot process.  
   */
  private static void loadSystemProperties() {
    String u = 
      System.getProperty("org.cougaar.core.node.properties");
    if (u == null) {
      u = "$INSTALL/configs/common/system.properties";
    }

    try {
      URL cip = Configuration.canonicalizeElement(u);
      if (cip != null) {
        Properties p = new Properties();
        InputStream in = cip.openStream();
        try {
          p.load(in);
        } finally {
          in.close();
        }
        for (Iterator it = p.keySet().iterator();
            it.hasNext();
            ) {
          String key = (String) it.next();
          if (System.getProperty(key) == null) {
            try {
              String value = p.getProperty(key);
              value = Configuration.resolveValue(value);
              System.setProperty(key, value);
            } catch (RuntimeException re) {
              re.printStackTrace();
            }
          }
        }
      }
    } catch (Exception e) {
      //e.printStackTrace();
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ServiceBroker createChildServiceBroker(BindingSite bs) {
    // node uses the root service broker
    return getServiceBroker();
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }

  /**
   * This method provides the initialization of a Node.
   */
  public void load() {
    super.load();

    // get the node name
    String name = System.getProperty("org.cougaar.node.name");
    if (name == null) {
      try {
        name = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException uhe) {
      }
      if (name == null) {
        throw new IllegalArgumentException("Node name not specified");
      }
    }
    MessageAddress nid = MessageAddress.getMessageAddress(name);

    // sb is our service broker (for *all* agents)
    ServiceBroker sb = getServiceBroker();

    // we need to make this available here so others can get it.
    sb.addService(NodeIdentificationService.class,
		  new NodeIdentificationServiceProvider(nid));

    // we need the initializerservice so that AgentManager can load external binders
    // This will use the CSMART DB (advertising the DBInitializerService)
    // if the experiment_id system property was set, and some other
    // initializer was not selected.
    // Otherwise, INI files are used (and no DBInitializerService is provided)
    // however, users may specify, for example, an XML initializer
    ComponentDescription compInitDesc = 
      new ComponentDescription(
          "component-init",
          Node.INSERTION_POINT+".Init",
          getInitializerComponentName(),
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
    List naParams = new ArrayList(4);
    naParams.add(nid);
    naParams.add(sb);
    naParams.add(agentManager);
    naParams.add(
        Boolean.valueOf(
          System.getProperty(IGNORE_REHYDRATED_AGENT_LIST_PROP)));
    String naClass = System.getProperty(
        NODE_AGENT_CLASSNAME_PROPERTY,
        NodeAgent.class.getName());
    ComponentDescription naDesc = 
      new ComponentDescription(
          name,
          Agent.INSERTION_POINT,
          naClass,
          null,  //codebase
          naParams,
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH);
    agentManager.add(naDesc);

    // may need to wait for the NodeAgent to come all the way up.
  }

  // Select the ComponentInitializer to use
  private String getInitializerComponentName() {
    String component = System.getProperty(INITIALIZER_PROP);

    if (component == null) {
      component = getOldComponentString();
      System.setProperty(INITIALIZER_PROP, component);
    }

    // If full class name not specified, intuit it
    if (component.indexOf(".") < 0) {
      // Build up the name, full name was not specified.
      component = 
        "org.cougaar.core.node." +
        component +
        "ComponentInitializerServiceComponent";
    }
    Logging.getLogger(getClass()).info(
        "Will intialize components from " + component);

    return component;
  }

  // Figure out whether to use Files or CSMART DB for Component initalization
  // if not explicitly specified with -D argument
  private String getOldComponentString() {
    String filename = System.getProperty(FILENAME_PROP);
    String expt = System.getProperty(EXPTID_PROP);
    if ((filename == null) && (expt == null)) {
      // use the default "name.ini"
      Logging.getLogger(getClass()).info(
          "Got no filename or experimentId! Using default File");
      return "File";
    }
    if (filename == null) {
      // use the experiment ID to read from the DB
      Logging.getLogger(getClass()).info(
          "Got no filename, using exptID " + expt);
      return "DB";
    }
    if (expt == null) {
      // use the filename provided
      Logging.getLogger(getClass()).info(
          "Got no exptID, using given filename " + filename);
    }

    return "File";
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

    Logging.getLogger(Node.class).info(
        "COUGAAR "+version+" "+buildTime+
        " "+repositoryTag+" "+repositoryModified+
        " "+repositoryTime);


    if (!(fullFormat)) {
      System.out.println(
          "COUGAAR\t"+version+"\t"+buildTime+
          "\t"+repositoryTag+"\t"+repositoryModified+
          "\t"+repositoryTime);
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

  //
  // old jar validation
  // will likely be removed/refactored in the future
  //

  private static void maybeValidateJars() {
    // check for valid plugin jars
    boolean validateJars = 
      PropertyParser.getBoolean("org.cougaar.validate.jars", false);
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
}
