/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import java.beans.Beans;
import java.util.*;
import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.plugin.ParameterizedPlugIn;
import java.net.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import org.cougaar.core.society.KeyRing;

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
 **/

class PlugInLoader {

  /** hold the loader **/
  private static PlugInLoader thePlugInLoader = new PlugInLoader();

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

  /** get the PlugInLoader **/
  public static PlugInLoader getInstance() {
    return thePlugInLoader;
  }

  /** may only be constructed by intializer.  To get the instance do
   * PlugInLoader.getInstance();
   **/
  private PlugInLoader() {
    installpath = System.getProperty("org.cougaar.install.path");
    Properties props = System.getProperties();
    checkplugins = (Boolean.valueOf(props.getProperty("org.cougaar.security.plugin.check", "false"))). booleanValue();
    debugging = (Boolean.valueOf(props.getProperty("org.cougaar.security.plugin.debug", "false"))). booleanValue();

    if (checkplugins) {
      quiet = false;
    } else {
      quiet = (Boolean.valueOf(props.getProperty("org.cougaar.security.plugin.quiet", "false"))). booleanValue();
    }

    // find lib/*
    URL[] liburls = searchForJars("lib");
    // find plugin/*
    URL[] pluginurls = searchForJars("plugins");
    precachePlugIns(pluginurls);
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

  /**  Take note of PlugIn URL information for later authentication.
   **/
  private void precachePlugIns(URL[] urls) {
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
   * addPlugInClass().
   **/
  private HashMap pluginClasses = new HashMap(89);

  private void addPlugInClass(Class c) {
    pluginClasses.put(c.getName(), c);
  }

  private Class findPlugInClass(String s) {
    return (Class) pluginClasses.get(s);
  }

  /** create a plugin object named pluginname with appropriate precautions.
   * If the plugin is an instance of ParameterizedPlugIn, the arguments
   * are passed to it.
   **/
  public PlugInServesCluster instantiatePlugIn(String pluginname, Vector arguments) {
    String classname = pluginname;

    try {
      boolean ok = authenticatePlugInClass(classname);
      if (ok || !checkplugins ) {
        PlugInServesCluster plugin =
          (PlugInServesCluster) Beans.instantiate(classloader, classname);
      
        // check that the plugin is trusted enough.
        if (authenticatePlugIn(plugin) == true || !checkplugins) {
          if (plugin instanceof ParameterizedPlugIn) {
            if (arguments != null) {
              // ((ParameterizedPlugIn)plugin).setParameters(arguments);
              System.err.println("Cannot set parameter with PlugInLoader");
            }
          }
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

  protected boolean authenticatePlugInClass(String classname) {
    Class c = findPlugInClass(classname);
    // need to load it?
    if (c == null) {
      try {
        c = classloader.loadClass(classname);
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      addPlugInClass(c);
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
  protected boolean authenticatePlugIn(PlugInServesCluster plugin) {
    // not implemented
    return true;
  }

  private boolean verifyPublicKey(PublicKey pk) {
    // not implemented
    return true;
  }
}
