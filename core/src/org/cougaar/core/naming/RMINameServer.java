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

package org.cougaar.core.naming;

import org.cougaar.core.society.Communications;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.registry.*;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
  
import java.util.*;

import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;

import javax.naming.*;

import org.cougaar.core.util.*;
import org.cougaar.util.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** implementation of the generic COUGAAR NameServer using RMI.
 * Looks for a NS remote at the alpreg.ini-specified location and
 * dispatches to it.
 *
 * Also has a create() method which can be used to actually create an
 * rmiregistry, a NSImpl (and register it at the expected place).
 *
 * @property org.cougaar.nameserver.verbose If set to </em>true</em> this is the same as verbosity=1
 * @property org.cougaar.nameserver.verbosity Set the Nameservice verbosity level: 0=quiet, 1=exceptions, 2=progress.
 * @property org.cougaar.nameserver.auto Start the nameserver automatically if possible (local) 
 * without an admin node.
 * @property org.cougaar.nameserver.local Circumvent RMI if possible for nameservice.
 **/

public class RMINameServer implements InitialContextFactory {
  
  private static int verbosity = 0;
  private static boolean autoStart = true;
  private static boolean fastLocal = true;
  static {
    if (Boolean.getBoolean("org.cougaar.nameserver.verbose"))
      verbosity=1;

    autoStart=(Boolean.valueOf(System.getProperty("org.cougaar.nameserver.auto",
                                                  autoStart?"true":"false"))).booleanValue();
    fastLocal=(Boolean.valueOf(System.getProperty("org.cougaar.nameserver.local",
                                                  fastLocal?"true":"false"))).booleanValue();

    int i = Integer.getInteger("org.cougaar.nameserver.verbosity",-1).intValue();
    if (i>=0) verbosity=i;
  }

  /** slot to keep the NS around **/
  private NS ns = null;

  /** lazy-eval function to find the remote NS **/
  private NS getNS() {

    // maybe circumvent RMI
    if (fastLocal) {
      NS tns = getLocalNS();
      if (tns != null) return tns;
    }

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
    RMIClientSocketFactory csf = NamingSocketFactory.getInstance();

    String url = "//"+addr+":"+port+"/NameService";
    if (verbosity>1) System.err.print("Attempting to contact "+url+": ");
    while (remote == null) {
      if (verbosity>1) System.err.print(" .");
      try {
        Registry r = LocateRegistry.getRegistry(addr, port, csf);
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

  public Context getInitialContext(Hashtable env) {
    try {
      return new NamingDirContext(getNS(), getNS().getRoot(), env);
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

  /** keep the NSImpl around for fastLocal use **/
  private static NS actual = null;
  private static Object actualLock = new Object();

  private static NS getLocalNS() { 
    synchronized (actualLock) {
      return actual;
    }
  }

  public static void create() {
    create(true);               // try really hard by default
  }

  public static void create(boolean tryHard) {
    int port = Communications.getPort();
    String addr = Communications.getFdsAddress();
    String url = "//"+addr+":"+port+"/NameService";
    RMIClientSocketFactory csf = NamingSocketFactory.getInstance();
    RMIServerSocketFactory ssf = NamingSocketFactory.getInstance();
    
    if (verbosity>1) System.err.print("Creating RMIRegistry at port "+port+":");
    Registry r = null;
    while (r == null) {
      if (verbosity>1) System.err.print(" .");
      try {
        r = LocateRegistry.createRegistry(port, csf, ssf);
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

      synchronized (actualLock) {
        actual = ns;
      }

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
      
      Context initialContext = rns.getInitialContext(new Hashtable());
      initialContext.bind("fred", "fred");

      initialContext.bind("wilma", "wilma");

      Context barneyContext = 
        initialContext.createSubcontext("barney");
      barneyContext.bind("bam bam", "bam bam");

      NamingEnumeration bindings = initialContext.listBindings("");
      while (bindings.hasMore()) {
        Binding binding = (Binding) bindings.next();
        System.out.println(binding.getName() + " " + binding.getObject());
        if (binding.getObject() instanceof Context) {
          NamingEnumeration subBindings = ((Context) binding.getObject()).listBindings("");
          while (subBindings.hasMore()) {
            binding = (Binding) subBindings.next();
            System.out.println("\t" + binding.getName() + " " + binding.getObject());
          }
        }
      }
      
      
      
      System.exit(0);
      // test Context implementation
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}








