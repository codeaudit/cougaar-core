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
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.MessageTransportException;

import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.society.ClusterManagementServesCluster;

import java.io.Serializable;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.security.*;
import java.security.cert.*;

import org.cougaar.core.plugin.AddPlugInMessage;
import org.cougaar.core.plugin.RemovePlugInMessage;
import org.cougaar.core.cluster.ClusterInitializedMessage;

import org.cougaar.core.cluster.ClusterIdentifier;

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
public class Node implements ArgTableIfc, ClusterManagementServesCluster
{
  public static int ALPPortNo;
  public static String ALPAddress = "127.0.0.1";
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



  /** Reference containing the Profiler **/
  private NodeProfiler theProfiler = null;
  private String id = null;
  public String getIdentifier() { return id; }

  public void setIdentifier(String s) { id = s; }

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
    Vector revisedArgs = new Vector(); // elements of args not related to -D values
    for ( int i=0; i<args.length; i+=1 ){
      if (args[i].startsWith("-D")) {
        debugProperties.put(args[i].substring(2), Boolean.TRUE);
      } else {
        // process a non -D argument
        revisedArgs.addElement(args[i]);
      }
    }

    // now, swap in the revised Argument listing
    String[] replacementArgs = new String[revisedArgs.size()];
    for ( int i=0; i<revisedArgs.size(); i+=1 )
      replacementArgs[i]=(String)revisedArgs.elementAt(i);
    args = replacementArgs;

    Node myNode = null;
    ArgTable myArgs = new ArgTable(args);

    // <START_PLUGIN_JAR_VALIDATION>
    String validateJars = (String)myArgs.get(SIGNED_PLUGIN_JARS);
    if( validateJars != null) {
      {
        boolean isValid = validatePluginJarsByStream();
        if( isValid == false ) {
          throw new RuntimeException("Error!  Found unsigned jars in plugin directory!");
        }
      }
    }
    // </START_PLUGIN_JAR_VALIDATION>

    String nodeName = (String) myArgs.get(NAME_KEY) ;
    java.util.Properties props = System.getProperties();

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
    }
    catch(Exception e){
      System.out.println("Caught an exception at the highest try block.  Exception is: " + e );
      e.printStackTrace();
    }
  }


  /** Returns true if all plugin jars are signed, else false **/
  private static boolean validatePluginJarsByStream()
  {
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
      for(int i=0; i<files.length; i++)
        {
          String filename = files[i];

          File f = new File(
                            installpath+File.separatorChar
                            +jarSubdirectory+File.separatorChar+filename);
          System.out.println("Testing Plugin Jar ("
                             + f.toString() +")");
          FileInputStream fis = new FileInputStream(f);
          JarInputStream jis = new  JarInputStream( fis );
          try{
            JarEntry je=null;
            while( null != (je = jis.getNextJarEntry()) ) {
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
          while( en.hasMoreElements() )
            {
              JarEntry je = (JarEntry)en.nextElement();
              java.security.cert.Certificate[] certs = je.getCertificates();
              if( certs == null ){
                //  directories are unsigned -- so if jars contain
                //  directory hierarchy structures (ala packages)
                //  these will show up.
                sumUnsigned++;
                //System.out.println("Unsigned Klass:" + je.getName());
              } else {
                int len = certs.length;
                boolean anymatch = false;
                for(int j=0;j<len;j++) {
                  java.security.cert.Certificate cc = certs[j];
                  if( cc.equals(myCert) == true ){ anymatch = true; }
                }
                if( anymatch == true ) {
                  sumMatchCerts++;
                }else {
                  sumUnMatchedCerts++;
                }
                sumSigned++;
              }
              //System.out.println(""+  je.getName() +" certs=" + certs);
            }
          System.out.print("        sigs:(" + sumUnsigned + "," + sumSigned + ")");
          if(sumSigned  == 0 ) System.out.println("...JAR FILE IS UNSIGNED.");
          else System.out.println("...JAR FILE IS SIGNED.");

          System.out.print("        alpcerts:(" + sumMatchCerts + "," + sumUnMatchedCerts + ")");
          if( (sumUnMatchedCerts  == 0) && (sumMatchCerts >0) ) System.out.println("...ALPINE CERTIFIED.");
          else System.out.println("...NOT ALPINE CERTIFIED.");

          /// CRASH THE PARTY IF PLUGINS ARE UNSIGNED...
          if( (sumSigned == 0) || (sumMatchCerts == 0) ) return false;
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
    System.out.println("Searching for plugin jars in directory: " + d.toString());
    if (d.exists() && d.isDirectory())
    {
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

  private static java.security.cert.Certificate getAlpineCertificate(File myCertFile) {

    java.security.cert.Certificate cert = null;
    try{
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
  public Node( ArgTable someArgs, Integer port ) throws UnknownHostException {
    super();
    setArgs( someArgs );
  }
    
  public Node(ArgTable someArgs) throws UnknownHostException {
    setArgs( someArgs );
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
   *   This method is responsible for creating the NodeProfilier class for the node.
   *   <p><PRE>
   *   PRE CONDITION:    Part of the construction  of a Node.
   *   POST CONDITION:   Node creates and contains the Node Access Object.
   *   INVARIANCE:
   *   </PRE>
   *    @todo I ust check the DB for the profile object and if not ther then create it
   **/
  protected void createProfiler(){
    NodeProfiler myProfiler = null;
    if( getArgs().containsKey( FILE_KEY ) )
      myProfiler = new NodeProfiler( getIdentifier(), (String)getArgs().get( FILE_KEY ) );
    else
      myProfiler = new NodeProfiler( getIdentifier() );
                
    theProfiler=myProfiler;
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
   *   PRE CONDITION:   ALP server started
   *   POST CONDITION:  address located and returned
   *   INVARIANCE:
   *   </PRE>
   *   @return String the network name for the platform as a string
   */
  public String findAddress() {
    return ALPAddress;
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
  protected final ArgTable getArgs(){ return theArgs; }

  /**
   *   Accessor method for theClusters
   *   <p>
   *   @return SetOfObject  The container that conatins references to all the cluster objects
   **/
  public final Vector getClusters(){ return theClusters; }

  /** Refernece containing the Messenger **/
  private transient MessageTransport theMessenger = null;

  /**
   *   Accessor method for theMessenger
   *   <p>
   *   @return Messenger The threaded messenger component of the node as referenced by theMessenger
   **/
  protected MessageTransport getMessenger(){ return theMessenger; }

  /**
   * 	Post a message to the destination's message queue
   *	@param aMessage The message object to post into the system.
   **/
  public final void sendMessage( Message aMessage ) throws MessageTransportException {
    theMessenger.sendMessage(aMessage);
  }

  /**
   *    This method provides the initialization of a Node.  This use to be part of the 
   *    Node constructor but is isolated here
   *    <p>
   *    @exception UnknownHostException IF the host can not be determined
   **/  
  protected void initNode() throws UnknownHostException
  {
    String name = null;
    if(getArgs().containsKey(NAME_KEY)) {
      name = (String)getArgs().get( NAME_KEY );
    } else {
      name = System.getProperty("org.cougaar.node.name");
    }
    if (name == null) name = findHostName();

    setIdentifier(name);

    if (getArgs() != null && 
        (getArgs().containsKey(REGISTRY_KEY) || 
         getArgs().containsKey(LOCAL_KEY))) {
      Communications.getInstance().startNameServer();
    }

    createProfiler();
    initTransport();        // set up the message handler
    loadClusters();         // set up the clusters.

    //mgmtLP = new MgmtLP(this); // MTMTMT turn off till RMI namespace works
  }

  private void initTransport() {
    String name = getIdentifier();
    if (alwaysSerialize) {
      theMessenger = new PipedMessageTransport(name);
    } else {
      theMessenger = Communications.getInstance().startMessageTransport(name);
    }
    Communications.setDefaultMessageTransport(theMessenger);
    theMessenger.setDisableRetransmission(disableRetransmission);
    theMessenger.start();
    System.err.println("Started "+theMessenger);
  }

  /**
   *   This method is responsible for creating the AlpProcesses for the node.
   **/
  protected void loadClusters(){
    Vector someClusters = new Vector();
    //First get a list of processes form the profilier
    Vector clusterNames = theProfiler.getNodeProfile().getClusterNames();

    if (clusterNames.size() > 0) {

      System.err.println("Creating Clusters:");
      // now go through the list an create the object abstractly through the Class static methods
      for( Enumeration e = clusterNames.elements(); e.hasMoreElements(); ){
        String myAlias = (String)e.nextElement();
        
        System.err.println("\t"+myAlias);
        ClusterServesClusterManagement newCluster = createCluster(myAlias);
        someClusters.addElement( newCluster );
      }
      System.err.print("Loading Plugins:");

      Enumeration cnames = clusterNames.elements();
      Enumeration clusters = someClusters.elements();
      while (cnames.hasMoreElements()) {
        String cname = (String) cnames.nextElement();
        System.err.print("\n\t"+cname);
        ClusterServesClusterManagement cluster = (ClusterServesClusterManagement) clusters.nextElement();
        if (cluster != null) {
          populateCluster(cname, cluster);
        }
      }
      System.err.println("\nPlugins Loaded.");
    }
    setClusters( someClusters );
  }

  /**
   *   Modifier method for theArgs
   *   @param aTable The ArgTable object that is used to modify theArgs
   **/
  protected final void setArgs( ArgTable aTable ) { theArgs = aTable; }

  /**
   *   Modifier method for theClusters
   *   @param someClusters The SetOfObject object that is used to modify theClusters
   **/
  private final void setClusters( Vector someClusters ) { theClusters = someClusters; }

  public void receiveMessage(Message m) {
    System.err.println("Received unhandled message: "+m);
    Thread.dumpStack();
  }
  
  //
  // ClusterManagementServesCluster
  //

  /**  This method enables the ClusterManagement to instatiate the Cluster and add it
   *   as a ClusterImpl.  This is accomplished from the Class class methods and the
   *   data present in the profiler
   */
  public ClusterServesClusterManagement createCluster(String clusterid)
  {
    try {
      ClusterProfiler cp = new ClusterProfiler( clusterid );

      ClusterServesClusterManagement cluster = cp.createCluster( );
      // we pass in the alias as the cluster identifier so we can use this to 
      //translate messages to system proxy objects 
      cluster.setClusterIdentifier(new ClusterIdentifier(clusterid));
      //move the cluster to the intialized state
      cluster.initialize();
      cluster.load(this);
      cluster.start();
      if ( cluster.getState() != cluster.ACTIVE ){
        System.err.println("Cluster "+cluster+" is not Active!");
      }
                
      return cluster;
    } catch (Exception ex) {
      System.err.println("Caught Exception during cluster initialization:"+ex);
      ex.printStackTrace();
    }
    return null;
  }


  public boolean populateCluster(String clusterid, ClusterServesClusterManagement cluster)
  {
    try {
      ClusterProfiler cp = new ClusterProfiler( clusterid );

      ClusterIdentifier cid = new ClusterIdentifier(clusterid);

      // add the plugins
      for (Enumeration e = cp.enumeratePlugins(); e.hasMoreElements();) {
        AddPlugInMessage myMessage = new AddPlugInMessage();
        myMessage.setOriginator(cid);
        myMessage.setTarget(cid);
        myMessage.setPlugIn( (String)e.nextElement() );
        // bypass the message system to initialize the cluster - send
        // the message directly.
        cluster.receiveMessage( myMessage);
      }

      // tell the cluster to proceed.
      ClusterInitializedMessage m = new ClusterInitializedMessage();
      m.setOriginator(cid);
      m.setTarget(cid);
      cluster.receiveMessage(m);

      return true;
    } catch (Exception ex) {
      System.err.println("Caught Exception during cluster initialization:"+ex);
      ex.printStackTrace();
    }
    return false;
  }




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

