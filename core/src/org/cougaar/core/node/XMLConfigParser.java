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

import java.util.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
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
import org.cougaar.core.component.*;
import org.cougaar.util.log.*;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Strings;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parses an XML society configuration file into
 * ComponentDescriptions, applying optional XSL transforms.
 */
public final class XMLConfigParser {

  private final String xml_input_file;
  private final String nodename;
  private final boolean validate;
  private final boolean use_xml_stylesheet;
  private final String t2_xsl_input_file;
  private final String t1_xsl_input_file;
  private final Map t1_xsl_params;
  private final Map t2_xsl_params;

  private Logger logger;

  private Map agents;
  private ContentHandler handler;

  private ConfigResolver config_resolver;
  private SAXTransformerFactory saxTFactory;
  private XMLReader xml_reader;

  private Thread t1_pipe_thread;
  private TransformerHandler t2_transformer_handler;

  /**
   * Create an XMLConfigParser.
   * <p>
   * Note that this class is <i>not</i> thread-safe!  Clients should
   * create a new instance per call or use an outer synchronize lock.
   * 
   * @param xml_file_name name of the XML file, e.g. "mySociety.xml"
   * @param nodename name of the node, e.g. "1AD_TINY"
   * @param validate validate the XML file content format
   * @param use_xml_stylesheet check for an XML file stylesheet
   *    instruction, e.g.:
   *       "&lt;?xml-stylesheet type="text/xml" href="society.xsl"?&gt;"
   * @param default_xsl_file_name if !use_xml_stylesheet, or the XML
   *    file does not specify an XSL stylesheet, then use this file,
   *    e.g. "society.xsl"
   * @param dynamic_xsl_file_name if !use_xml_stylesheet, or the XML
   *    file does not specify an XSL stylesheet, and the
   *    default_xsl_file_name is null, then use this XSL file to
   *    dynamically generate the XSL file, e.g. "make_society.xsl"
   * @param default_xsl_params if the default_xsl_file_name will be
   *    used, set these XSL parameters for that template file, e.g.
   *    "wpserver=false"
   * @param dynamic_xsl_params if the dynamic_xsl_file_name will be
   *    used, set these XSL parameters for that template file, e.g.
   *    "defaultAgent=SimpleAgent.xsl"
   */ 
  public XMLConfigParser(
      String xml_file_name,
      String nodename,
      boolean validate,
      boolean use_xml_stylesheet,
      String default_xsl_file_name,
      String dynamic_xsl_file_name,
      Map default_xsl_params,
      Map dynamic_xsl_params) {
    // rename & save
    this.xml_input_file = xml_file_name;
    this.nodename = nodename;
    this.validate = validate;
    this.use_xml_stylesheet = use_xml_stylesheet;
    this.t2_xsl_input_file = default_xsl_file_name;
    this.t1_xsl_input_file = dynamic_xsl_file_name;
    this.t2_xsl_params = default_xsl_params;
    this.t1_xsl_params = dynamic_xsl_params;
  }

  /**
   * Parse an XML file into a Map of per-agent ComponentDescription
   * Lists.
   *
   * @throws ParseException if there's a parsing error
   */ 
  public Map parseFile() throws ParseException {

    if (agents != null) {
      // already parsed?
      return agents;
    }

    try {
      // create logger
      logger = Logging.getLogger(getClass());
      if (logger.isInfoEnabled()) {
        logger.info(
            "Parsing"+
            (validate ? " validated" : "")+
            " XML file \""+ xml_input_file+"\"");
      }

      // create content handler
      agents = new HashMap(11);
      handler = new MyHandler(agents, nodename, logger);

      // create config reader
      config_resolver =
        new ConfigResolver(
            ConfigFinder.getInstance(),
            logger);

      // create transformer factory
      TransformerFactory tFactory =
        TransformerFactory.newInstance();
      if ((!tFactory.getFeature(SAXSource.FEATURE)) ||
          (!tFactory.getFeature(SAXResult.FEATURE))) {
        throw new RuntimeException(
            "XSLT TransformerFactory doesn't support the "+
            "\"SAXSource.FEATURE\" and/or the \"SAXResult.FEATURE\"");
          }
      saxTFactory = (SAXTransformerFactory) tFactory;
      saxTFactory.setURIResolver(config_resolver);

      // create reusable xml reader
      xml_reader = XMLReaderFactory.createXMLReader();
      xml_reader.setEntityResolver(config_resolver);

      // find the appropriate t2_transformer_handler.
      //
      // if dynamic xsl is used then a "t1_pipe_thread" will be
      // launched.
      //
      // if xsl is disabled then both the "t2_transformer_handler" and
      // "t1_pipe_thread" will be null.
      findTransformerHandler();

      // enable optional validation
      if (validate) { 
        xml_reader.setFeature(
            "http://xml.org/sax/features/validation",
            validate);
      }

      // set XML content handler to our handler
      if (t2_transformer_handler == null) {
        xml_reader.setContentHandler(handler);
      } else {
        prepareTransformerHandler();
      }

      // open society xml file (again if we're using XSL!)
      InputStream t2_xml_input_stream = 
        config_resolver.open(xml_input_file);

      // parse
      xml_reader.parse(new InputSource(t2_xml_input_stream));

      if (t1_pipe_thread != null) {
        // wait for dynamic xsl thread
        joinForPipeThread();
      }

    } catch (Exception e) {

      // examine exception and generate a better one
      throw cleanupException(e);

    } finally {
      // cleanup
      logger = null;
      handler = null;
      config_resolver = null; 
      saxTFactory = null; 
      xml_reader = null; 
      t1_pipe_thread = null;
      t2_transformer_handler = null;
    }

    return agents;
  }
  
  private void findTransformerHandler()
    throws IOException, TransformerConfigurationException, SAXException {

    if (use_xml_stylesheet) {
      // look for xsl header in xml file, e.g.:
      //  <?xml-stylesheet type="text/xml" href="society.xsl"?>
      findXmlStylesheetTransformerHandler();
      if (t2_transformer_handler != null) {
        return;
      }
    }

    if (t1_xsl_input_file == null) {
      if (t2_xsl_input_file == null) {
        // no xsl!   leave t2_transformer_handler as null
        if (logger.isInfoEnabled()) {
          logger.info("Not using XSL");
        }
        return;
      }

      // use default xsl (e.g. "society.xsl")
      findDefaultTransformerHandler();
      return;
    }
   
    if (t2_xsl_input_file == null) {
      // dynamic xsl (e.g. "make_society.xsl")
      //
      // also launches t1_pipe_thread
      findDynamicTransformerHandler();
      return;
    }

    // error
    throw new RuntimeException(
        "Specified both a dynamic XSL generator ("+
        t1_xsl_input_file+") and a default XSL ("+
        t2_xsl_input_file+")");
  }

  private void findXmlStylesheetTransformerHandler()
    throws IOException, TransformerConfigurationException {

    // look for xsl header in xml file, e.g.:
    //  <?xml-stylesheet type="text/xml" href="society.xsl"?>

    InputStream pi_xml_input_stream =
      config_resolver.open(xml_input_file);

    Source stylesheet_source = 
      saxTFactory.getAssociatedStylesheet(
          new StreamSource(pi_xml_input_stream),
          null,  // media
          null,  // title
          null); // charset

    if (logger.isInfoEnabled()) {
      logger.info(
          (stylesheet_source == null ?
           "Did not find associated XSL stylesheet" :
           ("Found associated XSL stylesheet: "+
            stylesheet_source.getSystemId())));
    }

    if (stylesheet_source == null) {
      return;
    }

    t2_transformer_handler =
      saxTFactory.newTransformerHandler(
          stylesheet_source);
  }

  private void findDefaultTransformerHandler() 
    throws IOException, TransformerConfigurationException {

    // explicit xsl (e.g. "society.xsl")
    if (logger.isInfoEnabled()) {
      logger.info(
          "Using default XSL stylesheet: "+t2_xsl_input_file);
    }

    InputStream t2_xsl_input_stream =
      config_resolver.open(t2_xsl_input_file);

    t2_transformer_handler =
      saxTFactory.newTransformerHandler(
          new StreamSource(t2_xsl_input_stream));
  }

  private void findDynamicTransformerHandler() 
    throws IOException, TransformerConfigurationException, SAXException {

    // dynamically generate xsl (e.g. "make_society.xsl")
    //
    // run the t1_xsl file on the xml file to generate an
    // in-memory t2_xsl.  We use a threaded pipe to connect the
    // t1_xsl output to our handler's transformer.
    //
    // t1:   (xsl generator)
    //   XML input:  "xml_input_file"
    //   XSL input:  "make_society.xsl"
    //   XSL output: pipe_out
    // t2:   (the generated xsl)
    //   XML input:  "xml_input_file"
    //   XSL input:  pipe_in
    //   XML output: handler
    // handler:
    //   parse config

    if (logger.isInfoEnabled()) {
      logger.info(
          "Using dynamic XSL stylesheet: "+t1_xsl_input_file);
    }

    final PipedOutputStream t1_pipe_output_stream =
      new PipedOutputStream();
    PipedInputStream t2_pipe_input_stream =
      new PipedInputStream();
    t1_pipe_output_stream.connect(t2_pipe_input_stream);

    // perform these actions in our thread, to simplify
    // debugging
    final InputStream t1_xml_input_stream;
    final Transformer t1_transformer;
    {
      // open society xml file
      t1_xml_input_stream =
        config_resolver.open(xml_input_file);

      // open preprocessing xsl file
      InputStream t1_xsl_input_stream =
        config_resolver.open(t1_xsl_input_file);

      // create outer transform handler
      t1_transformer = 
        saxTFactory.newTransformer(
            new StreamSource(t1_xsl_input_stream));
      t1_transformer.setURIResolver(config_resolver);

      // set optional node filter to help trim xsl
      t1_transformer.setParameter("node", nodename);

      // set optional xsl parameters
      if (t1_xsl_params != null) { 
        for (Iterator iter = t1_xsl_params.entrySet().iterator();
            iter.hasNext();
            ) {
          Map.Entry me = (Map.Entry) iter.next();
          String key = (String) me.getKey();
          String value = (String) me.getValue();
          t1_transformer.setParameter(key, value);
        }
      }
    }

    // create runnable to transform our xml to the xsl pipe
    Runnable r1 = new Runnable() {
      public void run() {
        try {
          // transform to xsl pipe
          t1_transformer.transform(
              new StreamSource(t1_xml_input_stream),
              new StreamResult(t1_pipe_output_stream));
        } catch (Exception e) {
          logger.error("failed", e);
        } finally {
          try {
            t1_pipe_output_stream.flush();
            t1_pipe_output_stream.close();
          } catch (Exception e2) {
            logger.error("failed", e2);
          }
        }
      }
    };

    // launch our t1 pipe thread (no ThreadService yet!)
    t1_pipe_thread = new Thread(r1); 
    t1_pipe_thread.start();

    // read templates from xsl pipe
    TemplatesHandler t2_templates_handler =
      saxTFactory.newTemplatesHandler();
    xml_reader.setContentHandler(t2_templates_handler);
    xml_reader.parse(new InputSource(t2_pipe_input_stream));
    Templates t2_templates = t2_templates_handler.getTemplates();

    // create transformer
    t2_transformer_handler =
      saxTFactory.newTransformerHandler(t2_templates);
  }

  private void prepareTransformerHandler() 
    throws SAXNotRecognizedException, SAXNotSupportedException {
    Transformer t2_transformer =
      t2_transformer_handler.getTransformer(); 

    // set config finder
    t2_transformer.setURIResolver(config_resolver);

    // set optional node filter to help trim handler input
    t2_transformer.setParameter("node", nodename);

    // set optional xsl parameters
    if (t2_xsl_params != null) { 
      for (Iterator iter = t2_xsl_params.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String key = (String) me.getKey();
        String value = (String) me.getValue();
        t2_transformer.setParameter(key, value);
      }
    }

    xml_reader.setContentHandler(t2_transformer_handler);
    xml_reader.setProperty(
        "http://xml.org/sax/properties/lexical-handler", 
        t2_transformer_handler);

    // set our handler
    t2_transformer_handler.setResult(new SAXResult(handler));
  }

  private void joinForPipeThread() 
    throws InterruptedException {
    if (t1_pipe_thread != null && t1_pipe_thread.isAlive()) {
      // this is unusual, since the t1 transform should close
      // the pipe and finish running.
      if (logger.isWarnEnabled()) {
        logger.warn(
            "Waiting for XSL transform pipe to finish,"+
            " possible deadlock or IO problem!");
      }
      t1_pipe_thread.join();
      if (logger.isWarnEnabled()) {
        logger.warn("XSL transform pipe finished");
      }
    }
  }

  private ParseException cleanupException(Exception e) {

    String msg =
        "Exception parsing society XML file "+xml_input_file;
    Exception cause = e;

    if (e instanceof SAXException) {
      SAXException sx = (SAXException) e;
      // extract nested sax exception
      String locator = null;
      if (sx instanceof SAXParseException) {
        SAXParseException spx = (SAXParseException) sx;
        msg +=
          ", locator="+
          "(publicId="+spx.getPublicId()+
          " systemId="+spx.getSystemId()+
          " lineNumber="+spx.getLineNumber()+
          " columnNumber="+spx.getColumnNumber()+
          ")";
      }
      Exception e2 = sx.getException();
      if (e2 != null) {
        cause = e2;
      }
    }

    return new ParseException(msg, cause);
  }

  public static class ParseException 
      extends Exception {
        public ParseException(String s) {
          super(s);
        }
        public ParseException(String s, Throwable t) {
          super(s, t);
        }
        public ParseException(Throwable t) {
          super(t);
        }
  }

  /** adapt a ConfigFinder to an XML EntityResolver + URIResolver **/
  private static class ConfigResolver 
      implements EntityResolver, URIResolver {

        private final ConfigFinder configFinder;
        private final Logger logger;

        public ConfigResolver(
            ConfigFinder configFinder,
            Logger logger) {
          this.configFinder = configFinder;
          this.logger = logger;
        }

        // ConfigFinder:
        public InputStream open(String aURL) throws IOException {
          return configFinder.open(aURL);
        }

        // EntityResolver:
        public InputSource resolveEntity(
            String publicId, String systemId)
          throws SAXException, IOException {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "XSL resolve entity (publicId="+publicId+
                ", systemId="+systemId+")");
          }
          InputStream is;
          try {
            is = configFinder.open(systemId);
          } catch (Exception e) {
            throw new SAXException(
                "Unable to resolve entity (publicId="+
                publicId+", systemId="+systemId+")",
                e);
          }
          return new InputSource(is);
            }

        // URIResolver:
        public Source resolve(String href, String base)
          throws TransformerException {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "XSL resolve URI (href="+href+", base="+base+")");
          }
          InputStream is;
          try {
            is = configFinder.open(href);
          } catch (Exception e) {
            throw new TransformerException(
                "Unable to resolve URI (href="+href+", base="+
                base+")",
                e);
          }
          return new StreamSource(is);
        }
  }

  /**
   * SAX ContextHandler that handles the society config parsing.
   */
  private static class MyHandler extends DefaultHandler {

    private static final int STATE_INITIAL = 0;
    private static final int STATE_PARSING = 1;
    private static final int STATE_PARSED = 2;

    private final Map agents;
    private final String nodename;
    private final Logger logger;

    private final Map currentComponent = new HashMap(11);
    private final CharArrayWriter argValueBuffer = new CharArrayWriter();

    private boolean thisNode;
    private List currentList;
    private String currentAgent;
    private boolean inArgument;

    private int state = STATE_INITIAL;

    private static final String AGENT_PRIORITY =
      ComponentDescription.priorityToString(
          ComponentDescription.PRIORITY_STANDARD);

    public MyHandler(
        Map ret,
        String nodename,
        Logger logger) {
      this.agents = ret;
      this.nodename = nodename;
      this.logger = logger;
    }
    
    public void startDocument() {
      // not thread-safe, but close enough to warn developers
      if (state != STATE_INITIAL) {
        throw new RuntimeException(
            "ContentHandler "+
            (state == STATE_PARSING ?
             "already parsing" :
             "completed parsing, please create a new instance"));
      }
      state = STATE_PARSING;
    }

    public void endDocument() {
      if (state != STATE_PARSING) {
        throw new RuntimeException(
            "ContentHandler "+
            (state == STATE_INITIAL ?
             "never started" :
             "already closed")+
            " document?");
      }
      state = STATE_PARSED;
    }

    // begin element
    public void startElement(
        String namespaceURI,
        String localName,
        String qName,
        Attributes atts)
      throws SAXException {

      if (logger.isDetailEnabled()) {
        StringBuffer buf = new StringBuffer();
        buf.append("startElement(");
        buf.append("namswpaceURI=").append(namespaceURI);
        buf.append(", localName=").append(localName);
        buf.append(", qName=").append(qName);
        buf.append(", atts[").append(atts.getLength());
        buf.append("]{");
        for (int i = 0, n = atts.getLength(); i < n; i++) {
          buf.append("\n(uri=").append(atts.getURI(i));
          buf.append(" localName=").append(atts.getLocalName(i));
          buf.append(" qName=").append(atts.getQName(i));
          buf.append(" type=").append(atts.getType(i));
          buf.append(" value=").append(atts.getValue(i));
          buf.append("), ");
        }
        buf.append("\n}");
        logger.detail(buf.toString());
      }

      if (localName.equals("node")) {
        startNode(atts);
      }

      if (!thisNode) {
        return;
      }

      if (localName.equals("agent")) {
        startAgent(atts);
      } else if (localName.equals("component")) {
        startComponent(atts);
      } else if (localName.equals("argument")) {
        startArgument(atts);
      } else {
        // ignore
      }
    }

    // misc characters within an element, e.g. argument data
    public void characters(char[] ch, int start, int length)
      throws SAXException {

      if (logger.isDetailEnabled()) {
        StringBuffer buf = new StringBuffer();
        buf.append("characters(ch[").append(ch.length).append("]{");
        buf.append(ch, start, length);
        buf.append("}, ").append(start).append(", ");
        buf.append(length).append(")");
        logger.detail(buf.toString());
      }

      if (!thisNode) {
        return;
      }

      if (inArgument) {
        // inside component argument, so save characters
        argValueBuffer.write(ch, start, length);
      }
    }

    // end element
    public void endElement(String namespaceURI, String localName, String qName)
      throws SAXException {

      if (logger.isDetailEnabled()) {
        logger.detail(
            "endElement("+
            "namswpaceURI="+namespaceURI+
            ", localName="+localName+
            ", qName="+qName+")");
      }

      if (!thisNode) {
        return;
      }

      if (localName.equals("argument")) {
        endArgument();
      } else if (localName.equals("component")) {
        endComponent();
      } else if (localName.equals("agent")) {
        endAgent();
      } else if (localName.equals("node")) {
        endNode();
      } else {
        // ignore
      }
    }

    // xml parser error
    public void error(SAXParseException exception) throws SAXException {
      logger.error("Error parsing the file", exception);
      super.error(exception);
    }

    // xml parser warning
    public void warning(SAXParseException exception) throws SAXException {
      logger.warn("Warning parsing the file", exception);
      super.warning(exception);
    }

    // our element handlers:

    private void startNode(Attributes atts)
      throws SAXException {

      String name = Strings.intern(atts.getValue("name"));

      if (!nodename.equals(name)) {
        if (logger.isDebugEnabled()) {
          logger.debug("skipping node "+name);
        }
        currentAgent = null;
        thisNode = false;
        return;
      }

      thisNode = true;
      currentAgent = name;

      if (logger.isInfoEnabled()) {
        logger.info("starting node "+currentAgent);
      }

      // make a new place for the node's components
      currentList = (List) agents.get(currentAgent);
      if (currentList == null) {
        currentList = new ArrayList(1);
        agents.put(currentAgent, currentList);
      }
    }

    private void endNode()
      throws SAXException {
      if (thisNode) {
        thisNode = false;
        currentAgent = null;
        currentList = null;
        if (logger.isInfoEnabled()) {
          logger.info("finished node "+nodename);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("skipped node");
        }
      }
    }

    private void startAgent(Attributes atts)
      throws SAXException {
      String name = Strings.intern(atts.getValue("name"));
      if (name == null) {
        throw new RuntimeException(
            "Agent lacks name attribute");
      }

      currentAgent = name;
      if (logger.isDebugEnabled()) {
        logger.debug("starting agent "+currentAgent);
      }

      // make a new place for the agent's components
      currentList = (List) agents.get(currentAgent);
      if (currentList == null) {
        currentList = new ArrayList(1);
        agents.put(currentAgent, currentList);
      }
    }

    private void endAgent()
      throws SAXException {
      // restore name to node's name
      if (logger.isDebugEnabled()) {
        logger.debug("finished agent "+currentAgent);
      }
      currentAgent = nodename;
      currentList = (List) agents.get(currentAgent);
    }

    private void startComponent(Attributes atts)
      throws SAXException {

      if (currentList == null) {
        throw new RuntimeException(
            "component not within node or agent?");
      }
      if (!currentComponent.isEmpty()) {
        throw new RuntimeException(
            "startComponent already within component? "+
            currentComponent);
      }

      currentComponent.put("name", Strings.intern(atts.getValue("name")));
      currentComponent.put("class", Strings.intern(atts.getValue("class")));
      currentComponent.put("priority", Strings.intern(atts.getValue("priority")));
      currentComponent.put("insertionpoint", Strings.intern(atts.getValue("insertionpoint")));
    }

    private void endComponent()
      throws SAXException {

      if (currentComponent.isEmpty()) {
        throw new RuntimeException(
            "endComponent not within component?");
      }
      ComponentDescription desc = 
        makeComponentDesc(currentComponent);
      currentComponent.clear();

      currentList.add(desc);
    }

    private void startArgument(Attributes atts)
      throws SAXException {
      if (currentComponent.isEmpty()) {
        throw new RuntimeException(
            "Argument not in component!");
      }
      if (inArgument) {
        throw new RuntimeException(
            "Already have an argument value buffer? "+argValueBuffer);
      }
      inArgument = true;
    }

    private void endArgument()
      throws SAXException {
      if (!inArgument) {
        throw new RuntimeException("Not in argument?");
      }
      inArgument = false;

      String argument = argValueBuffer.toString().trim();
      argValueBuffer.reset();

      ArrayList argumentList = (ArrayList)
        currentComponent.get("arguments");
      if (argumentList == null) {
        argumentList = new ArrayList(1);
        currentComponent.put("arguments", argumentList);
      }
      argumentList.add(argument);
    }

    // utility methods:

    private static String insertionPointContainer(String insertionPoint) {
      int i = 
        (insertionPoint == null ? 
         -1 :
         insertionPoint.lastIndexOf('.'));
      if (i < 0) {
        throw new RuntimeException(
            "insertionpoint \""+insertionPoint+"\" lacks '.'");
      }
      return insertionPoint.substring(0, i);
    }

    private static ComponentDescription makeComponentDesc(
        Map componentProps) {
      String name = (String) componentProps.get("name");
      String classname = (String) componentProps.get("class");
      String priority = (String) componentProps.get("priority");
      String insertionPoint = (String) componentProps.get("insertionpoint");
      //      String order = (String) componentProps.get("order");
      ArrayList args = (ArrayList) componentProps.get("arguments");
      List vParams = null;
      if ((args != null) && (args.size() > 0)) {
        vParams = Collections.unmodifiableList(args);
      }
      return
        makeComponentDesc(
            name, vParams, classname, priority, insertionPoint);
    }

    private static ComponentDescription makeComponentDesc(
        String name,
        List vParams,
        String classname,
        String priority,
        String insertionPoint) {
      return new ComponentDescription(
          name,
          insertionPoint,
          classname,
          null, //codebase
          vParams, //params
          null, //certificate
          null, //lease
          null, //policy
          ComponentDescription.parsePriority(priority));
        }
  }
}
