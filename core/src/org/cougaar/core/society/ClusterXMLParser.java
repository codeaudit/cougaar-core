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

import org.w3c.dom.*;
import org.apache.xerces.parsers.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.cougaar.util.PropertyTree;
import org.cougaar.util.StringUtility;

public final class ClusterXMLParser {

  private ClusterXMLParser() {
    // no need for a constructor -- these are static utility methods
  }

  public static PropertyTree parse(
      String filename) throws IOException, SAXException {
    return parse(new FileInputStream(filename));
  }

  private static final Vector EMPTY_VECTOR = new Vector(0);
  private static final Vector getEmptyVector() {
    return EMPTY_VECTOR;
  }

  /**
   * Read a Cluster </code>PropertyTree</code> from an
   * <code>InputStream</code>.
   *
   * Expected format is:<pre>
   *   &lt;cluster&gt;
   *     &lt;class&gt;clusterClassname&lt;/class&gt;
   *     &lt;uic&gt;clusterUIC&lt;/uic&gt;
   *     &lt;cloned&gt;false&lt;/cloned&gt;
   *     &lt;plugin&gt;pluginClassname&lt;/plugin&gt;
   *     &lt;plugin&gt;pluginClassname(arg0, arg1, argN)&lt;/plugin&gt;
   *     &lt;!-- more "&lt;plugin&gt;" lines --&gt;
   *     &lt;component&gt;componentString&lt;/component&gt;
   *     &lt;!-- more "&lt;component&gt;" lines --&gt;
   *   &lt;/cluster&gt;
   * </pre> and is parsed into a PropertyTree containing:<pre>
   *   {class=clusterClassname,
   *    uid=clusterUIC,
   *    cloned=false,
   *    plugins={PropertyTree of pluginClassname=[Vector of args]},
   *    components=[List of componentStrings]}
   * </pre>.
   */
  public static PropertyTree parse(
      InputStream in) throws IOException, SAXException {
    PropertyTree clusterPT = new PropertyTree();
    PropertyTree pluginsPT = null;
    List componentsList = null;
    List l = readXML(in);
    int nl = ((l != null) ? l.size() : 0);
    for (int i = 0; i < nl; i++) {
      Map.Entry meI = (Map.Entry)l.get(i);
      String name = (String)meI.getKey();
      String value = (String)meI.getValue();
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
        clusterPT.put(name, value);
      }
    }
    return clusterPT;
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
    if (!(rootName.equals("cluster"))) {
      throw new IllegalArgumentException(
          "Expecting <cluster>, not "+rootName);
    }
    NodeList rootNodeList = rootElem.getChildNodes();
    int nRootNodes = rootNodeList.getLength();

    List result = new ArrayList();

    for (int i = 0; i < nRootNodes; i++) {
      org.w3c.dom.Node subNode = 
        (org.w3c.dom.Node)rootNodeList.item(i);
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
   * Write a cluster "XML" file from the given <code>PropertyTree</code>.
   *
   * @throws ClassCastException if the PropertyTree contains illegal elements.
   *
   * @see parse(InputStream) for output XML format
   */
  public static void write(PrintStream out, PropertyTree pt) {
    out.print(
        "<?xml version=\"1.0\" ?>\n"+
        "<!-- machine-generated cluster ini file -->\n"+
        "<cluster>\n");
    for (int i = 0; i < pt.size(); i++) {
      Map.Entry meI = pt.get(i);
      String name = (String)meI.getKey();
      Object value = meI.getValue();
      if (name.equals("plugins")) {
        PropertyTree spt = (PropertyTree)value;
        int nspt = spt.size();
        out.print("<!-- "+nspt+" plugin"+((nspt>1)?"s":"")+": -->\n");
        for (int j = 0; j < nspt; j++) {
          Map.Entry meJ = spt.get(j);
          out.print("<plugin>");
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
          out.print("</plugin>\n");
        }
      } else if (name.equals("components")) {
        List l = (List)value;
        int nl = l.size();
        out.print("<!-- "+nl+" component"+((nl>1)?"s":"")+": -->\n");
        for (int j = 0; j < nl; j++) {
          out.print("<component>"+(String)l.get(j)+"</component>\n");
        }
      } else {
        // expecting "class", "uic", or "cloned"
        out.print("<"+name+">"+(String)value+"</"+name+">\n");
      }
    }
    out.print("</cluster>\n");
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
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
