/*
 *
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
package org.cougaar.domain.planning.servlet;

import java.io.*;
import java.util.*;

import org.cougaar.core.servlet.ServletUtil;

import org.cougaar.util.PropertyTree;

/**
 * Parser for the <code>PlanViewServlet</code>'s "Advanced Search" 
 * loading of the built-in predicates.
 *
 * @see #parse(BufferedReader) for input stream format
 */
public class PredTableParser {

  /** @see #parse(BufferedReader) */
  public static final PropertyTree parse(InputStream in) {
    return parse(new BufferedReader(new InputStreamReader(in)));
  }

  /** @see #parse(BufferedReader) */
  public static final PropertyTree parse(Reader in) {
    return parse(new BufferedReader(in));
  }

  /**
   * Parse the input to build a PropertyTree of encoded predicates
   * for the <code>PlanViewServlet</code>'s "Advanced Search" page.
   * <pre>
   * Expected file format is:
   *
   *   Entry separator lines start with "***"
   *
   *   Comment lines start with "**" and then can contain additional 
   *     characters (except having the third char be "*", since this
   *     would confuse things with the entry separator)
   *
   *   Entries must have at least two lines or they are ignored.  They
   *     must be prefixed AND followed by "***" lines.
   *
   *   The first line is the "key", and the following lines make the
   *     "value".  These are encoded with 
   *     <tt>ServletUtil.encodeForHTML</tt> and 
   *     <tt>ServletUtil.encodeForJava</tt> 
   *     to let the PlanViewServlet print them in HTML and Javascript.
   * </pre>
   * @see #main(String[]) for an example
   */
  public static final PropertyTree parse(BufferedReader in) {
    PropertyTree pt = new PropertyTree();
    try {
      // read entries
readEntries:
      while (true) {
        // read the key line
        String rawKey = in.readLine();
        if (rawKey == null) {
          // end of input
          break readEntries;
        }
        if (rawKey.startsWith("**")) {
          // ignore comment or empty entry
          continue readEntries;
        }
        // read the value lines
        String rawValue = null;
readValue:
        while (true) {
          String s = in.readLine();
          if (s == null) {
            // end of input
            break readEntries;
          }
          if (!(s.startsWith("**"))) {
            // value line, typical case
            if (rawValue != null) {
              rawValue += "\n"+s;
            } else {
              rawValue = s;
            }
          } else {
            // control line
            if ((s.length() <= 2) ||
                (s.charAt(2) != '*')) {
              // comment  ("**?");
              continue readValue;
            } else {
              // end entry ("***");
              if (rawValue != null) {
                break readValue;
              } else {
                // ignore 
                break readEntries;
              }
            }
          }
        }
        // encode the key and value for javascript use
        String encKey = ServletUtil.encodeForHTML(rawKey);
        String encValue = ServletUtil.encodeForJava(rawValue);
        // add to property tree
        pt.put(encKey, encValue);
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println(
          "Unable to parse PlanViewServlet's predicates: "+ioe);
    }
    return pt;
  }

  /** testing utility, plus illustrates file format. */
  public static void main(String[] args) {
    String s = 
      "******"+
      "\n** comment "+
      "\n**"+
      "\n***"+
      "\n******"+
      "\nkey"+
      "\nvalue"+
      "\n******"+
      "\ncomplex (x < \"foo\" > z) \\ endKey"+
      "\n**comment"+
      "\n// java comment"+
      "\n//"+
      "\n/* another java comment \"here\"*/"+
      "\n"+
      "\nvalue"+
      "\n(y < \"z\")"+
      "\n  ** not a comment \\ here"+
      "\n  *** not the end of the entry"+
      "\n  more junk > blah"+
      "\nkeep double-encoded: \\\" \\n"+
      "\n"+
      "\nend..."+
      "\n***";
    System.out.println("Test with:\n"+s);
    // can replace this deprecated input with something newer...
    StringReader srin = 
      new StringReader(s);
    // parse!
    PropertyTree pt = parse(srin);
    int n = pt.size();
    System.out.println("Parsed["+n+"]");
    for (int i = 0; i < n; i++) {
      String ki = (String)pt.getKey(i);
      String vi = (String)pt.getValue(i);
      System.out.println(i+")");
      System.out.println("  key=|"+ki+"|");
      System.out.println("  value=|"+vi+"|");
    }
  }

}
