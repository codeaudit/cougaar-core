/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.*;
import org.cougaar.util.log.*;
import org.cougaar.util.ConfigFinder;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class to provide service for initializing Components
 * from an XML file.
 *
 * <pre>
 * @property org.cougaar.node.name
 *   The name for this Node.
 * @property org.cougaar.society.file
 *   The name of the XML file from which to read this Node's definition
 * @property org.cougaar.node.validate
 *   Indicates if the XML parser should be validating or not. Expected values are true/false.
 *
 * </pre>
 **/
public class XMLComponentInitializerServiceProvider
  implements ServiceProvider {

  // SAX Parser constants required by new xerces parser
  static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  private String filename;
  private String nodename;
  Logger logger;
  HashMap allComponents = new HashMap();

  /*
   * "allComponents" is a mapping of of the name of the parent component (String)
   *   to a HashMap.  There is one entry in here for each agent (including the node agent)
   * This HashMap is a mapping of insertion point container names (ie the  insertion
   *   point truncated at the last '.') -- a String -- to a ArrayList. The list
   *   contains an ordered list of components to be inserted at this container.
   * This ArrayList is an ordered list of HashMaps. Each HashMap maps property names
   *   (String) like "insertionpoint" to String values. A special value in the HashMap is
   *   "arguments" which maps to a ArrayList of strings (ordered)
   *   that are the component's arguments.
   */

  private static String insertionPointContainer(String insertionPoint) {
    return insertionPoint.substring(0, insertionPoint.lastIndexOf('.'));
  }

  public XMLComponentInitializerServiceProvider() {
    this.logger = Logging.getLogger(getClass());

    filename = System.getProperty("org.cougaar.society.file");
    nodename = System.getProperty("org.cougaar.node.name");
    if (logger.isShoutEnabled())
      logger.shout(
        "Will initialize node from XML file \""
          + filename
          + "\", creating node named \""
          + nodename
          + "\"");

    if ((nodename == null) && logger.isErrorEnabled())
      logger.error(
        "Node name is null. That's not going to work. Set -Dorg.cougaar.node.name.");
    if ("".equals(nodename) && logger.isErrorEnabled())
      logger.error(
        "Node name is the empty string.  That's not going to work. Set -Dorg.cougaar.node.name.");
    if ((filename == null) && logger.isErrorEnabled())
      logger.error(
        "File name is null. That's not going to work. Set -Dorg.cougaar.society.file.");
    if ("".equals(filename) && logger.isErrorEnabled())
      logger.error(
        "File name is the empty string.  That's not going to work. Set -Dorg.cougaar.society.file.");
    try {
      parseFile();
      if ((allComponents.size() == 0) && logger.isErrorEnabled())
        logger.error(
          "The configuration for node \""
            + nodename
            + "\" was not found in the file \""
            + filename
            + "\"");
    } catch (Exception e) {
      logger.error("Exception reading society XML file " + filename, e);
    }
  }

  private void parseFile()
    throws
      FileNotFoundException,
      IOException,
      SAXException,
      ParserConfigurationException {
    MyHandler handler = new MyHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(Boolean.getBoolean("org.cougaar.core.node.validate"));
    if(logger.isDebugEnabled())
      logger.debug((factory.isValidating()) ? "Validating against schema" : "Validating disabled");
    factory.setNamespaceAware(true);
    SAXParser saxParser = factory.newSAXParser();

    // Uncomment the following line when we go back to xerces2 again - bug 2823
    //    saxParser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
    
    InputStream istr = ConfigFinder.getInstance().open(filename);
    if (istr == null) {
      logger.error("null InputStream from ConfigFinder on " + filename);
      return;
    }
    InputSource is = new InputSource(istr);
    if (is != null) {
      saxParser.parse(is, handler);
    } else {
      logger.error("Unable to open " + filename + " for XML initialization");
    }
  }

  public Object getService(
    ServiceBroker sb,
    Object requestor,
    Class serviceClass) {
    if (serviceClass != ComponentInitializerService.class) {
      throw new IllegalArgumentException(
        getClass() + " does not furnish " + serviceClass);
    }
    return new ComponentInitializerServiceImpl();
  }

  public void releaseService(
    ServiceBroker sb,
    Object requestor,
    Class serviceClass,
    Object service) {
  }

  private class ComponentInitializerServiceImpl
    implements ComponentInitializerService {

    private ComponentDescription[] noComponents = new ComponentDescription[0];

    /**
     * Get the descriptions of components with the named parent having
     * an insertion point below the given container insertion point.
     **/
    public ComponentDescription[] getComponentDescriptions(
      String parentName,
      String containerInsertionPoint)
      throws InitializerException {

      if (logger.isInfoEnabled())
        logger.info(
          "Looking for direct sub-components of "
            + parentName
            + " just below insertion point "
            + containerInsertionPoint);

      ComponentDescription[] ret = noComponents;
      try {
        HashMap byParent = (HashMap) allComponents.get(parentName);
        if (byParent != null) {
          ArrayList byInsertionPoint =
            (ArrayList) byParent.get(containerInsertionPoint);
          if (byInsertionPoint != null) {
            ret = makeComponents(byInsertionPoint);
          }
        }
      } catch (Exception e) {
        throw new InitializerException(
          "getComponentDescriptions("
            + parentName
            + ", "
            + containerInsertionPoint
            + ")",
          e);
      }
      if (logger.isInfoEnabled())
        logger.info("Returning " + ret.length + " component descs");
      if (logger.isDebugEnabled())
        dump(ret);
      return ret;
    }
  }

  private void dump(ComponentDescription[] comps) {
    for (int i=0; i<comps.length; i++) {
      StringBuffer output = new StringBuffer();
      output.append("  COMP(");
      output.append(i);
      output.append("):");
      output.append(comps[i].toString());
      logger.debug(output.toString());
    }
  }

  private ComponentDescription[] makeComponents(List components) {
    ComponentDescription[] ret = new ComponentDescription[components.size()];
    Iterator componentEnum = components.iterator();
    int i = 0;
    while (componentEnum.hasNext()) {
      HashMap componentProps = (HashMap) componentEnum.next();
      String name = (String) componentProps.get("name");
      String classname = (String) componentProps.get("class");
      String priority = (String) componentProps.get("priority");
      String insertionPoint = (String) componentProps.get("insertionpoint");
      String order = (String) componentProps.get("order");
      ArrayList args = (ArrayList) componentProps.get("arguments");
      Vector vParams = null;
      if ((args != null) && (args.size() > 0)) {
        vParams = new Vector(args.size());
        Iterator argsIter = args.iterator();
        while (argsIter.hasNext()) {
          vParams.addElement(argsIter.next());
        }
      }
      ret[i++] =
        makeComponentDesc(name, vParams, classname, priority, insertionPoint);
    }
    return ret;
  }

  private ComponentDescription makeComponentDesc(
						 String name,
    Vector vParams,
    String classname,
    String priority,
    String insertionPoint) {
    return new ComponentDescription(name, insertionPoint, classname, null,
    //codebase
    vParams, //params
    null, //certificate
    null, //lease
    null, //policy
    ComponentDescription.parsePriority(priority));
  }

  private class MyHandler extends DefaultHandler {

    boolean thisNode = false;
    Map currentComponent;
    String currentParent;
    CharArrayWriter chars;
    private final String stdPriority =
      ComponentDescription.priorityToString(
        ComponentDescription.PRIORITY_STANDARD);

    public void startElement(
      String namespaceURI,
      String localName,
      String qName,
      Attributes atts)
      throws SAXException {
      if (localName.equals("node")) {
        String thisName = atts.getValue("name");
        if (nodename.equals(thisName)) {
          if (logger.isDebugEnabled())
            logger.debug("started element for this node: " + thisName);
          currentParent = thisName;
          HashMap nodeComponents = new HashMap();
          allComponents.put(thisName, nodeComponents);
          // Space for node agent components
          nodeComponents.put(
            insertionPointContainer(Agent.INSERTION_POINT),
            new ArrayList());
          thisNode = true;
        } else {
          currentParent = null;
          thisNode = false;
        }
      }
      if (!thisNode)
        return;
      if (localName.equals("agent")) {

        String name = atts.getValue("name");
        if (logger.isDebugEnabled())
          logger.debug("started element for agent " + name);
        assert(allComponents.get(name) == null);
        currentParent = name;
        // make a new place for the agent's components
        Map byInsertionPoint = new HashMap();
        allComponents.put(name, byInsertionPoint);

        // Add this agent to the Node
        Map currentAgent = new HashMap();
        currentAgent.put("class", atts.getValue("class"));
        currentAgent.put("insertionpoint", Agent.INSERTION_POINT);
        currentAgent.put("priority", stdPriority);
        ArrayList param = new ArrayList();
        currentAgent.put("name", name);
        param.add(name);
        currentAgent.put("arguments", param);
        byInsertionPoint = (Map) allComponents.get(nodename);
        List theNodesAgents =
          (List) byInsertionPoint.get(
            insertionPointContainer(Agent.INSERTION_POINT));
        theNodesAgents.add(currentAgent);
      } else if (localName.equals("component")) {

        currentComponent = new HashMap();
        currentComponent.put("name", atts.getValue("name"));
        currentComponent.put("class", atts.getValue("class"));
        currentComponent.put("priority", atts.getValue("priority"));
        String insertionPoint = atts.getValue("insertionpoint");
        currentComponent.put("insertionpoint", insertionPoint);
        Map byInsertionPoint = (Map) allComponents.get(currentParent);
        List componentList =
          (List) byInsertionPoint.get(insertionPointContainer(insertionPoint));
        if (componentList == null) {
          componentList = new ArrayList();
          byInsertionPoint.put(
            insertionPointContainer(insertionPoint),
            componentList);
        }
        componentList.add(currentComponent);

      } else if (localName.equals("argument")) {
        assert(currentComponent != null);
        assert(chars == null);
        chars = new CharArrayWriter();
      }

    }

    /**
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
      throws SAXException {
      if (!thisNode)
        return;
      if (chars != null)
        chars.write(ch, start, length);
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName)
      throws SAXException {
      if (!thisNode)
        return;
      if (localName.equals("argument")) {
        ArrayList argumentList = (ArrayList) currentComponent.get("arguments");
        if (argumentList == null) {
          argumentList = new ArrayList();
          currentComponent.put("arguments", argumentList);
        }
        String argument = chars.toString().trim();
        argumentList.add(argument);
        chars = null;
      } else if (localName.equals("component")) {
        assert(currentComponent != null);
        currentComponent = null;
      } else if (localName.equals("agent")) {
        currentParent = nodename;
        if (logger.isDebugEnabled())
          logger.debug("finished a agent");
      } else if (localName.equals("node")) {
        thisNode = false;
        currentParent = null;
        if (logger.isDebugEnabled())
          logger.debug("finished a node");
      }
    }

    /**
     * @see org.xml.sax.ErrorHandler#error(SAXParseException)
     */
    public void error(SAXParseException exception) throws SAXException {
      logger.error("Error parsing the file", exception);
      super.error(exception);
    }

    /**
     * @see org.xml.sax.ErrorHandler#warning(SAXParseException)
     */
    public void warning(SAXParseException exception) throws SAXException {
      logger.warn("Warning parsing the file", exception);
      super.warning(exception);
    }

  }

}
