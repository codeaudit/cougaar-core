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
import java.util.*;

import org.cougaar.util.PropertyTree;
import org.cougaar.util.StringUtility;

public final class NodeINIParser {

  private NodeINIParser() {
    // no need for a constructor -- these are static utility methods
  }

  public static PropertyTree parse(String filename) {
    try {
      return parse(new FileInputStream(filename));
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static PropertyTree parse(InputStream in) {
    return parse(new BufferedReader(new InputStreamReader(in)));
  }

  /**
   * Read a Node </code>PropertyTree</code> from a <code>BufferedReader</code>.
   *
   * Expected format is:<pre>
   *   # option comment lines and empty lines
   *   [ ignored section headers ]
   *   cluster = clusterName
   *   # more "#cluster=clusterName" lines
   * </pre> and is parsed into a PropertyTree containing one 
   * <code>Map.Entry</code>:<pre>
   *   {clusters=[List of clusterNames]}
   * </pre>.
   */
  public static PropertyTree parse(BufferedReader in) {
    PropertyTree nodePT = new PropertyTree();
    try {
      List clustersList = null;
      while (true) {
        String s = in.readLine();
        if (s == null) {
          break;
        }
        s = s.trim();
        int eqIndex;
        if ((s.length() == 0) ||
            (s.startsWith("#")) ||
            ((eqIndex = s.indexOf("=")) <= 0)) {
          // ignore empty lines, "#comments", and non-"name=value" lines
          continue;
        }
        String name = s.substring(0, eqIndex).trim(); 
        String value = s.substring(eqIndex+1).trim(); 
        if (name.equals("cluster")) {
          // add cluster to List of "clusters"
          if (clustersList == null) {
            clustersList = new ArrayList();
            nodePT.put("clusters", clustersList);
          }
          clustersList.add(value);
        } else {
          // not expecting other values
          nodePT.put(name, value);
        }
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println("Error: " + ioe);
    }
    return nodePT;
  }

  /**
   * Write a Node "INI" file from the given <code>PropertyTree</code>.
   *
   * @throws ClassCastException if the PropertyTree contains illegal elements.
   */
  public static void write(PrintStream out, PropertyTree pt) {
    out.print("# machine-generated node ini file\n");
    for (int i = 0; i < pt.size(); i++) {
      Map.Entry meI = pt.get(i);
      String name = (String)meI.getKey();
      Object value = meI.getValue();
      if (name.equals("clusters")) {
        List l = (List)value;
        int nl = l.size();
        out.print("# "+nl+" cluster"+((nl>1)?"s":"")+":\n");
        for (int j = 0; j < nl; j++) {
          out.print("cluster="+(String)l.get(j)+"\n");
        }
      } else {
        // not expecting other values
        out.print(name+"="+(String)value+"\n");
      }
    }
  }

  public static void main(String[] args) {
    System.out.println("testme!");
    String fname = args[0];
    System.out.println("read: "+fname);
    PropertyTree pt = parse(fname);
    System.out.println(pt);
    System.out.println("write to stdout");
    write(System.out, pt);
  }

}
