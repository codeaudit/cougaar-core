/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Class to provide service for initializing Components
 * from an XML file.
 * <p> 
 * <pre>
 * @property org.cougaar.society.file
 *   The name of the XML file from which to read this Node's
 *   definition
 * </pre> 
 */ 
public class XMLComponentInitializerServiceProvider
  implements ServiceProvider {

  private static final String XML_FILE_NAME_PROP = 
    "org.cougaar.society.file";
  private static final String XML_FILE_NAME = 
    System.getProperty(XML_FILE_NAME_PROP);

  private static final String NODE_NAME_PROP =
    "org.cougaar.node.name";
  private static final String NODE_NAME =
    System.getProperty(NODE_NAME_PROP);

  // backwards compatibility for the wp server:
  private static final String IMPLICIT_WP_SERVER_PROP = 
    "org.cougaar.core.load.wp.server";
  private static final boolean IMPLICIT_WP_SERVER = 
    PropertyParser.getBoolean(IMPLICIT_WP_SERVER_PROP, true);

  private static final String MY_CLASS_NAME =
    XMLComponentInitializerServiceProvider.class.getName();

  private final ComponentInitializerService serviceImpl =
      new ComponentInitializerServiceImpl();

  public XMLComponentInitializerServiceProvider() {
  }
  
  private static Map get_overrides() {
    Map ret = new HashMap();
    // add node name
    if (NODE_NAME != null && !NODE_NAME.equals("")) {
      ret.put("node", NODE_NAME);
    }
    // backwards compatibility for the wp server:
    if (!IMPLICIT_WP_SERVER) {
      ret.put("wpserver", "false");
    }
    return ret;
  }

  public Object getService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass) {
    return 
      ((serviceClass == ComponentInitializerService.class) ?
       (serviceImpl) : 
       null);
  }

  public void releaseService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass,
      Object service) {
  }

  private static class ComponentInitializerServiceImpl
      implements ComponentInitializerService {

        private static final ComponentDescription[] NO_COMPONENTS =
          new ComponentDescription[0];

        private final Logger logger;

        // could make this a map of soft references
        private final Map agents;

        public ComponentInitializerServiceImpl() {
          this.logger = Logging.getLogger(MY_CLASS_NAME);
          this.agents = parseAgents();
        }

        private Map parseAgents() {
          if (XML_FILE_NAME == null) {
            throw new RuntimeException(
                "\"-D"+XML_FILE_NAME_PROP+"\" XML filename not set");
          } 

          if (logger.isShoutEnabled()) {
            logger.shout(
                "Initializing node \""+NODE_NAME+
                "\" from XML file \""+XML_FILE_NAME+"\"");
          }

          Map ret = 
            XMLConfigParser.parseAgents(
                XML_FILE_NAME,
                NODE_NAME,
                null, // all agents in this file
                get_overrides());
          if (logger.isDetailEnabled()) {
            logger.detail("found "+ret.size()+" agents");
          }

          // we should minimally have the node-agent's config
          if (ret.isEmpty()) {
            throw new RuntimeException(
                "The configuration for node \""+NODE_NAME+ 
                "\" was not found in XML file \""+XML_FILE_NAME+"\"");
          }

          return ret;
        }

        /**
         * Get the descriptions of components with the named parent having
         * an insertion point below the given container insertion point.
         **/
        public ComponentDescription[] getComponentDescriptions(
            String agentName,
            String containmentPoint)
          throws InitializerException {

          if (logger.isInfoEnabled()) {
            logger.info(
                "Looking for direct sub-components of "+agentName+
                " just below insertion point "+
                containmentPoint);
          }

          List agentDescs;
          try {
            agentDescs = getAgentDescs(agentName);
          } catch (Exception e) {
            logger.error(
                "Failed getComponentDescriptions("+agentName+")",
                e);
            throw new InitializerException(
                "getComponentDescriptions("+agentName+")",
                e);
          }

          ComponentDescription[] ret = 
            filterByContainmentPoint(
                agentDescs, containmentPoint);

          if (logger.isDetailEnabled()) {
            StringBuffer buf = new StringBuffer();
            buf.append("returning ");
            buf.append(ret.length);
            buf.append(" component descriptions: ");
            for (int i = 0; i < ret.length; i++) {
              appendDesc(buf, ret[i]);
            }
            logger.detail(buf.toString());
          }

          return ret;
        }
        
        private List getAgentDescs(
            String agentName) throws Exception {
          if (agentName == null) {
            throw new IllegalArgumentException("null agentName");
          }

          // check already parsed XML_FILE_NAME
          List l = (List) agents.get(agentName);
          if (l != null) {
            if (logger.isDebugEnabled()) {
              logger.debug("Found cached agent "+agentName+" config");
            }
            return l;
          }

          // look for agent.xml
          //
          // we could cache a parse failure, but these are probably
          // rare 
          if (logger.isInfoEnabled()) {
            logger.info("Parsing "+agentName+".xml");
          }
          Map m = 
            XMLConfigParser.parseAgents(
                agentName+".xml",
                null, // no "<node>"
                agentName, // just this agent
                get_overrides());
          l = (List) m.get(agentName);
          if (logger.isDetailEnabled()) {
            logger.detail(
                "found "+(l == null ? 0 : l.size())+" components");
          }

          // cache
          agents.put(agentName, l);

          return l;
        }

        private ComponentDescription[] filterByContainmentPoint(
            List l,
            String containmentPoint) {
          if (l == null ||
              l.isEmpty()) {
            return NO_COMPONENTS;
          }
          List retList = null;
          for (int i = 0, n = l.size(); i < n; i++) {
            ComponentDescription cd =
              (ComponentDescription) l.get(i);
            String ip = cd.getInsertionPoint();
            if (ip.startsWith(containmentPoint) &&
                ip.indexOf('.', containmentPoint.length()+1) < 0) {
              if (retList == null) {
                retList = new ArrayList();
              }
              retList.add(cd);
            }
          }
          if (retList == null) {
            return NO_COMPONENTS;
          }
          return (ComponentDescription[])
            retList.toArray(
                new ComponentDescription[retList.size()]);
        }
        
        private static void appendDesc(
            StringBuffer buf, ComponentDescription cd) {
          buf.append("\n   <component name=\'");
          buf.append(cd.getName());
          buf.append("\' class=\'");
          buf.append(cd.getClassname());
          buf.append("\' priority=\'");
          buf.append(cd.getPriority());
          buf.append("\' insertionpoint=\'");
          buf.append(cd.getInsertionPoint());
          buf.append("\'");
          Object o = cd.getParameter();
          if (o == null) {
            buf.append("/>");
            return;
          }
          buf.append(">");
          if (o instanceof List) {
            List l = (List) o;
            for (int i = 0, n = l.size(); i < n; i++) {
              buf.append("\n    <argument>");
              buf.append(l.get(i));
              buf.append("</argument>");
            }
          } else {
            buf.append("\n    <argument>");
            buf.append(o);
            buf.append("</argument>");
          }
          buf.append("\n   </component>");
        }
  }
}
