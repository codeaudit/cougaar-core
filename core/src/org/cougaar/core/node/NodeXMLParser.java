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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.xerces.parsers.DOMParser;
import org.cougaar.util.PropertyTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class NodeXMLParser {

  private NodeXMLParser() {
    // no need for a constructor -- these are static utility methods
  }

  public static PropertyTree parse(
      String filename) throws IOException, SAXException {
    return parse(new FileInputStream(filename));
  }

  /**
   * Read a Node </code>PropertyTree</code> from an
   * <code>InputStream</code>.
   *
   * Expected format is:<pre>
   *   &lt;node&gt;
   *     &lt;cluster&gt;clusterName&lt;/cluster&gt;
   *     &lt;!-- more "&lt;cluster&gt;" lines --&gt;
   *   &lt;/node&gt;
   * </pre> and is parsed into a PropertyTree containing one 
   * <code>Map.Entry</code>:<pre>
   *   {clusters=[List of clusterNames]}
   * </pre>.
   */
  public static PropertyTree parse(
      InputStream in) throws IOException, SAXException {
    PropertyTree nodePT = new PropertyTree();
    List clustersList = null;
    List l = readXML(in);
    int nl = ((l != null) ? l.size() : 0);
    for (int i = 0; i < nl; i++) {
      Map.Entry meI = (Map.Entry)l.get(i);
      String name = (String)meI.getKey();
      String value = (String)meI.getValue();
      if (name.equals("cluster")) {
        // add plugin to propertyTree of "clusters"
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
    return nodePT;
  }

  /**
   * Parse XML to a List of (name, value) pairs.
   *
   * I'm sure there are more efficient implementations than using a
   * DOM, but this is fine for now.
   */
  private static final List readXML(InputStream in) 
      throws IOException, SAXException {
    DOMParser parser = new DOMParser();
    try {
      InputSource inSource = new InputSource(in);
      parser.parse(inSource);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
        }
      }
    }
    Document document = parser.getDocument();
    Element rootElem = document.getDocumentElement();

    String rootName = rootElem.getNodeName();
    if (!(rootName.equals("node"))) {
      throw new IllegalArgumentException(
          "Expecting <node>, not "+rootName);
    }
    NodeList rootNodeList = rootElem.getChildNodes();
    int nRootNodes = rootNodeList.getLength();

    List result = new ArrayList();

    for (int i = 0; i < nRootNodes; i++) {
      org.w3c.dom.Node subNode = (org.w3c.dom.Node)rootNodeList.item(i);
      if (subNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
        continue;
      }
      final String name = subNode.getNodeName().trim();
      NodeList subNodeList = subNode.getChildNodes();
      org.w3c.dom.Node firstSubNode;
      if ((subNodeList.getLength() != 1) ||
          ((firstSubNode = 
            (org.w3c.dom.Node)subNodeList.item(0)).getNodeType() ==
           org.w3c.dom.Node.ELEMENT_NODE)) {
        continue;
      }
      final String value = (firstSubNode).getNodeValue().trim();
      Map.Entry resultElem = 
        new Map.Entry() {
          public Object getKey() { 
            return name; 
          }
          public Object getValue() {
            return value; 
          }
          public Object setValue(Object o) {
            throw new UnsupportedOperationException(); 
          }
        };
      result.add(resultElem);
    }

    return result;
  }


  /**
   * Write a node "XML" file from the given <code>PropertyTree</code>.
   *
   * @throws ClassCastException if the PropertyTree contains illegal elements.
   *
   * @see #parse(InputStream) for output XML format
   */
  public static void write(PrintStream out, PropertyTree pt) {
    out.print(
        "<?xml version=\"1.0\" ?>\n"+
        "<!-- machine-generated node ini file -->\n"+
        "<node>\n");
    for (int i = 0; i < pt.size(); i++) {
      Map.Entry meI = pt.get(i);
      String name = (String)meI.getKey();
      Object value = meI.getValue();
      if (name.equals("clusters")) {
        List l = (List)value;
        int nl = l.size();
        out.print("<!-- "+nl+" cluster"+((nl>1)?"s":"")+": -->\n");
        for (int j = 0; j < nl; j++) {
          out.print("<cluster>"+(String)l.get(j)+"</cluster>\n");
        }
      } else {
        // not expecting other values
        out.print("<"+name+">"+(String)value+"</"+name+">\n");
      }
    }
    out.print("</node>\n");
  }

  public static void main(String[] args) {
    System.out.println("testme!");
    String fname = args[0];
    System.out.println("read: "+fname);
    PropertyTree pt;
    try {
      pt = parse(fname);
    } catch (Exception e) {
      e.printStackTrace();
      pt = new PropertyTree();
    }
    System.out.println(pt);
    System.out.println("write to stdout");
    try {
      write(System.out, pt);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }
}
