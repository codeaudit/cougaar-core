/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.applet.Applet;
import java.applet.AppletContext;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.cougaar.bootstrap.Bootstrapper;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.node.Node;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.AppletService;

/**
 * This component loads a Cougaar {@link Node} as a browser {@link Applet}.
 * <p>
 * The server requires an HTML file to load this applet, e.g.:<pre>
 *   &lt;html&gt;&lt;body&gt;&lt;applet
 *     code=org.cougaar.core.node.NodeApplet.class
 *     archive="lib/bootstrap.jar,lib/util.jar,lib/core.jar,lib/<i>your_code</i>.jar,sys/log4j.jar"
 *     width=400 height=400&gt;
 *       &lt;param name="properties" value="
 *         -Dorg.cougaar.node.name=<i>MyNode</i>
 *         -Dorg.cougaar.society.file=<i>MyNode.xml</i>
 *         "/&gt;
 *       Unable to load Applet.
 *   &lt;/applet&gt;&lt;/body&gt;&lt;/html&gt;
 * </pre>
 * In addition to the above jars, the server directory will also require your
 * agent XML file (e.g. "MyNode.xml") and the agent template configuration
 * files:<pre>
 *   configs/common/*.xsl
 * </pre>
 */
public class NodeApplet extends Applet {

  private static final String[] REQUIRED_PROPS = new String[] {
    "properties",
    "-Dorg.cougaar.society.file",
    "-Dorg.cougaar.node.name",
    "-Dorg.xml.sax.driver",
  };

  private static final String[] DEFAULT_PROPS = new String[] {
    // config path without $CWD
    "-Dorg.cougaar.config.path=$CIP;$CIP/configs/common",
    // read the node from XML
    "-Dorg.cougaar.core.node.InitializationComponent=XML",
    // avoid javaiopatch check in persistence
    "-Dorg.cougaar.core.persistence.verifyJavaIOPatch=false",
    // local-only wp
    "-Dorg.cougaar.society.xsl.param.wpserver=singlenode",
    // local-only mts
    "-Dorg.cougaar.society.xsl.param.mts=singlenode",
    // avoid mtsstd jar
    "-Dorg.cougaar.society.xsl.param.socketFactory=false",
    // trim the config:
    "-Dorg.cougaar.society.xsl.param.servlets=false",
    "-Dorg.cougaar.society.xsl.param.planning=false",
    "-Dorg.cougaar.society.xsl.param.communities=false",
    "-Dorg.cougaar.core.load.planning=false",
    // no "LDMDomains.ini"
    "-Dorg.cougaar.core.domain.config.enable=false",
    // TODO sysProps: org.cougaar.core.qos.rss.RSSMetricsUpdateServiceImpl
    "-Dorg.cougaar.society.xsl.param.metrics=trivial",
  };

  // defaults for -Dorg.xml.sax.driver
  private static final String[] XML_DRIVERS = new String[] {
    "org.apache.crimson.parser.XMLReaderImpl", // jdk1.4
    "com.sun.org.apache.xerces.internal.parsers.SAXParser", // jdk1.5
  };

  // applet run state
  private static final int LOADING = 0;
  private static final int RUNNING = 1;
  private static final int FAILED  = 2;

  private final Object state_lock = new Object();
  private int state = LOADING;

  public void init() {
    setLayout(new BorderLayout());

    // override properties table
    Properties props;
    try {
      props = createProperties();
    } catch (Exception e) {
      // probably missing a required property
      updateState(FAILED, e);
      return;
    }
    SystemProperties.overrideProperties(props);

    Runnable r = new Runnable() {
      public void run() {
        // create the root service broker and binding site
        final ServiceBroker rootsb = new ServiceBrokerSupport() { };
        BindingSite rootbs = 
          new BindingSite() {
            public ServiceBroker getServiceBroker() { return rootsb; }
            public void requestStop() { }
          };

        // add our AppletService
        AppletService appletService = createAppletService();
        rootsb.addService(
            AppletService.class,
            new SimpleServiceProvider(AppletService.class, appletService));

        // launch the node
        Throwable t = null;
        try {
          Node myNode = new Node();
          // this will call our "load()" method
          BindingUtility.activate(myNode, rootbs, rootsb);
          // done with our job... quietly finish.
        } catch (Throwable e) {
          t = new RuntimeException("Unable to start node", e);
        }

        updateState((t == null ? RUNNING : FAILED), t);
      }
    };

    (new Thread(r, "Cougaar main")).start();
  }

  private Properties createProperties() {
    Properties ret = new Properties();

    // applets are always allowed to see these properties:
    ret.put("file.separator", System.getProperty("file.separator"));
    ret.put("java.class.version", System.getProperty("java.class.version"));
    ret.put("java.vendor", System.getProperty("java.vendor"));
    ret.put("java.vendor.url", System.getProperty("java.vendor.url"));
    ret.put("java.version", System.getProperty("java.version"));
    ret.put("line.separator", System.getProperty("line.separator"));
    ret.put("os.arch", System.getProperty("os.arch"));
    ret.put("os.name", System.getProperty("os.name"));
    ret.put("path.separator", System.getProperty("path.separator"));

    // figure out our cip, which is our path minus the basename
    // e.g.  http://x:y/z/file -->  http://x:y/z
    URL url = getCodeBase();
    String cip = "";
    if (url.getProtocol() != null) cip += url.getProtocol()+"://";
    if (url.getHost() != null) cip += url.getHost();
    if (url.getPort() >= 0) cip += ":"+url.getPort();
    if (url.getPath() != null) {
      String s = url.getPath();
      int i = s.lastIndexOf("/");
      if (i > 0) s = s.substring(0, i);
      cip += s;
    }

    // required cougaar install path as a URL:
    ret.put("org.cougaar.install.path", cip);
    ret.put("user.home", cip);
    ret.put("user.dir", cip);

    // set default properties
    putAll(ret, DEFAULT_PROPS);

    // copy in optional applet params
    String more_props = getParameter("properties");
    if (more_props != null) {
      ret.put("properties", "true");
      String[] sa = more_props.split(" ");
      putAll(ret, sa);
    }

    // we must explicitly set the xml factory driver, otherwise the XMLFactory
    // will attempt to call "System.getProperty(..)" and throw an exception
    if (!ret.containsKey("org.xml.sax.driver")) {
      for (int i = 0; i < XML_DRIVERS.length; i++) {
        String s = XML_DRIVERS[i];
        try {
          Class c = Class.forName(s);
        } catch (Exception e) {
          continue;
        }
        ret.put("org.xml.sax.driver", s);
        break;
      }
    }

    // check for required properties
    for (int i = 0; i < REQUIRED_PROPS.length; i++) {
      String s = REQUIRED_PROPS[i];
      if (s.startsWith("-D")) s = s.substring(2);
      if (!ret.containsKey(s)) {
        throw new RuntimeException("Missing parameter: "+s);
      }
    }

    // remove artificial "properties" property
    ret.remove("properties");

    return ret;
  }

  private static void putAll(Map m, String[] sa) {
    int n = (sa == null ? 0 : sa.length);
    for (int i = 0; i < n; i++) {
      String s = sa[i].trim();
      int j = s.indexOf('=');
      if (j <= 0 || j >= s.length()-1) continue;
      String name = s.substring(0, j);
      if (name.startsWith("-D")) name = name.substring(2);
      String value = s.substring(j+1);
      m.put(name, value);
    }
  }

  public void destroy() {
    // call NodeControlService.shutdown()?
  }

  public String getAppletInfo() {
    return "Cougaar";
  }

  private AppletService createAppletService() {
    // this is a dumb proxy
    return new AppletService() {
      public boolean isActive() {
        return NodeApplet.this.isActive();
      }
      public URL getDocumentBase() {
        return NodeApplet.this.getDocumentBase();
      }
      public URL getCodeBase() {
        return NodeApplet.this.getCodeBase();
      }
      public String getParameter(String name) {
        return NodeApplet.this.getParameter(name);
      }
      public void showStatus(String msg) {
        NodeApplet.this.showStatus(msg);
      }
      public void showDocument(URL url, String target) {
        AppletContext ac = NodeApplet.this.getAppletContext();
        if (target == null) {
          ac.showDocument(url);
        } else {
          ac.showDocument(url, target);
        }
      }

      public Dimension getSize() {
        return NodeApplet.this.getSize();
      }
      public void setLayout(LayoutManager mgr) {
        NodeApplet.this.setLayout(mgr);
      }
      public Component add(Component comp) {
        return NodeApplet.this.add(comp);
      }
      public Component add(String name, Component comp) {
        return NodeApplet.this.add(name, comp);
      }

      public void override_action(ActionHandler action) {
        NodeApplet.this.override_action(action);
      }
      public void override_paint(PaintHandler paint) {
        NodeApplet.this.override_paint(paint);
      }
    };
  }

  private final AppletService.ActionHandler superActionHandler =
    new AppletService.ActionHandler() {
      public boolean action(AppletService.ActionHandler x, Event evt, Object what) {
        return NodeApplet.super.action(evt, what);
      }
    };
  private AppletService.ActionHandler actionHandler = superActionHandler;

  private void updateState(int i, Throwable t) {
    if (t != null) {
      t.printStackTrace();
    }
    synchronized (state_lock) {
      state = i;
      state_lock.notifyAll();
      repaint();
    }
  }

  private final AppletService.PaintHandler superPaintHandler =
    new AppletService.PaintHandler() {
      public void paint(AppletService.PaintHandler x, Graphics g) {
        NodeApplet.super.paint(g);

        String msg;
        synchronized (state_lock) {
          msg =
            (state == LOADING ? "Loading Cougaar.." :
             state == RUNNING ? "Cougaar is Running" :
             "Failed to start Cougaar (see Java Console)");
        }

        g.setFont(new java.awt.Font("System", java.awt.Font.BOLD, 14));
        Dimension d = getSize();
        int row = (d == null ? 18 : (int) (d.getHeight()/2));
        g.drawString(msg, 10, row);
      }
    };
  private AppletService.PaintHandler paintHandler = superPaintHandler;

  private void override_action(AppletService.ActionHandler handler) {
    this.actionHandler = (handler == null ? superActionHandler : handler);
    repaint();
  }
  private void override_paint(AppletService.PaintHandler handler) {
    this.paintHandler = (handler == null ? superPaintHandler : handler);
  }

  public boolean action(Event evt, Object what) {
    return actionHandler.action(superActionHandler, evt, what);
  }
  public void paint(Graphics g) {
    paintHandler.paint(superPaintHandler, g);
  }

  private static final class SimpleServiceProvider implements ServiceProvider {
    private final Class clazz;
    private final Service service;
    public SimpleServiceProvider(Class clazz, Service service) {
      this.clazz = clazz;
      this.service = service;
    }
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      return (clazz.isAssignableFrom(serviceClass) ? service : null);
    }
    public void releaseService(
        ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
    }
  }
}
