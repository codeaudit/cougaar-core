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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.cougaar.core.component.ComponentDescription;

import org.cougaar.util.StringUtility;

/**
 * Parse an INI stream into an <code>ComponentDescription[]</code>.
 * <p>
 * There should also be a similar XML parser.
 */
public final class INIParser {

  private INIParser() { 
    // no need for a constructor -- these are static utility methods
  }

  public static ComponentDescription[] parse(
      String filename,
      String containerInsertionPoint) {
    try {
      return 
        parse(
            new FileInputStream(filename), 
            containerInsertionPoint);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static ComponentDescription[] parse(
      InputStream in,
      String containerInsertionPoint) {
    return 
      parse(
          new BufferedReader(new InputStreamReader(in)),
          containerInsertionPoint);
  }

  private static final Vector EMPTY_VECTOR = new Vector(0);
  private static final Vector getEmptyVector() {
    return EMPTY_VECTOR;
  }

  /** Pattern for matching insertionPoint&priority **/
  final static Pattern namePattern = Pattern.compile("([a-zA-Z0-9.]+)(\\((.+)\\))?");

  /**
   * Read an INI file from a <code>BufferedReader</code> to create
   * <code>ComponentDescription</code>s.
   *
   * Expected format is:<pre>
   *   # option comment lines and empty lines
   *   [ ignored section headers ]
   *   insertionPoint = classname
   *   # or specify String arguments, which are saved as a Vector "param"
   *   insertionPoint = classname(arg0, arg1, argN)
   *   # more "insertionPoint = " lines
   * </pre> and is parsed into an array of ComponentDescriptions.
   * <p>
   * These fields are not currently supported:<br>
   *   codebase, certificate, lease, policy
   * <p>
   * Two insertion points have special backwards-compatability<ul>
   *   <li>"cluster" == "Node.AgentManager.Agent"</li>
   *   <li>"plugin" == "Node.AgentManager.Agent.PluginManager.Plugin"</li>
   * </ul>
   * <p>
   * Any insertion point (including aliases) may be postfixed with "(<em>PRIORITY</em>)"
   * where "<em>PRIORITY</em>" is one of the component priority names specified in
   * ComponentDescription (e.g. HIGH, INTERNAL, BINDER, COMPONENT, LOW, or STANDARD).  Note that
   * the name must be in upper case, no punctuation and no extra whitespace.  An example
   * is: "plugin(LOW)=org.cougaar.test.MyPlugin", which would specify that MyPlugin should be
   * loaded as a low priority plugin (e.g. after most other plugins and internal components).
   * <p>
   * Also "cluster=name" and is converted into<pre>
   *   "Node.AgentManager.Agent=org.cougaar.core.agent.ClusterImpl(name)"
   * </pre> as a default classname.
   * <p>
   * These old "cluster" values are <u>ignored</u>:<ul>
   *   <li>"class"   (now specified in the Node's INI!)</li>
   *   <li>"uic"     (ignored)</li>
   *   <li>"cloned"  (ignored)</li>
   * </ul>
   * @see org.cougaar.core.component.ComponentDescription
   */
  public static ComponentDescription[] parse(
                                             BufferedReader in,
                                             String containerInsertionPoint) 
  {

    List descs = new ArrayList();
    int line = 0;
    try {

readDescriptions:
      while (true) {

        // read an entry
        String s = null;
        while (true) {
          // read the current line
          line++;
          String tmp = in.readLine();
          if (tmp == null) {
            // end of file
            if (s != null) {
              System.err.println(
                  "Warning: INI file ends in ignored \""+s+"\"");
            }
            break readDescriptions;
          }
          if (tmp.endsWith("\\")) {
            tmp = tmp.substring(0, tmp.length()-1);
            if (!(tmp.endsWith("\\"))) {
              // line continuation
              s = ((s != null) ? (s + tmp) : tmp);
              continue;
            }
          }
          // finished line
          s = ((s != null) ? (s + tmp) : tmp);
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

        // special case name/value pairs
        if (name.equals("name") ||
            name.equals("class") ||
            name.equals("uic") ||
            name.equals("cloned")) {
          // ignore!
          continue;
        }

        // name is the insertion point
        String insertionPoint = name;
        int priority = -1;      // illegal priority (undefined) - we'll default it later
        {
          Matcher m = namePattern.matcher(insertionPoint);
          if (m.matches()) {   
            // group 1 is the new name
            String n = m.group(1);
            if (n != null) insertionPoint=n;

            // group 3 is the priority or null
            String p = m.group(3);
            if (p != null) {
              try {
                priority = ComponentDescription.parsePriority(p);
              } catch (IllegalArgumentException iae) {
                System.err.println("Warning: illegal component priority line "+line+": "+s);
              } 
            }
          } else {
            System.err.println("Warning: unparsable component description line "+line+": "+s);
          }
        }

        // FIXME only a simplistic property of "Vector<String>" is supported
        //
        // parse value into classname and optional parameters
        String classname;
        Vector vParams;
        int p1;
        int p2;
        if (((p1 = value.indexOf('(')) > 0) && 
            ((p2 = value.lastIndexOf(')')) > p1)) {
          classname = value.substring(0, p1);
          vParams = StringUtility.parseCSV(value, (p1+1), p2);
        } else {
          classname = value;
          vParams = getEmptyVector();
        }

        // fix the insertion point for backwards compatibility
        if (insertionPoint.equals("plugin")) {
          // should load into an Agent
          insertionPoint = "Node.AgentManager.Agent.PluginManager.Plugin";
        } else if (insertionPoint.equals("cluster")) {
          if (vParams.size() == 0) {
            // fix "cluster=name" to be "cluster=classname(name)"
            vParams = new Vector(1);
            vParams.add(classname);
            classname = "org.cougaar.core.agent.ClusterImpl";
          }
          // should load into a Node
          insertionPoint = "Node.AgentManager.Agent";
        }

        if (insertionPoint.startsWith(".")) {
          // prefix with container's insertion point
          insertionPoint = containerInsertionPoint + insertionPoint;
        }

        if (priority == -1) {
          // default binders to PRIORITY_BINDER instead of STANDARD 
          if (insertionPoint.endsWith(".Binder")) {
            priority = ComponentDescription.PRIORITY_BINDER;
          } else {
            priority = ComponentDescription.PRIORITY_COMPONENT;
          }
        }

        // FIXME unsupported fields: codebase, certificate, lease, policy
        //
        // create a new ComponentDescription
        ComponentDescription cd =
          new ComponentDescription(
              classname,        // name
              insertionPoint,
              classname,
              null,             // codebase
              vParams,
              null,             // certificate
              null,             // lease
              null,             // policy
              priority          // priority, of course.
              );

        // save
        descs.add(cd);
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println("Error: " + ioe);
    }
    return (ComponentDescription[])
      descs.toArray(
          new ComponentDescription[descs.size()]);
  }

  /**
   * Write an "INI" file from the given <code>ComponentDescription[]</code>.
   *
   * @throws ClassCastException if the ComponentDescription[] contains 
   *    illegal elements.
   */
  public static void write(PrintStream out, ComponentDescription[] descs) {
    out.print("# machine-generated ini file\n");
    int ndescs = ((descs != null) ? descs.length : 0);
    out.print("# "+ndescs+" component"+((ndescs>1)?"s":"")+":\n");
    for (int i = 0; i < ndescs; i++) {
      ComponentDescription descI = descs[i];
      out.print(descI.getInsertionPoint());
      out.print("=");
      out.print(descI.getClassname());
      Vector vParams = (Vector)descI.getParameter();
      int nvParams = vParams.size();
      if (nvParams > 0) {
        out.print("(");
        int k = 0;
        out.print((String)vParams.elementAt(k));
        while (++k < nvParams) {
          out.print(", ");
          out.print((String)vParams.elementAt(k));
        }
        out.print(")");
      }
      out.print("\n");
    }
  }

  public static void main(String[] args) {
    System.out.println("testme!");
    String fname = args[0];
    System.out.println("read: "+fname);
    ComponentDescription[] descs = parse(fname, "testBase");
    System.out.println("write to stdout");
    write(System.out, descs);
  }

}
