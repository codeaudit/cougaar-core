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

public final class ClusterINIParser {

  private ClusterINIParser() { 
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

  private static final Vector EMPTY_VECTOR = new Vector(0);
  private static final Vector getEmptyVector() {
    return EMPTY_VECTOR;
  }

  /**
   * Read a Cluster </code>PropertyTree</code> from a 
   * <code>BufferedReader</code>.
   *
   * Expected format is:<pre>
   *   # option comment lines and empty lines
   *   [ ignored section headers ]
   *   class = clusterClassname
   *   uic = clusterUIC
   *   cloned = false
   *   plugin = pluginClassname
   *   plugin = pluginClassname(arg0, arg1, argN)
   *   # more "#plugin=.." lines
   *   component = componentString
   *   # more "#component=.." lines
   * </pre> and is parsed into a PropertyTree containing:<pre>
   *   {class=clusterClassname,
   *    uid=clusterUIC,
   *    cloned=false,
   *    plugins={PropertyTree of pluginClassname=[Vector of args]},
   *    components=[List of componentStrings]}
   * </pre>.
   */
  public static PropertyTree parse(BufferedReader in) {
    PropertyTree clusterPT = new PropertyTree();
    try {
      PropertyTree pluginsPT = null;
      List componentsList = null;
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
        if (name.equals("plugin")) {
          // add plugin to propertyTree of "plugins"
          if (pluginsPT == null) {
            pluginsPT = new PropertyTree();
            clusterPT.put("plugins", pluginsPT);
          }
          // parse plugin classname and optional parameters
          String piName;
          Vector piParams;
          int p1;
          int p2;
          if (((p1 = value.indexOf('(')) > 0) && 
              ((p2 = value.lastIndexOf(')')) > p1)) {
            piName = value.substring(0, p1);
            piParams = StringUtility.parseCSV(value, (p1+1), p2);
          } else {
            piName = value;
            piParams = getEmptyVector();
          }
          pluginsPT.put(piName, piParams);
        } else if (name.equals("component")) {
          // add component to list of "components"
          if (componentsList == null) {
            componentsList = new ArrayList();
            clusterPT.put("components", componentsList);
          }
          componentsList.add(value);
        } else {
          // expecting "class", "uic", or "cloned"
          //
          // should probably guard against "plugins" and "components"!
          clusterPT.put(name, value);
        }
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println("Error: " + ioe);
    }
    return clusterPT;
  }

  /**
   * Write a cluster "INI" file from the given <code>PropertyTree</code>.
   *
   * @throws ClassCastException if the PropertyTree contains illegal elements.
   */
  public static void write(PrintStream out, PropertyTree pt) {
    out.print("# machine-generated cluster ini file\n");
    for (int i = 0; i < pt.size(); i++) {
      Map.Entry meI = pt.get(i);
      String name = (String)meI.getKey();
      Object value = meI.getValue();
      if (name.equals("plugins")) {
        PropertyTree spt = (PropertyTree)value;
        int nspt = spt.size();
        out.print("# "+nspt+" plugin"+((nspt>1)?"s":"")+":\n");
        for (int j = 0; j < nspt; j++) {
          Map.Entry meJ = spt.get(j);
          out.print("plugin=");
          out.print(meJ.getKey());
          Vector piParams = (Vector)meJ.getValue();
          int nPiParams;
          if ((nPiParams = piParams.size()) > 0) {
            out.print("(");
            int k = 0;
            out.print((String)piParams.elementAt(k));
            while (++k < nPiParams) {
              out.print(", ");
              out.print((String)piParams.elementAt(k));
            }
            out.print(")");
          }
          out.print("\n");
        }
      } else if (name.equals("components")) {
        List l = (List)value;
        int nl = l.size();
        out.print("# "+nl+" component"+((nl>1)?"s":"")+":\n");
        for (int j = 0; j < nl; j++) {
          out.print("component="+(String)l.get(j)+"\n");
        }
      } else {
        // expecting "class", "uic", or "cloned"
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
