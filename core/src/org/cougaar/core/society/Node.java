/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.society.ClusterManagementServesCluster;

import org.cougaar.core.component.ComponentDescription;

import java.io.Serializable;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Field;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.security.*;
import java.security.cert.*;

import org.cougaar.core.plugin.AddPlugInMessage;
import org.cougaar.core.plugin.RemovePlugInMessage;
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
* <li>Provide validation services to ensure all access request are valid before processing them.</li>
* </ul>
* <p>
* Usage: java [org.cougaar.core.society.Node] [-f configFileName ] [-h] [-help] [-n nodeName]");
* <ul>
* <li>-f     Configuration file name.  Default = [NAME]. Example test will map to file test.ini</li>
* <li>-h     Print the help message.</li>
* <li>-help  Print the help message.</li>
* <li>-n     Name. Allows you to name the Node.  Default = Computer's name </li>
* <li>-p     Port. allows you to specifcy a port number to run nameserver on.  Default = random </li>
* </ul>
**/
public class Node 
implements ArgTableIfc, MessageTransportClient, ClusterManagementServesCluster
{
  public static int NSPortNo;
  public static String NSHost = "127.0.0.1";
  //protected MgmtLP mgmtLP;

  static boolean alwaysSerialize = false;
  static boolean disableRetransmission = false;

  // initialize static vars from system properties.
  static {
    Properties props = System.getProperties();
    if ((Boolean.valueOf(props.getProperty("org.cougaar.message.shortCircuit", "false"))).booleanValue()) {
      System.err.println("Use alwaysSerialize instead of shortCircuit (same functionality, less deceptive name.");
      System.exit(-1);
    }
    alwaysSerialize = (Boolean.valueOf(props.getProperty("org.cougaar.message.alwaysSerialize", "false"))).booleanValue();
    disableRetransmission = Boolean.getBoolean("org.cougaar.core.cluster.persistence.enable");
  }

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
   **/
  static public void main(String[] args){
    if ("true".equals(System.getProperty("org.cougaar.useBootstrapper", "true"))) {
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  static public void launch(String[] args) {

    printVersion();

    // BEGIN BBN Debugging
    // quickly walk through the args and process any
    // that match -D
    Properties debugProperties = new Properties();
    // elements of args not related to -D values
    List revisedArgs = new ArrayList();
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-D")) {
        debugProperties.put(args[i].substring(2), Boolean.TRUE);
      } else {
        // process a non -D argument
        revisedArgs.add(args[i]);
      }
    }

    // now, swap in the revised Argument listing
    String[] replacementArgs = new String[revisedArgs.size()];
    for (int i = 0; i < revisedArgs.size(); i++) {
      replacementArgs[i] = (String)revisedArgs.get(i);
    }
    args = replacementArgs;

    Node myNode = null;
    ArgTable myArgs = new ArgTable(args);

    // <START_PLUGIN_JAR_VALIDATION>
    String validateJars = (String)myArgs.get(SIGNED_PLUGIN_JARS);
    if (validateJars == null) {
      // not validating
    } else if (validatePluginJarsByStream()) {
      // validation succeeded
    } else {
      throw new RuntimeException(
          "Error!  Found unsigned jars in plugin directory!");
    }
    // </START_PLUGIN_JAR_VALIDATION>

    String nodeName = (String) myArgs.get(NAME_KEY) ;
    java.util.Properties props = System.getProperties();

    // keep track of the node name we are using
    props.put("org.cougaar.core.society.Node.name", nodeName);

    String config = (String) myArgs.get(CONFIG_KEY);
    if (config != null) {
      props.put("org.cougaar.config", config);
      System.err.println("Set config to "+config);
    }

    String cs = (String) myArgs.get(CS_KEY);
    if (cs != null && cs.length()>0) {
      props.put("org.cougaar.config.server", cs);
      System.err.println("Using ConfigurationServer at "+cs);
    }

    String ns = (String) myArgs.get(NS_KEY);
    if (ns != null && ns.length()>0) {
      props.put("org.cougaar.name.server", ns);
      System.err.println("Using NameServer at "+ns);
    }

    String port = (String) myArgs.get(PORT_KEY);
    if (port != null && port.length()>0) {
      props.put("org.cougaar.name.server.port", port);
      System.err.println("Using NameServer on port " + port);
    }

    // try block to ensure we catch all exceptions and exit gracefully
    try{
      myNode = new Node(myArgs);
      myNode.initNode();
      // done with our job... quietly finish.
    } catch (Exception e) {
      System.out.println(
          "Caught an exception at the highest try block.  Exception is: " +
          e );
      e.printStackTrace();
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


  /** Reference to the ArgTable used to start this Clsuter **/
  private ArgTable theArgs;
  /** Reference contatinment of the SetOfObject of Clusters **/
  private Vector theClusters = new Vector();
  /** Reference to the flag for simulating the start of the testing cycle test **/
  private boolean theTested = false;
  /** Address and port number of this machine as provided by the cl args **/
  private static Vector theAddress;

  /**
   *   Node constructor with command line arguments for port number.  
   *    Reserved fore use of the SANode to specify  a port number to the AlpServer. 
   *     Then calls the standard consructor
   *   <p><PRE>
   *   PRE CONDITION:   New key word used to construct object
   *   POST CONDITION:  Object created in the heap
   *   INVARIANCE:
   *   </PRE>
   *    @param someArgs The arg table constucted from the command line agument list.
   *   @param  The String array conataining all of the arguments passed to the main method
   *   from the command line
   *   @return Node Refernce to the object just created by the new keyword
   *    @exception UnknownHostException IF the host can not be determined
   **/
  public Node(ArgTable someArgs, Integer port) throws UnknownHostException {
    super();
    setArgs(someArgs);
  }

  public Node(ArgTable someArgs) throws UnknownHostException {
    setArgs(someArgs);
  }

  /**
   *   This method cleans up the application and releases all resources.  Called as part of a shutdown
   *   procedure or a gracefull exit due to exception.
   *   <p><PRE>
   *   PRE CONDITION:    Node exiting alp system
   *   POST CONDITION:   Resouces released and graceful exit accomplished
   *   INVARIANCE:
   *   </PRE>
   **/
  protected void cleanup(){
  }

  /**
   *   Returns name of computer platform as an String version of an IPAddress
   *   <p><PRE>
   *   PRE CONDITION:   Locates InetAddress for node
   *   POST CONDITION:  converts InetAddress to an IPAddress string represntation
   *   INVARIANCE:      Does not chnage IPAddress for node
   *   </PRE>
   *   @return String The current value of the IPAddress as a string
   */
  public String findIPAddress() throws UnknownHostException {
    return InetAddress.getLocalHost().toString();
  }

  /**
   *   Returns name of computer platform as an String version of an IPAddress
   *   <p><PRE>
   *   PRE CONDITION:   COUGAAR server started
   *   POST CONDITION:  address located and returned
   *   INVARIANCE:
   *   </PRE>
   *   @return String the network name for the platform as a string
   */
  public String findAddress() {
    return NSHost;
  }


  /**
   *   Returns Domain Name Services (DNS) name of computing platform
   *   <p><PRE>
   *   PRE CONDITION:   NA
   *   POST CONDITION:  If localHost call is succesfful then will retunr the host name as
   *           a string else throws an UnknownHostException
   *   INVARIANCE:
   *   </PRE>
   *   @return String the string object containing the local DNS name
   */
  public String findHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
  }

  /**
   *   Accessor method for theArgs
   *   <p>
   *   @return ArgTable  The container that conatins references to all theArgs 
   **/
  protected final ArgTable getArgs() {
    return theArgs; 
  }

  /**
   * Accessor method for theClusters
   * <p>
   * @return A Vector that contains references to all the cluster objects
   **/
  public final Vector getClusters() {
    return 
      ((theClusters != null) ? 
       (theClusters) :
       new Vector(0));
  }

  public final synchronized List getClusterIdentifiers() {
    int n = ((theClusters != null) ? theClusters.size() : 0);
    List l = new ArrayList(n);
    for (int i = 0; i < n; i++) {
      ClusterServesClusterManagement ci = 
        (ClusterServesClusterManagement)theClusters.elementAt(i);
      l.add(ci.getClusterIdentifier());
    }
    return l;
  }

  /** Refernece containing the Messenger **/
  private transient MessageTransportServer theMessenger = null;

  /**
   *   Accessor method for theMessenger
   *   <p>
   *   @return Messenger The threaded messenger component of the node as referenced by theMessenger
   **/
  protected MessageTransportServer getMessenger() { 
      // Nb:  One proxy for all requestors!
    return theMessenger; 
  }

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
  protected void initNode() throws UnknownHostException {
    // Command-line script uses "-n MyNode" and lets the filename default
    //   to "MyNode.ini".
    //
    // Remote console always specifies name as a property and the filename
    //   as an argument.
    String name = null;
    String filename = (String)getArgs().get(FILE_KEY);
    if (getArgs().containsKey(NAME_KEY)) {
      name = (String)getArgs().get(NAME_KEY);
      if (filename == null) {
        filename = name+".ini";
      }
    } else {
      name = System.getProperty("org.cougaar.node.name");
    }
    if (name == null) {
      name = findHostName();
    }

    NodeIdentifier nid = new NodeIdentifier(name);
    setNodeIdentifier(nid);

    // load node properties
    ComponentDescription[] nodeDescs = null;
    if (filename != null) {
      try {
        // currently assumes ".ini" format
        InputStream in = getConfigFinder().open(filename);
        nodeDescs = INIParser.parse(in, "Node");
      } catch (Exception e) {
        System.err.println(
            "Unable to parse node \""+name+"\" file \""+filename+"\"");
        e.printStackTrace();
      }
    }

    if (getArgs().containsKey(REGISTRY_KEY) || 
        getArgs().containsKey(LOCAL_KEY)) {
      Communications.getInstance().startNameServer();
    }

    // set up the message handler and register this Node
    initTransport();  

    registerExternalNodeController();

    // load the clusters
    //
    // once bulk-add ComponentMessages are implements this can
    //   be done with "this.receiveMessage(compMsg)"
    add(nodeDescs);

    //mgmtLP = new MgmtLP(this); // MTMTMT turn off till RMI namespace works
  }


    // **** QUO *****
    // Change this to create (or find?) a MessageTransportManager as the
    // value of theMessenger.
  private void initTransport() {
    String name = getIdentifier();
//      if (alwaysSerialize) {
//        theMessenger = new PipedMessageTransport(name);
//      } else {
//        theMessenger = Communications.getInstance().startMessageTransport(name);
//      }
    theMessenger = Communications.getInstance().startMessageTransport(name);
    Communications.setDefaultMessageTransport(theMessenger);
    // theMessenger.setDisableRetransmission(disableRetransmission);
    System.err.println("Started "+theMessenger);
    theMessenger.registerClient(this);
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
    String rmiHost = (String)getArgs().get(CONTROL_KEY);
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
    String srmiPort = (String)getArgs().get(CONTROL_PORT_KEY);
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

  /**
   * Create a Cluster from a ComponentDescription.
   * <p>
   * This should be moved into the future "AgentManager".
   */
  protected ClusterServesClusterManagement createCluster(
      ComponentDescription desc) {

    // check the cluster classname
    String clusterClassname = desc.getClassname();

    // load an instance of the cluster
    //
    // FIXME use the "desc.getCodebase()" and other arguments
    ClusterServesClusterManagement cluster;
    try {
      Class clusterClass = Class.forName(clusterClassname);
      Object clusterInstance = clusterClass.newInstance();
      if (!(clusterInstance instanceof ClusterServesClusterManagement)) {
        throw new ClassNotFoundException();
      }
      cluster = (ClusterServesClusterManagement)clusterInstance;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException(
          "Unable to load agent class: \""+clusterClassname+"\"");
    }

    // the parameter should be the cluster name
    String clusterid;
    try {
      clusterid = (String)((List)desc.getParameter()).get(0);
    } catch (Exception e) {
      clusterid = null;
    }
    if (clusterid == null) {
      throw new IllegalArgumentException(
          "Agent specification lacks a String \"name\" parameter");
    }
   
    // set the ClusterId
    ClusterIdentifier cid = new ClusterIdentifier(clusterid);
    cluster.setClusterIdentifier(cid);

    //move the cluster to the intialized state
    BindingUtility.activate(cluster,new NodeProxy(), null);
    if (cluster.getState() != GenericStateModel.ACTIVE) {
      System.err.println("Cluster "+cluster+" is not Active!");
    }

    return cluster;
  }
  
  private class NodeProxy implements AgentBindingSite, ClusterManagementServesCluster, BindingSite {
    // ClusterManagementServesCluster
    public Object instantiateBean(String className) throws ClassNotFoundException {
      return Node.this.instantiateBean(className);
    }
    public Object instantiateBean(ClassLoader classLoader, String className) throws ClassNotFoundException {
      return Node.this.instantiateBean(classLoader, className);
    }
    public void logEvent( Object anEvent ) { Node.this.logEvent(anEvent);}
    public void sendMessage(Message message) throws MessageTransportException {
      Node.this.sendMessage(message);
    }
    public MessageTransportServer getMessageTransportServer() {return Node.this.getMessageTransportServer(); }
    public String getName() {return Node.this.getName(); }

    // BindingSite
    public ServiceBroker getServiceBroker() {return null; }
    public void requestStop() {}
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
   * This should be moved into the future "AgentManager".
   */
  protected void add(ComponentDescription[] descs) {

    int nDescs = ((descs != null) ? descs.length : 0);

    System.err.print("Creating Clusters:");
    List clusters = new ArrayList(nDescs);
    for (int i = 0; i < nDescs; i++) {
      try {
        ComponentDescription desc = descs[i];
        String insertionPoint = desc.getInsertionPoint();
        if (!("Node.AgentManager.Agent".equals(insertionPoint))) {
          // fix to support non-agent components
          throw new IllegalArgumentException(
              "Currently only agent ADD is supported, not "+
              insertionPoint);
        }
        ClusterServesClusterManagement cluster = createCluster(desc);
        ClusterIdentifier cid = cluster.getClusterIdentifier();
        String cname = cid.toString();
        System.err.print("\n\t"+cname);
        clusters.add(cluster);
      } catch (Exception e) {
        System.err.println(
            "\nUnable to load cluster["+i+"]: "+e);
        e.printStackTrace();
      }
    }

    System.err.print("\nLoading Plugins:");
    int nClusters = clusters.size();
    for (int i = 0; i < nClusters; i++) {
      try {
        ClusterServesClusterManagement cluster = 
          (ClusterServesClusterManagement)clusters.get(i);
        ClusterIdentifier cid = cluster.getClusterIdentifier();
        String cname = cid.toString();
        // read from file
        System.err.print("\n\t"+cname);
        // parse the cluster properties
        // currently assume ".ini" files
        InputStream in = getConfigFinder().open(cname+".ini");
        ComponentDescription[] cDescs = 
          INIParser.parse(in, "Node.AgentManager.Agent");

        // add the plugins and other cluster components
        //
        // FIXME could benefit from a bulk-add message
        for (int j = 0; j < cDescs.length; j++) {
          ComponentMessage addCM = 
            new ComponentMessage(
                cid,
                cid,
                ComponentMessage.ADD,
                cDescs[j]);
          // bypass the message system to initialize the cluster
          cluster.receiveMessage(addCM);
        }

        // tell the cluster to proceed.
        ClusterInitializedMessage m = new ClusterInitializedMessage();
        m.setOriginator(cid);
        m.setTarget(cid);
        cluster.receiveMessage(m);
      } catch (Exception e) {
        System.err.println(
            "\nUnable to add cluster["+i+"] child omponents: "+e);
        e.printStackTrace();
        clusters.set(i, null);
      }
    }

    System.err.println("\nPlugins Loaded.");

    // save these clusters as children of the node
    addClusters(clusters);

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
      int n = clusters.size();
      for (int i = 0; i < n; i++) {
        ClusterServesClusterManagement ci = 
          (ClusterServesClusterManagement)clusters.get(i);
        if (ci != null) {
          ClusterIdentifier ciId = ci.getClusterIdentifier();
          try {
            eListener.handleClusterAdd(eController, ciId);
          } catch (Exception e) {
            // lost listener?  should we kill this Node?
            System.err.println(
                "Lost connection to external listener? "+e.getMessage());
            try {
              eController.setExternalNodeActionListener(null);
            } catch (Exception e2) {
            }
            break;
          }
        }
      }
    }
  }

  /**
   * Modifier method for theArgs
   * @param aTable The ArgTable object that is used to modify theArgs
   **/
  protected final void setArgs(ArgTable aTable) {
    theArgs = aTable; 
  }

  private final synchronized void addClusters(List addedClusters) { 
    int n = addedClusters.size();
    if (theClusters == null) {
      theClusters = new Vector(addedClusters.size());
    }
    for (int i = 0; i < n; i++) {
      Object oi = addedClusters.get(i);
      if (oi != null) {
        theClusters.addElement(oi);
      }
    }
  }

  private final synchronized void addCluster(
      ClusterServesClusterManagement cluster) {
    if (theClusters == null) {
      theClusters = new Vector();
    }
    theClusters.addElement(cluster);
  }

  public MessageAddress getMessageAddress() {
    return myNodeIdentity_;
  }

  public void receiveMessage(Message m) {
    try {
      if (m instanceof ComponentMessage) {
        ComponentMessage cm = (ComponentMessage)m;
        ComponentDescription desc = cm.getComponentDescription();
        int operation = cm.getOperation();
        if (operation == ComponentMessage.ADD) {
          add(desc);
        } else {
          // not implemented yet!  will be okay once Node is a Container
          throw new UnsupportedOperationException(
            "Unsupported ComponentMessage: "+m);
        }
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


  /** Method for construction of all the contained beans.
   *    Calls the chained method with a NULL value for the ClassLoader.
   *    <p>
   *    This is the method that contains the hook for swapping ClassLoaders prior to calling the chained method.
   *    <p>
   *    @param  aBean  The String object that contains the fully dot notated descriptin of the bean to construct
   *    @return Object The refernce to the new bean as an Object. It is the requestors responsibility to check the type before casting.
   *    @exception Exception If there is a problem instantiating the
   *    bean.  Will most often be java.lang.ClassNotFoundException or 
   *    java.io.IOException (the exceptions thrown by Beans.instantiate());
   *   @see java.beans.Beans
   **/
  public Object instantiateBean( String aBean ) throws ClassNotFoundException {
    return instantiateBean( null, aBean);
  }

  /** Overloaded method provides the interface for construction of the contained beans.
   *    Uses the static interface in the java.bean.Beans class to create a bean.
   *    <p>
   *    In its simplest form this method instantiates and returns the bean for the requesting component.
   *    In its most abstract form this will access a threaded BeanFactory that will validate all 
   *    bean dependecies and versioning and then create the Bean using the proper ClassLoader.
   *   <p><PRE>
   *   PRE CONDITION:    Component requests the construction of a specific bean
   *   POST CONDITION:   Bean is instantiated and the reference is returned to the requestor
   *   INVARIANCE:       
   *   </PRE>
   *    @param  aBean  The String object that contains the fully dot notated descriptin of the bean to construct
   *    @param  sClassLoader    The ClassLoader to use in te construction process.
   *    @return Object The refernce to the new bean as an Object. It is the requestors responsibility to check the type before casting.
   *    @exception IOException if an I/O error occurs.
   *    @exception ClassNotFoundException if the class of a serilized object could not be found.
   *    @exception Exception If there is a problem instantiating the
   *    bean.  Will most often be java.lang.ClassNotFoundException or 
   *    java.io.IOException (the exceptions thrown by Beans.instantiate());
   *   @see java.beans.Beans
   **/
  public Object instantiateBean( ClassLoader aClassLoader, String aBean ) throws ClassNotFoundException {
    try {
      return Beans.instantiate( aClassLoader, aBean );
    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
      throw new ClassNotFoundException(aBean);
    }
  }

  public MessageTransportServer getMessageTransportServer() {
    return getMessenger();
  }

  public void logEvent(Object event) {
    try {
      System.out.println(getIdentifier()+": "+event);
    } catch (Exception e) {
      System.err.println("logEvent caught: "+e);
      e.printStackTrace();
    }
  }

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

}

