/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.society.*;

import java.rmi.*;
import java.rmi.registry.*;
  
import java.util.*;
import org.cougaar.util.*;

import java.net.*;

/** List the entire contents of an ALP Nameserver.
 * Expects the usual configuration options, but also takes an optional
 * nameserver spec as a first argument (host:port:lpport)
 * example: java org.cougaar.core.society.LsNS draught:8888:5555
 **/

public class LsNS {
  private static void ls(NameServer ns, String p) {
    for (Iterator i = ns.entrySet(p).iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      System.out.print(p+entry.getKey());
      Object v = entry.getValue();
      if (v instanceof NameServer.Directory) {
        NameServer.Directory d = (NameServer.Directory) v;
        System.out.println("/");
        ls(ns, d.getPath());
      } else {
        System.out.println(" = "+v);
      }
    }
  }
      
  public static void main(String[] args) {
    try {
      if (args.length>0) System.getProperties().put("org.cougaar.name.server",args[0]);

      Communications c = Communications.getInstance();
      c.put("address","localhost");
      MessageTransport mt = c.startMessageTransport("Test");
      NameServer ns = mt.getNameServer();

      ls(ns, "/");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
