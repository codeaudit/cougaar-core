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

import java.util.*;


/** NameServer testing utility.
 * With no arguments, contacts the name server and prints out
 * all non-directory keys known by the NameServer.
 * With an argument, does the same set of lookups (without printing)
 * forever, printing a "." every thousand RMI operations.
 **/

public class PingNameServer {
  private static int count = 0;
  private static boolean isPrinting = false;

  private static void inc() {
    count++;
    if (count%1000 == 0) System.err.print(".");
  }

  public static void main(String[] arg) {
    Communications c = Communications.getInstance();
    MessageTransportServer mt = c.startMessageTransport("TestNS");
    c.setDefaultMessageTransport(mt);
    NameServer ns = c.getDefaultNameServer();

    if (arg.length >0) {
      while (true) {
        searchDir(ns, "/");
      }
    } else {
      isPrinting=true;
      searchDir(ns, "/");
      System.err.println("\nLookups="+count);
    }
  }

  static void searchDir(NameServer ns, String path) {
    Collection keys = ns.keySet(path);
    inc();
    if (keys != null) {
      for (Iterator i = keys.iterator(); i.hasNext();) {
        String key = (String) i.next();
        String kp = path+key;
        try {
          Object value = ns.get(kp);
          inc();
          if (value instanceof NameServer.Directory) {
            String p = ((NameServer.Directory) value).getPath();
            searchDir(ns,p);
          } else {
            if (isPrinting) System.err.println(kp);
          }
        } catch (RuntimeException re) {
          searchDir(ns, kp);
        }
      }
    }
  }

}
