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

import java.io.*;
import java.lang.reflect.*;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.util.ConfigFileFinder;
import org.cougaar.core.component.ServiceProvider;
import java.util.*;
import java.net.*;

/**
 * Rendezvous for consistent communications within an alp society.
 * Generally, information comes first from defaults (localhost:8000:555)
 * then alpreg.ini (if it can be found), then properties and command-line
 * arguments.
 * 
 * Parameters:
 * org.cougaar.name.server=host[:port[:lpsport]]
 * org.cougaar.name.server.port=port
 * org.cougaar.message.transportClass=name-or-class
 *
 * Arguments:
 * -ns foo sets org.cougaar.name.server.
 **/

public class Communications {
  private static final String configFileName = "alpreg.ini";

  final public static String REGISTRY_DEFAULT_PORT = "8000";
  final public static String REGISTRY_DEFAULT_LPSPORT = "5555";

  private final Map props = new HashMap(11);

  public Communications() {
    setup_defaults();
    read_alpreg();
    apply_overrides();          // Apply command line overrides
  }

  public Communications(String args) {
    setup_defaults();
    initializeFromArguments(args);
  }
  
  private void read_alpreg() {
    try {
      InputStream fs = ConfigFileFinder.open(configFileName);
      if (fs == null) {
        System.out.println("Could not find "+configFileName+": Will use defaults.");
        return;
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(fs) );
      String line = null;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("//")) continue;
        if (line.startsWith("[")) continue;
        int index = line.indexOf( "=" );
        if ( index != -1 && index < line.length()) {
          String prop = line.substring(0, index).trim();
          String value = line.substring(index + 1).trim();
          put(prop,value);
        }
      }
      in.close();
    } catch (FileNotFoundException fnfe) {
    } catch(IOException e) {
      System.out.println("Error: " + e);
    }
  }

  /**
   * Apply overrides from command line
   **/
  private void apply_overrides() {
    String server = System.getProperty("org.cougaar.name.server");
    if (server != null) {
      try {
        Vector v = org.cougaar.util.StringUtility.parseCSV(server,':');
        int l = v.size();
        if (l>0) {
          InetAddress addr = InetAddress.getByName((String) v.elementAt(0));
          put("address", addr.getHostAddress());
          put("hostname", addr.getHostName());
        }
        if (l>1) {
          String s = (String) v.elementAt(1);
          try {
            Integer.parseInt(s); // Be sure it can be parsed
            put("port", s);
          } catch (NumberFormatException nfe) {
            System.err.println("org.cougaar.name.server[1] is not an integer: \""+s+"\".");
          }
        }
        if (l>2) {
          String s = (String) v.elementAt(2);
          try {
            Integer.parseInt(s); // Be sure it can be parsed
            put("lpsport", s);
          } catch (NumberFormatException nfe) {
            System.err.println("org.cougaar.name.server[1] is not an integer: \""+s+"\".");
          }
        }          
      } catch (UnknownHostException uhe) {
        System.err.println("org.cougaar.name.server specifies unknown host: " + server);
      }
    }
    String serverPort = System.getProperty("org.cougaar.name.server.port");
    if (serverPort != null) {
      try {
        Integer.parseInt(serverPort); // Be sure it can be parsed
        put("port", serverPort);
      } catch (NumberFormatException nfe) {
        System.err.println("org.cougaar.name.server.port is not an integer: " + serverPort);
      }
    }
  }

  public void put(String key, String v) {
    props.put(key,v);
  }

  public String get(String key) {
    return (String) props.get(key);
  }

  private void setup_defaults() {
    put("transport", defaultTransportClass);
    put("port", REGISTRY_DEFAULT_PORT);
    put("lpsport", REGISTRY_DEFAULT_LPSPORT);
    try {
      put("address", InetAddress.getLocalHost().getHostAddress());
      put("hostname", InetAddress.getLocalHost().getHostName());
    } catch (Exception e) {
      put("address", "127.0.0.1");
      put("hostname", "localhost");
    }
    put("alias", "Administrator");
  }

  public void initializeFromArguments(String arg) {
    try {
      int w = arg.indexOf(":");
      int x = arg.indexOf(":", w + 1);
      int y = arg.indexOf(":", x + 1);
      int z = arg.indexOf(":", y + 1);
      put("alias",arg.substring(0, w));
      put("address",arg.substring(w + 1, x));
      put("port",arg.substring(x + 1, y));
      if (z != -1) {
        put("lpsport",arg.substring(y + 1, z));
      } else {
        put("lpsport",arg.substring(y + 1));
      }
    } catch(Exception e) {
      System.out.println("Error constructing registry arg from argument: '"+arg+"'");
    }
  }

  // singleton pattern
  private static Communications _instance = null;
  public synchronized static Communications getInstance() {
    if (_instance != null) return _instance;      
    _instance = new Communications();
    return _instance;
  }

  // static accessors
  
  public static int getLPSPort() {
    return Integer.parseInt(getInstance().get("lpsport"));
  }
  public static int getPort() {
    return Integer.parseInt(getInstance().get("port"));
  }
  public static String getFdsAddress() {
    return getInstance().get("address");
  }


  /** map of key->classname **/
  private static final HashMap transports = new HashMap(7);

  /** default transport key **/
  private static String defaultTransportClass = null;

  public static final boolean putTransport(String key, String classname) {
    synchronized (transports) {
      if (transports.get(key) != null) return false;
      transports.put(key, classname);
      transports.put(key.toLowerCase(), classname);
      transports.put(classname, classname);
      return true;
    }
  }
  public static final String getTransport(String key) {
    synchronized (transports) {
      return (String) transports.get(key);
    }
  }

  public static final String getTransport() {
    return getTransport(defaultTransportClass);
  }

  // initialize static vars from system properties.
  static {
    Properties props = System.getProperties();
    putTransport("RMI", "org.cougaar.core.society.rmi.SimpleRMIMessageTransport");
    // putTransport("Pipe", "org.cougaar.core.society.PipedMessageTransport");

    String key = props.getProperty("org.cougaar.message.transport");
    if (key != null) {
      if (key.indexOf('.') > -1)
        defaultTransportClass = key;
      else 
        defaultTransportClass = getTransport(key);
    }

    //putTransport("Voyager", "org.cougaar.core.society.voyager.VoyagerMessageTransport");
    String mtc = props.getProperty("org.cougaar.message.transportClass");
    if (mtc != null) {
      defaultTransportClass = mtc;
    }

    if (defaultTransportClass==null) {
      defaultTransportClass = getTransport("RMI");
    }
  }

  /** creates a MessageTransport acting for the entity specified by id.
   **/
  public MessageTransportServer startMessageTransport(String id) {
//      String mtcn = defaultTransportClass;
//      try {
//        Class mtc = Class.forName(mtcn);
//        Class[] pattern = new Class[1];
//        pattern[0] = String.class;
//        Constructor cons = mtc.getConstructor(pattern);
//        Object[] args = new Object[1];
//        args[0] = id;
//        return (MessageTransport) cons.newInstance(args);
//      } catch (Exception e) {
//        System.err.println("Could not create MessageTransport ("+mtcn+") for "+id);
//        e.printStackTrace();
//        throw new Error("Node error: "+e);
//      }

      // Later this impl will be instantiated by Node as a component
      MessageTransportServerImpl impl = 
	  new MessageTransportServerImpl(id);
      // Later this provide will be registered with the Node by the
      // impl.
      MessageTransportServerServiceFactory provider = impl.getProvider();
      
      
      defaultNameServer = provider.getDefaultNameServer();

      // For now we make a single proxy, for all clients
      Object server =
	  provider.getService(null, this, MessageTransportServer.class);
      return (MessageTransportServer) server;
  }

  private static Object defaultLock = new Object();
  private static MessageTransportServer defaultMessageTransport = null;
  private static NameServer defaultNameServer = null;

  public static void setDefaultMessageTransport(MessageTransportServer mt) {
    synchronized (defaultLock) {
      if (defaultMessageTransport!=null) 
        throw new RuntimeException("Default MessageTransport already set.");
      defaultMessageTransport = mt;
    }
  }

  public static MessageTransportServer getDefaultMessageTransport() {
    return defaultMessageTransport;
  }
  public static NameServer getDefaultNameServer() {
    return defaultNameServer;
  }
    

  /** Start an actual NameServer instance **/
  public void startNameServer() {
    /*
    String tname = get("transport");
    String clname = getTransport(tname);
    */
    String clname = defaultTransportClass;
    
    try {
      Class cl = Class.forName(clname);
      Method meth = cl.getMethod("startNameService", null);
      meth.invoke(null, null);
    } catch (Exception e) {
      System.err.println("Exception starting transport "+clname+" :");
      e.printStackTrace();
    }
  }
}
