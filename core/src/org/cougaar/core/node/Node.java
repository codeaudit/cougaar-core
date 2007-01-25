/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.cougaar.bootstrap.Bootstrapper;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.Configuration;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This component is the root component of the
 * <a href="http://www.cougaar.org">Cougaar Agent Architecture</a>,
 * containing the {@link #main} method. 
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
 *
 * @property org.cougaar.filename
 *   The file name (.ini) for starting this Node, which defaults to 
 *   (<em>org.cougaar.node.name</em>+".ini") if both
 *   <em>org.cougaar.filename</em> and
 *   <em>org.cougaar.experiment.id</em> are not specified.  If this
 *   property is specified then <em>org.cougaar.experiment.id</em>
 *    must not be specified.
 *
 * @property org.cougaar.experiment.id
 *   The experiment identifier for running this Node; see 
 *   <em>org.cougaar.filename</em> for details.
 *
 * @property org.cougaar.install.path
 *   The <em>base</em> path for finding jar and configuration files.
 *
 * @property org.cougaar.validate.jars
 *   If <em>true</em>, will check the certificates on the 
 *   (<em>org.cougaar.install.path</em>+"/plugin/*.jar") files.
 *   Defaults to <em>false</em>.
 *
 * @property org.cougaar.security.certificate
 *   The path of the <em>org.cougaar.install.path</em> for finding
 *   the <em>org.cougaar.validate.jars</em> certificates.
 */
public class Node
extends ContainerSupport
{
  public static final String INSERTION_POINT = "Node";

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
    if (SystemProperties.getBoolean(
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
  public static void launch(Object[] args) {
    // convert any command-line args to System Properties
    loadSystemProperties();
    if (!setSystemProperties(args)) {
      return; // must be "--help"
    }

    // display the version info
    printVersion(true);

    // check for valid plugin jars
    maybeValidateJars();

    // create the root service broker and binding site
    final ServiceBroker rootsb = new ServiceBrokerSupport() {};
    BindingSite rootbs = 
      new BindingSite() {
        public ServiceBroker getServiceBroker() { return rootsb; }
        public void requestStop() { }
      };

    // import external container services, if any
    importServices(rootsb, args);

    // try block to ensure we catch all exceptions and exit gracefully
    try {
      Node myNode = new Node();
      // this will call our "load()" method
      BindingUtility.activate(myNode, rootbs, rootsb);
      // done with our job... quietly finish.
    } catch (Throwable e) {
      System.out.println(
          "Caught an exception at the highest try block."+
          "  Exception is: " +
          e );
      e.printStackTrace();
    }
  }

  /**
   * Import services passed in by our {@link #launch} args.
   * <p>
   * Examples of supported args[<i>index</i>] values:<pre>
   *   // just like "rootsb.addService(clazz, sp)"
   *   new Object[] { MyService.class, new ServiceProvider() {..} }
   *
   *   // use a boilerplate ServiceProvider service wrapper
   *   new Object[] { MyService.class, new MyService() {..} }
   *
   *   // use reflection to avoid most classloader problems
   *   new Object[] { "com.foo.MyService", new InvocationHandler() {..} }
   * </pre>
   */
  public static void importServices(ServiceBroker rootsb, Object[] args) {
    for (int i = 0; i < args.length; i++) {
      // look for an Object[2]
      Object oi = args[i];
      if (oi instanceof String) continue;
      if (!(oi instanceof Object[])) {
        throw new RuntimeException(
            "Expecting a String or Object[], not "+oi);
      }
      Object[] oa = (Object[]) oi;
      if (oa.length != 2) {
        throw new RuntimeException(
            "Expecting an Object[2], not ["+oa.length+"]");
      }
      Object cl_obj = oa[0];
      Object sv_obj = oa[1];

      // get the service class
      final Class clazz;
      if (cl_obj instanceof Class) {
        clazz = (Class) cl_obj;
      } else if (cl_obj instanceof String) {
        try {
          clazz = Class.forName((String) cl_obj);
        } catch (Exception e) {
          throw new RuntimeException("Unknown classname "+cl_obj, e);
        }
      } else {
        throw new RuntimeException(
            "Expecting a Class or String, not "+cl_obj);
      }

      // get the service provider
      ServiceProvider sp;
      if (sv_obj instanceof ServiceProvider) {
        sp = (ServiceProvider) sv_obj;
      } else {
        final Object service;
        if (sv_obj instanceof InvocationHandler) {
          // create a reflective proxy
          service = 
            Proxy.newProxyInstance(
                rootsb.getClass().getClassLoader(),
                new Class[] {clazz},
                ((InvocationHandler) sv_obj));
        } else {
          // it's okay of the object doesn't implement "Service", since it
          // might be implement in code that lacks our Service API.
          service = sv_obj;
        }

        sp = new ServiceProvider() {
          public Object getService(
              ServiceBroker sb, Object requestor, Class serviceClass) {
            return (clazz.isAssignableFrom(serviceClass) ? service : null);
          }
          public void releaseService(
              ServiceBroker sb, Object requestor,
              Class serviceClass, Object service) {
          }
        };
      }

      // add the service
      rootsb.addService(clazz, sp);
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
  private static boolean setSystemProperties(Object[] args) {
    // separate the args into "-D" properties and normal arguments
    for (int i = 0; i < args.length; i++) {
      Object oi = args[i];
      if (!(oi instanceof String)) continue; 
      String argi = (String) oi;
      if (argi.startsWith("-D")) {
        // add a "late" system property
        int sepIdx = argi.indexOf('=');
        if (sepIdx < 0) {
          SystemProperties.setProperty(argi.substring(2), "");
        } else {
          SystemProperties.setProperty(
              argi.substring(2, sepIdx), argi.substring(sepIdx+1));
        }
      } else if (argi.equals("-n")) {
        // old "-n node" pattern
        String name = (String) args[++i];
        SystemProperties.setProperty("org.cougaar.node.name", name);
        Logger logger = Logging.getLogger(Node.class);
        if (logger.isInfoEnabled()) {
          logger.info(
              "Set node name to "+name+
              "\nThe command line format \"-n "+name+"\" has been deprecated"+
              "\nPlease use \"-Dorg.cougaar.node.name="+name+"\"");
        }
      } else if (argi.equals("-c")) {
        // ignore
        Logger logger = Logging.getLogger(Node.class);
        if (logger.isInfoEnabled()) {
          logger.info( "Ignoring unused command-line argument \"-c\"");
        }
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
      SystemProperties.getProperty("org.cougaar.core.node.properties");
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
          if (SystemProperties.getProperty(key) == null) {
            try {
              String value = p.getProperty(key);
              value = Configuration.resolveValue(value);
              SystemProperties.setProperty(key, value);
            } catch (RuntimeException re) {
              re.printStackTrace();
            }
          }
        }
      } // if cip not null
    } catch (Exception e) {
      // failed to open input stream
      // or canonicalizeElement had a MalformedURLException
      // or...

      // Failed to loadSystemProperties
      //e.printStackTrace();
      //      System.err.println("Failed to loadSystemProperties from " + u, e);
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

    // prevent future changes to the underlying system properties map
    SystemProperties.finalizeProperties();

    // add the agent manager, which loads the node-agent
    add(new ComponentDescription(
          "org.cougaar.core.agent.AgentManager",
          "Node.Component",
          "org.cougaar.core.agent.AgentManager",
          null,  //codebase
          null,  //params
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH));
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
    } catch (Exception e) {
      // Failed to get version info, reflection problem
    }

    Logger logger = Logging.getLogger(Node.class);
    if (logger.isInfoEnabled()) {
      logger.info(
          "COUGAAR "+version+" "+buildTime+
          " "+repositoryTag+" "+repositoryModified+
          " "+repositoryTime);
    }

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
      String vminfo = SystemProperties.getProperty("java.vm.info");
      String vmv = SystemProperties.getProperty("java.vm.version");
      System.out.println("VM: JDK "+vmv+" ("+vminfo+")");
      String os = SystemProperties.getProperty("os.name");
      String osv = SystemProperties.getProperty("os.version");
      System.out.println("OS: "+os+" ("+osv+")");
    }
  }

  //
  // old jar validation
  // will likely be removed/refactored in the future
  //

  private static void maybeValidateJars() {
    // check for valid plugin jars
    boolean validateJars = SystemProperties.getBoolean("org.cougaar.validate.jars");
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

  /** Returns true if all plugin jars are signed, else false */
  private static boolean validatePluginJarsByStream() {
    String jarSubdirectory = "plugins";
    String installpath = SystemProperties.getProperty("org.cougaar.install.path");
    String defaultCertPath = "configs" + File.separatorChar + "common"
      + File.separatorChar + "alpcertfile.cer";

    String certPath = SystemProperties.getProperty("org.cougaar.security.certificate",
        defaultCertPath);

    try{
      String files[] = searchForJars(jarSubdirectory, installpath);
      Vector cache = new Vector();
      File myCertFile = new File(installpath + File.separatorChar + certPath );

      Logger logger = Logging.getLogger(Node.class);
      if (logger.isInfoEnabled()) {
        logger.info("Using certificate from: "+myCertFile);
      }

      java.security.cert.Certificate myCert = null;
      myCert = getAlpineCertificate(myCertFile);
      for (int i=0; i<files.length; i++) {
        String filename = files[i];

        File f = new File(
            installpath+File.separatorChar+
            jarSubdirectory+File.separatorChar+filename);

        if (logger.isInfoEnabled()) {
          logger.info("Testing Plugin jar "+f);
        }
      
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

  /** look for jar files in org.cougaar.install.path/subdir */
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
