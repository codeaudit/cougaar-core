/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society.rmi;

import org.cougaar.core.society.NameServer;
import org.cougaar.core.society.Communications;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.registry.*;
  
import java.util.*;
import org.cougaar.util.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** implementation of the generic ALP NameServer using RMI.
 * Looks for a NS remote at the alpreg.ini-specified location and
 * dispatches to it.
 *
 * Also has a create() method which can be used to actually create an
 * rmiregistry, a NSImpl (and register it at the expected place).
 *
 * @property org.cougaar.nameserver.verbose false iff true is the same as verbosity=1
 * @property org.cougaar.nameserver.verbosity verbosity level 0=quiet, 1=exceptions, 2=progress.
 * @property org.cougaar.nameserver.auto Start the nameserver automatically if possible (local) 
 * without an admin node.
 **/

public class RMINameServer implements NameServer {
  
  private static int verbosity = 0;
  private static boolean autoStart = true;

  static {
    if (Boolean.getBoolean("org.cougaar.nameserver.verbose"))
      verbosity=1;

    autoStart=(Boolean.valueOf(System.getProperty("org.cougaar.nameserver.auto",
                                                  autoStart?"true":"false"))).booleanValue();

    int i = Integer.getInteger("org.cougaar.nameserver.verbosity",-1).intValue();
    if (i>=0) verbosity=i;
  }

  /** slot to keep the NS around **/
  private NS ns = null;

  /** lazy-eval function to find the remote NS **/
  private NS getNS() {
    synchronized (this) {
      if (ns != null) return ns;// Now do it safely
      ns = contactNS();
      return ns;
    }
  }

  /** actually contact a remote NS, based on setup information **/
  private NS contactNS() {
    boolean first = true;
    Object remote = null;

    int port = Communications.getPort();
    String addr = Communications.getFdsAddress();

    String url = "//"+addr+":"+port+"/NameService";
    if (verbosity>1) System.err.print("Attempting to contact "+url+": ");
    while (remote == null) {
      if (verbosity>1) System.err.print(" .");
      try {
        Registry r = LocateRegistry.getRegistry(addr, port);
        remote = r.lookup(url);
      } catch (Exception e) { 
        // ignore
        if (verbosity>0) {
          e.printStackTrace();
        } else {
          System.err.print("!");
        }
      }
      
      if (remote == null) {
        if (first) {
          if (autoStart && isLocalServer()) {
            autoStart = false;  //  only try once
            create(false);           // 
          }

          System.err.print("Waiting for "+url);
          first = false;
        } else {
          System.err.print(".");
        }
        try {
          wait(500);
        } catch (InterruptedException ie) {}
      }
    }

    if (!first) {               // we printed "waiting" - don't leave 'em in suspense
      System.err.println("OK");
    }
    if (verbosity>1) System.err.println("\nContacted "+url+" as "+remote);
    return (NS) remote;
  }


  public void clear() {
    clear("/");
  }
  public void clear(Object directory) {
    try {
      getNS().clear((String) directory);
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
  }

  public boolean containsKey(Object key) {
    try {
      return getNS().containsKey((String) key);
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
      return false;
    }
  }        
  public boolean containsValue(Object value) { return false; }
  public Set entrySet() {
    return entrySet("/");
  }
  public Set entrySet(Object directory) {
    try {
      return new HashSet(getNS().entrySet((String) directory));
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
      return null;
    }
  }
  public Object get(Object key) {
    if (verbosity>1) System.err.print("Looking up '"+key+"':");
    try {
      Object r = getNS().get((String) key);
      if (verbosity>1) System.err.println(" Found "+r);
      return r;
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
    return null;
  }

  public boolean isEmpty() {
    return isEmpty("/");
  }
  public boolean isEmpty(Object directory) {
    try {
      return getNS().isEmpty((String) directory);
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
      return true;
    }
  }
  public Set keySet() {
    return keySet("/");
  }
  public Set keySet(Object directory) {
    try {
      return new HashSet(getNS().keySet((String) directory));
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
      return null;
    }
  }

  public Object put(Object name, Object o) {
    if (verbosity>1) System.err.print("Adding "+o+" as '"+name+"':");
    try {
      Object r = getNS().put((String) name, o);
      if (verbosity>1) System.err.println(" OK");
      return r;
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
    return null;
  }

  public void putAll(Map t) {
    for (Iterator i = t.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry e = (Map.Entry) i.next();
      put(e.getKey(), e.getValue());
    }
  }

  /** remove an object (and name) from the directory **/
  public Object remove(Object name) {
    if (verbosity>1) System.err.print("Removing "+name+"':");
    try {
      Object r = getNS().remove((String)name);
      if (verbosity>1) System.err.println(" OK");
      return r;
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
    return null;
  }

  public int size() {
    return size("/");
  }

  public int size(Object name) {
    if (verbosity>1) System.err.print("getting size of "+name+"':");
    try {
      int r = getNS().size((String)name);
      if (verbosity>1) System.err.println(" OK");
      return r;
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
    return -1;
  }

  public Collection values() {
    return values("/");
  }
  public Collection values(Object directory) {
    if (verbosity>1) System.err.print("values '"+directory+"':");
    try {
      Collection o = getNS().values((String) directory);
      if (verbosity>1) System.err.println(" Found "+o.size()+" elements.");
      return o;
    } catch (RemoteException re) {
      if (verbosity>1) System.err.println(" Failed:");
      if (verbosity>0) {
        re.printStackTrace();
      } else {
        System.err.print("!");
      }
    }
    return null;
  }

  /** @return true IFF the Communications server specified is 
   * expected to be on this host.
   **/
  private static boolean isLocalServer() {
    String addr = Communications.getFdsAddress();

    // quick test for localhost...
    if (addr.equals("localhost") ||
        addr.equals("localHost")        // bogus
        ) {
      return true; 
    }

    try {
      InetAddress de = InetAddress.getByName(addr);
      InetAddress me = InetAddress.getLocalHost();
      //System.err.println("Dest = "+de+"\nMe = "+me);
      if (de.equals(me))
        return true;
    } catch (UnknownHostException e) {
    }
    return false;
  }
    

  ////
  //// actually create a server
  ////

  public static void create() {
    create(true);               // try really hard by default
  }

  public static void create(boolean tryHard) {
    int port = Communications.getPort();
    String addr = Communications.getFdsAddress();
    String url = "//"+addr+":"+port+"/NameService";
    
    if (verbosity>1) System.err.print("Creating RMIRegistry at port "+port+":");
    Registry r = null;
    while (r == null) {
      if (verbosity>1) System.err.print(" .");
      try {
        r = LocateRegistry.createRegistry(port);
      } catch (RemoteException re) {
        if (! tryHard) return;  //  bail
        if (verbosity>0) {
          re.printStackTrace();
        } else {
          System.err.print("!");
        }
      }
    }

    if (verbosity>1) System.err.println(" Created "+r);

    if (verbosity>1) System.err.print(" Creating Nameservice at "+url+":");
    try {
      NS ns = new NSImpl();

      r.rebind(url, ns);
      //System.err.println("Bound "+ns+" to "+url);
      if (verbosity>1) System.err.println("OK");

      System.err.println("RMI NameServer started at "+url);
    } catch (Exception e) {
      if (! tryHard) return;  //  bail
      System.err.println("NameServer ("+url+") startup failed: ");
      e.printStackTrace();
    }
  }

  // tester
  
  public static void main(String[] args) {
    RMINameServer.create();
    RMINameServer rns = new RMINameServer();
    
    try {
      NS ns = new NSImpl();
      rns.put("fred", ns);
      Object o = rns.get("fred");
      System.err.println("got "+o);
      
      Enumeration stuff = new Enumerator(rns.values());
      if (stuff != null) {
        System.err.println("top level =");
        while (stuff.hasMoreElements()) {
          System.err.println("\t"+stuff.nextElement());
        }
      } else {
        System.err.println("Nothing!");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
