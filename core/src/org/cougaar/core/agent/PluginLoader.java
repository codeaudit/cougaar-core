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

package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import java.beans.Beans;
import java.util.*;
import org.cougaar.core.plugin.PluginServesCluster;
//import org.cougaar.core.plugin.ParameterizedPlugin;
import java.net.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import org.cougaar.core.node.KeyRing;
import org.cougaar.util.PropertyParser;
/**
 * Facility for loading plugins and associated code.
 * Extends classpath using the following rules (AIP=org.cougaar.install.path):
 *   AIP/lib/*			no security restrictions, but no plugins allowed
 *   AIP/plugins/*		must be signed, plugin restrictions.
 *   AIP/FOO/lib/*		no plugins, plugin restrictions, a plugin defined
 *                              in AIP/plugins/FOO.jar must be on the stack.
 *   CLASSPATH			no restrictions
 * These load-order rules are implemented, but the loaded classes may not
 * have all the specified restrictions imposed for the 1999 demo.
 *
 * Checks for signatures on plugin jar files.
 *
 * There should be exactly one of these per VM.

   * @property org.cougaar.security.plugin.check When set to true (and PluginLoader is enabled), 
   * will perform additional security checks on plugins prior to loading.
   * @property org.cougaar.security.plugin.debug When set to true (and PluginLoader is enabled),
   * will be verbose about plugin class loading progress.
   * @property org.cougaar.security.plugin.quiet When set to true (and PluginLoader is enabled),
   * will be as quiet as possible while classloading plugins.
   * 
 *
 * @deprecated Do not use - ancient code kept for historical value.
 **/

class PluginLoader {

  /** hold the loader **/
  private static PluginLoader thePluginLoader = new PluginLoader();

  private URLClassLoader classloader;

  private String installpath;

  /** iff true, will refuse to create unauthenticated plugins **/
  private boolean checkplugins = false;

  /** iff true, will report on plugin authentication **/
  private boolean debugging = false;

  /** iff true, will be quiet about non-errors **/
  private boolean quiet = false;

  /** @return the URLClassLoader used for loading plugins **/
  public URLClassLoader getClassLoader() { return classloader; }

  /** get the PluginLoader **/
  public static PluginLoader getInstance() {
    return thePluginLoader;
  }

  /** may only be constructed by intializer.  To get the instance do
   * PluginLoader.getInstance();
   **/
  private PluginLoader() {
    installpath = System.getProperty("org.cougaar.install.path");
    checkplugins = PropertyParser.getBoolean("org.cougaar.security.plugin.check", false);;
    debugging = PropertyParser.getBoolean("org.cougaar.security.plugin.debug", false);

    if (checkplugins) {
      quiet = false;
    } else {
      quiet = PropertyParser.getBoolean("org.cougaar.security.plugin.quiet", false);
    }

    // find lib/*
    URL[] liburls = searchForJars("lib");
    // find plugin/*
    URL[] pluginurls = searchForJars("plugins");
    precachePlugins(pluginurls);
    // find FOO/lib/*
    URL[] produrls = searchForProductJars();    
    noteProductURLs(produrls);

    URL[] urls = new URL[liburls.length + pluginurls.length + produrls.length];
    int i=0;
    System.arraycopy(liburls, 0, urls, 0, liburls.length);
    i+= liburls.length;
    System.arraycopy(pluginurls, 0, urls, i, pluginurls.length);
    i+= pluginurls.length;
    System.arraycopy(produrls, 0, urls, i, produrls.length);
    
    classloader = new URLClassLoader(urls);
  }

  /** look for jar files in org.cougaar.install.path/subdir **/
  private URL[] searchForJars(String subdir) {
    File d = new File(installpath+File.separatorChar+subdir);
    if (d.exists() && d.isDirectory()) {
      String[] files = d.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return (name.endsWith(".jar") ||
                  name.endsWith(".zip") ||
                  name.endsWith(".plugin"));
        }
      });
      
      int l = files.length;
      URL[] urls = new URL[l];
      for (int i = 0; i < l; i++) {
        String f = files[i];
        try {
          String pf = new File(d, f).getCanonicalPath();
          if (debugging) System.err.println(subdir+" found "+pf);
          urls[i] = new URL("file:"+pf);
        } catch (Exception e) {e.printStackTrace();}
      }
      return urls;
    }
    return new URL[0];
  }

  /** look for jar files in org.cougaar.install.path/PRODUCT/lib **/
  private URL[] searchForProductJars() {
    ArrayList urlv = new ArrayList();

    File d = new File(installpath);
    if (d.exists() && d.isDirectory()) {
      String[] files = d.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return new File(dir,name).isDirectory();
        }
      });
      
      int l = files.length;
      for (int i = 0; i<l; i++) {
        String f = files[i];
        URL[] subs = searchForJars(f+File.separatorChar+"lib");
        int sl = subs.length;
        for (int j = 0; j<sl; j++) {
          urlv.add(subs[j]);
        }
      }
      
      int ul = urlv.size();
      URL[] urls = new URL[ul];
      for (int i = 0; i < ul; i++) {
        urls[i] = (URL) urlv.get(i);
      }
      return urls;
    } 
    return new URL[0];
  }

  private Set pluginURLs = new HashSet();

  /**  Take note of Plugin URL information for later authentication.
   **/
  private void precachePlugins(URL[] urls) {
    int l = urls.length;
    for (int i=0; i<l; i++) {
      pluginURLs.add(urls[i]);
    }
  }

  private Set productURLs = new HashSet();
  /** note URLs pointing to product-specific libs. **/
  private void noteProductURLs(URL[] urls) {
    int l = urls.length;
    for (int i=0; i<l; i++) {
      productURLs.add(urls[i]);
    }
  }

  /** a map from classname to class instances.
   * Once a plugin class is authenticated, it is placed in here with 
   * addPluginClass().
   **/
  private HashMap pluginClasses = new HashMap(89);

  private void addPluginClass(Class c) {
    pluginClasses.put(c.getName(), c);
  }

  private Class findPluginClass(String s) {
    return (Class) pluginClasses.get(s);
  }

  /** create a plugin object named pluginname with appropriate precautions.
   * If the plugin is an instance of ParameterizedPlugin, the arguments
   * are passed to it.
   **/
  public PluginServesCluster instantiatePlugin(String pluginname, Vector arguments) {
    String classname = pluginname;

    try {
      boolean ok = authenticatePluginClass(classname);
      if (ok || !checkplugins ) {
        PluginServesCluster plugin =
          (PluginServesCluster) Beans.instantiate(classloader, classname);
      
        // check that the plugin is trusted enough.
        if (authenticatePlugin(plugin) == true || !checkplugins) {
          /*
          if (plugin instanceof ParameterizedPlugin) {
            if (arguments != null) {
              // ((ParameterizedPlugin)plugin).setParameters(arguments);
              System.err.println("Cannot set parameter with PluginLoader");
            }
          }
          */
          return plugin;
        }
      }
    } catch (Exception e) {
      System.err.println("Caught "+e+":");
      e.printStackTrace();
    }
    return null;
  }

  private HashSet verifiedClasses = new HashSet();

  protected boolean authenticatePluginClass(String classname) {
    Class c = findPluginClass(classname);
    // need to load it?
    if (c == null) {
      try {
        c = classloader.loadClass(classname);
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      addPluginClass(c);
    }
    
    if (verifiedClasses.contains(c)) 
      return true;

    boolean ok = false;
    // check it
    ProtectionDomain pd = c.getProtectionDomain();
    if (pd != null) {
      if (debugging)
        System.err.println(classname+".ProtectionDomain()="+pd);
      CodeSource cs = pd.getCodeSource();
      if (cs != null) {
        if (debugging)
          System.err.println(classname+".CodeSource()="+cs);
        URL url = cs.getLocation();
        if (!pluginURLs.contains(url)) {
          // broken out because we might want to limit plugins strictly to
          // the plugin directory.
          if (productURLs.contains(url)) {
            if (!quiet) 
              System.err.println(classname+" was not defined in a plugin jar ("+url+")");
          }
        } else {
          // ok so far - check the certs
          java.security.cert.Certificate[] certs = cs.getCertificates();
          boolean trusted = false;
          if (certs != null) {
            int l = certs.length;
            for (int i=0; i<l; i++) {
              java.security.cert.Certificate cert = certs[i];
              PublicKey pk = cert.getPublicKey();
              if (verifyPublicKey(pk)) {
                trusted=true;
                try {
                  cert.verify(pk);
                  ok = true;
                  break;
                } catch (Exception e) {
                  if (!quiet) 
                    System.err.println(classname+" failed verification against "+pk+":"+e);
                }
              }
            } 
          }
          if (!trusted) {
            if (!quiet) 
              System.err.println(classname+" was not signed by a trusted authority.");
          }
        }        
      }
    }

    // cache the verification
    verifiedClasses.add(c);

    return ok;
  }
  protected boolean authenticatePlugin(PluginServesCluster plugin) {
    // not implemented
    return true;
  }

  private boolean verifyPublicKey(PublicKey pk) {
    // not implemented
    return true;
  }
}
