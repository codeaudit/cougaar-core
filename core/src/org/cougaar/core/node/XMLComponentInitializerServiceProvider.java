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

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Map;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.*;
import org.cougaar.util.log.*;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.PropertyParser;

/**
 * Class to provide service for initializing Components
 * from an XML file.
 *
 * <pre>
 * @property org.cougaar.society.file
 *   The name of the XML file from which to read this Node's
 *   definition
 * @property org.cougaar.node.name
 *   The name for this Node.
 * @property org.cougaar.society.xml.validate 
 *   Indicates if the XML parser should be validating or not.
 *   Defaults to "false".
 * @property org.cougaar.node.validate
 *   Same as "-Dorg.cougaar.society.xml.validate" 
 * @property org.cougaar.society.xsl.checkXML
 *    Check for an XSL stylesheet, e.g.:
 *      &lt;?xml-stylesheet type="text/xml" href="society.xsl"?&gt;
 *    Defaults to "true".
 * @property org.cougaar.society.xsl.default.file
 *    Default XSL stylesheet if "-Dorg.cougaar.society.xsl.checkXML"
 *    is false or an xml-stylesheet is not found.  Defaults to
 *    null. 
 * @property org.cougaar.society.xsl.dynamic.file
 *    Dynamic XSL stylesheet that generates the XSL stylesheet
 *    based upon the XML file contents, unless an XSL stylesheet
 *    is specified in the XML file
 *    (-Dorg.cougaar.society.xsl.checkXML) or specified
 *    (-Dorg.cougaar.society.xsl.default.file).  Defaults to
 *    "make_society.xsl".
 * </pre>
 **/
public class XMLComponentInitializerServiceProvider
  implements ServiceProvider {

  private static final String XML_FILE_NAME = 
    System.getProperty("org.cougaar.society.file");

  private static final String NODE_NAME =
    System.getProperty("org.cougaar.node.name");

  private static final boolean VALIDATE =
    Boolean.getBoolean("org.cougaar.society.xml.validate") ||
    Boolean.getBoolean("org.cougaar.core.node.validate");

  private static final boolean USE_XML_STYLESHEET = 
    PropertyParser.getBoolean(
        "org.cougaar.society.xsl.checkXML",
        true);

  private static final String DEFAULT_XSL_FILE_NAME =
    System.getProperty(
        "org.cougaar.society.xsl.default.file",
        null);

  private static final String DYNAMIC_XSL_FILE_NAME = 
    System.getProperty(
        "org.cougaar.society.xsl.dynamic.file",
        "make_society.xsl");

  // could make this a soft reference, since we can always
  // reparse our xml file
  private final ComponentInitializerService serviceImpl;

  public XMLComponentInitializerServiceProvider() {

    //
    // parse the config when this class is constructed!
    //

    String filename = XML_FILE_NAME;
    if (filename == null || filename.equals("")) {
      throw new RuntimeException(
          "Missing \"-Dorg.cougaar.society.file=STRING\" system"+
          " property that specifies the XML config file");
    }

    String nodename = NODE_NAME;
    if (nodename == null || nodename.equals("")) {
      throw new RuntimeException(
          "Missing \"-Dorg.cougaar.node.name=STRING\" system property"+
          " that specifies the local node's name");
    }

    // for backwards compatibility we need the wpserver parameter.
    //
    // We could add other -D options here, e.g.
    //   Properties props = 
    //     SystemProperties.getSystemPropertiesWithPrefix(
    //       "org.cougaar.society.xsl.param");
    //   Map m = <convert props to map> 
    // However, this would probably just complicate the config.
    Map default_xsl_params =
      Collections.singletonMap(
          "wpserver",
          System.getProperty(
            "org.cougaar.core.load.wp.server",
            "true"));

    Map dynamic_xsl_params = null;

    Logger logger = Logging.getLogger(getClass());
    if (logger.isShoutEnabled()) {
      logger.shout(
          "Initializing node \""+nodename+
          "\" from XML file \""+ filename+"\"");
    }

    Map agents;
    try {
      XMLConfigParser xcp = 
        new XMLConfigParser(
            filename,
            nodename,
            VALIDATE,
            USE_XML_STYLESHEET,
            DEFAULT_XSL_FILE_NAME,
            DYNAMIC_XSL_FILE_NAME,
            default_xsl_params,
            dynamic_xsl_params);
      agents = xcp.parseFile();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to parse XML file ("+
            "filename="+filename+
            ", nodename="+nodename+
            ", validate="+VALIDATE+
            ", use_xml_stylesheet="+USE_XML_STYLESHEET+
            ", default_xsl_file_name="+DEFAULT_XSL_FILE_NAME+
            ", dynamic_xsl_file_name="+DYNAMIC_XSL_FILE_NAME+
            ", default_xsl_params="+default_xsl_params+
            ", dynamic_xsl_params="+dynamic_xsl_params+
            ")",
          e);
    }

    // agents fully initialized

    if (agents.isEmpty()) {
      throw new RuntimeException(
          "The configuration for node \""+nodename+ 
          "\" was not found in XML file \""+filename+"\"");
    }

    serviceImpl = 
      new ComponentInitializerServiceImpl(
          logger,
          agents);
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
        private final Map agents;

        public ComponentInitializerServiceImpl(
            Logger logger,
            Map agents) {
          this.logger = logger;
          this.agents = agents;
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

          ComponentDescription[] ret = NO_COMPONENTS;
          try {
            List l = (List) agents.get(agentName);
            if (l != null) {
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
              if (retList != null) {
                ret = (ComponentDescription[])
                  retList.toArray(
                      new ComponentDescription[retList.size()]);
              }
            }
          } catch (Exception e) {
            throw new InitializerException(
                "getComponentDescriptions("+agentName+", "+
                containmentPoint+")",
                e);
          }

          if (logger.isInfoEnabled()) {
            StringBuffer buf = new StringBuffer();
            buf.append("Returning ");
            buf.append(ret.length);
            buf.append(" component descriptions: ");
            for (int i = 0; i < ret.length; i++) {
              appendDesc(buf, ret[i]);
            }
            logger.info(buf.toString());
          }

          return ret;
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
