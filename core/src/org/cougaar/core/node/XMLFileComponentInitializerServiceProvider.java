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

import java.io.*;
import java.util.*;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.*;
import org.cougaar.core.node.ComponentInitializerService.InitializerException;
import org.cougaar.util.log.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

class XMLFileComponentInitializerServiceProvider implements ServiceProvider {

	private String filename;
	private String nodename;
  Logger logger;
  HashMap allComponents = new HashMap();
  /*
   * "allComponents" is a mapping of of the name of the parent component (String)
   *   to a HashMap.  There is one entry in here for each agent (including the node agent)
   * This HashMap is a mapping of insertion point container names (ie the  insertion
   *   point truncated at the last '.') -- a String -- to a TreeSet. The tree set 
   *   contains the list of components to be inserted at this container.
   * This TreeSet is an ordered list of HashMaps. Each HashMap is wrapped in an OrderedThing
   *   to accommodate its order. Each HashMap maps property names 
   *   (String) like "insertionpoint" to String values. A special value in the HashMap is
   *   "arguments" which maps to a TreeSet of strings (wrapped in OrderedThing objects)
   *   that are the component's arguments. 
   */

  private static String insertionPointContainer(String insertionPoint) {
    return insertionPoint.substring(0, insertionPoint.lastIndexOf('.'));
  }
    
	public XMLFileComponentInitializerServiceProvider() {
    this.logger = Logging.getLogger(getClass());

		filename = System.getProperty("org.cougaar.society.file");
		nodename = System.getProperty("org.cougaar.node.name");
		try {
			parseFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseFile()
		throws FileNotFoundException, IOException, SAXException {
		XMLReader xr = new org.apache.crimson.parser.XMLReaderImpl();
		MyHandler handler = new MyHandler();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		FileReader r = new FileReader(filename);
		xr.parse(new InputSource(r));
		r.close();
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

		private ComponentDescription[] noComponents =
			new ComponentDescription[0];
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
          HashMap byParent = (HashMap)allComponents.get(parentName);
          if (byParent != null) {
            TreeSet byInsertionPoint = (TreeSet)byParent.get(containerInsertionPoint);
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
			return ret;
		}
	}
  
  private ComponentDescription[] makeComponents(Set components) {
		ComponentDescription[] ret = new ComponentDescription[components.size()];
		Iterator componentEnum = components.iterator();
		int i = 0;
		while (componentEnum.hasNext()) {
      OrderedThing orderedComponent = (OrderedThing)componentEnum.next();
      HashMap componentProps = (HashMap) orderedComponent.thing;
      String name = (String) componentProps.get("name");
      String classname = (String) componentProps.get("class");
      String priority = (String) componentProps.get("priority");
      String insertionPoint = (String) componentProps.get("insertionpoint");
      String order = (String) componentProps.get("order");
      TreeSet args = (TreeSet) componentProps.get("arguments");
      Vector vParams = null;
      if ((args != null) && (args.size() > 0)) {
        vParams = new Vector(args.size());
        Iterator argsIter = args.iterator();
        while (argsIter.hasNext()) {
          OrderedThing arg = (OrderedThing) argsIter.next();
          vParams.addElement(arg.thing);
        }
      }
      ret[i++] =
          makeComponentDesc(vParams, classname, priority, insertionPoint);
		}
		return ret;
	}

  private ComponentDescription makeComponentDesc(
    Vector vParams,
    String classname,
    String priority,
    String insertionPoint) {
          return new ComponentDescription(
            classname,
            insertionPoint,
            classname,
            null,
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
		double currentComponentOrder;
		double currentArgumentOrder;
		CharArrayWriter chars;
    private final String stdPriority = ComponentDescription.priorityToString(ComponentDescription.PRIORITY_STANDARD);

		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
			if (localName.equals("node")) {
				String thisName = atts.getValue("name");
				if (nodename.equals(thisName)) {
          currentParent = thisName;
          HashMap nodeComponents = new HashMap();
          allComponents.put(thisName, nodeComponents); // Space for node agent components
          nodeComponents.put(insertionPointContainer(Agent.INSERTION_POINT), new TreeSet());
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
        TreeSet param = new TreeSet();
        currentAgent.put("name", name);
        param.add(new OrderedThing(0.0, name));
        currentAgent.put("arguments", param);
        byInsertionPoint = (Map)allComponents.get(nodename);
        Set theNodesAgents = (Set)byInsertionPoint.get(insertionPointContainer(Agent.INSERTION_POINT));
        theNodesAgents.add(new OrderedThing(0.0, currentAgent));
			} else if (localName.equals("component")) {
        
				currentComponent = new HashMap();
				currentComponentOrder =
					Double.parseDouble(atts.getValue("order"));
				currentComponent.put("name", atts.getValue("name"));
				currentComponent.put("class", atts.getValue("class"));
				currentComponent.put("priority", atts.getValue("priority"));
        String insertionPoint = atts.getValue("insertionpoint");
        currentComponent.put("insertionpoint", insertionPoint);
				currentComponent.put("order", atts.getValue("order"));
        Map byInsertionPoint = (Map)allComponents.get(currentParent);
        Set componentList = (Set)byInsertionPoint.get(insertionPointContainer(insertionPoint)); 
        if (componentList == null) {
          componentList = new TreeSet();
          byInsertionPoint.put(insertionPointContainer(insertionPoint), componentList);
        }
        componentList.add(new OrderedThing(currentComponentOrder, currentComponent));
        
			} else if (localName.equals("argument")) {
				assert(currentComponent != null);
				assert(chars == null);
				currentArgumentOrder =
					Double.parseDouble(atts.getValue("order"));
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
		public void endElement(
			String namespaceURI,
			String localName,
			String qName)
			throws SAXException {
			if (!thisNode)
				return;
			if (localName.equals("argument")) {
				TreeSet argumentList =
					(TreeSet) currentComponent.get("arguments");
				if (argumentList == null) {
					argumentList = new TreeSet();
					currentComponent.put("arguments", argumentList);
				}
				String argument = chars.toString().trim();
				argumentList.add(
					new OrderedThing(currentArgumentOrder, argument));
				chars = null;
				currentArgumentOrder = -1;
			} else if (localName.equals("component")) {
				assert(currentComponent != null);
				currentComponent = null;
			} else if (localName.equals("agent")) {
        currentParent = nodename;
			} else if (localName.equals("node")) {
				thisNode = false;
        currentParent = null;
			}
		}

		/**
		 * @see org.xml.sax.ErrorHandler#error(SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException {
			super.error(exception);
		}

		/**
		 * @see org.xml.sax.ErrorHandler#warning(SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException {
			super.warning(exception);
		}

	}

	private static class OrderedThing implements Comparable {
		public OrderedThing(double order, Object thing) {
			this.order = order;
			this.thing = thing;
		}
		public double order;
		public Object thing;
		/**
		 * @see java.lang.Comparable#compareTo(Object)
		 */
		public int compareTo(Object o) {
			OrderedThing other = (OrderedThing) o;
			if (this.order < other.order)
				return -1;
			if (this.order > other.order)
				return 1;
      // Can't let them be equal or they collide in the TreeSet
      if (this.thing.hashCode() < other.thing.hashCode())
        return -1;
			return 1;
		}
	}

}
