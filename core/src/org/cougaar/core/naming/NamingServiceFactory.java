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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;

import javax.naming.*;

//  import org.cougaar.core.util.*;
//  import org.cougaar.util.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Creates an RMI stub for communicating with the name server or the
 * server itself. In any JVM the first factory needing to access the
 * name server is responsible for creating it. Others just use what
 * the first factory creates, waiting if necessary. The location of
 * the name server is specified in alpreg.ini. If that location is
 * remote, the rmi registry at the port specified by alpreg.ini is
 * contacted to obtain a stub for the server. If necessary, we wait
 * for either the registry or the server since either may not yet have
 * been created,
 *
 * @property org.cougaar.nameserver.verbose If set to </em>true</em> this is the same as verbosity=1
 * @property org.cougaar.nameserver.verbosity Set the Nameservice verbosity level: 0=quiet, 1=exceptions, 2=progress.
 * @property org.cougaar.nameserver.auto Start the nameserver automatically if possible (local) 
 * without an admin node.
 **/

public class NamingServiceFactory implements InitialContextFactory {

  /** Set from system properties to additional information **/
  private static int verbosity = 0;
  /** Set true if the name server is to be run as part of a Node **/
  private static boolean autoStart = true;

  /**
   * Initialize static things (properties)
   **/
  static {
    if (Boolean.getBoolean("org.cougaar.nameserver.verbose"))
      verbosity = 1;

    autoStart=(Boolean.valueOf(System.getProperty("org.cougaar.nameserver.auto",
                                                  autoStart?"true":"false"))).booleanValue();

    int i = Integer.getInteger("org.cougaar.nameserver.verbosity", -1).intValue();
    if (i >= 0) verbosity = i;
  }

  /** slot to keep the NS around. Everyone uses this **/
  private static NS ns = null;

  /**
   * Accessor for the ns slot. The first caller fills in the slot.
   * Everyone else waits until it is filled.
   **/
  private synchronized static NS getNS() {
    if (ns == null) {           // Has this been set yet?
      int port = Communications.getPort();
      String addr = Communications.getFdsAddress();
      NamingSocketFactory nsf = NamingSocketFactory.getInstance();
      
      String url = "//" + addr + ":" + port + "/NameService";
      if (verbosity > 0) System.err.print("Attempting to contact " + url + ": ");
      // First we have to locate (or become) the registry
      while (ns == null) {
        Registry r = null;
        try {
          r = LocateRegistry.getRegistry(addr, port, nsf);
          try {
            r.list();             // Test that this is a real registry
          } catch (Exception e) {
            r = null;
            throw e;
          }
          ns = (NS) r.lookup(url);
        } catch (Exception e) {
          if (verbosity > 0) {
            if (r != null)
              System.err.println("No name server bound in rmi registry " + r);
            else
              System.err.println("No rmi registry");
          } else {
            System.err.print("!");
          }
          if (verbosity > 1) e.printStackTrace();
          // No name server at specified location. Should we be "it"
          if (autoStart && isLocalServer()) {
            autoStart = false;  //  only try once
            try {
              if (verbosity > 0)
                System.err.print("Creating name server at port " + port + ": ");
              if (r == null)
                r = LocateRegistry.createRegistry(port, nsf, nsf);
              ns = new NSImpl();
              r.rebind(url, ns);
              break;            // Done. Get outta here
            } catch (Exception e2) {
              /* Apparently another node created the registry between
                 when we tried to get it above and when we tried to
                 create it here. Just go around the loop again and get
                 the registry the other node just created */
              if (verbosity > 0) {
                if (r != null) {
                  System.err.println("Could not bind a new name server in our rmi registry");
                  System.err.println("This is bad news and should not be possible");
                } else {
                  System.err.println("Failed to create rmi registry. Another node probably got there first.");
                }
                if (verbosity > 1) e2.printStackTrace();
              } else {
                System.err.print("!");
              }
            }
          }
        }
        try {
          Thread.sleep(500L);
        } catch (InterruptedException ie) {
        }
      }
      if (verbosity > 0) System.err.println("OK");
    }
    return ns;
  }

  public Context getInitialContext(Hashtable env) {
    try {
      return new NamingDirContext(getNS(), getNS().getRoot(), env);
    } catch (RemoteException re) {
      if (verbosity > 0) System.err.println(" Failed:");
      if (verbosity > 1) {
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
      e.printStackTrace();
    }
    return false;
  }

  // tester
  
  public static void main(String[] args) {
    NamingServiceFactory nsf = new NamingServiceFactory();
    try {
      Context initialContext = nsf.getInitialContext(new Hashtable());
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








