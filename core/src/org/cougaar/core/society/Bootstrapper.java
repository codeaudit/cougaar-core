/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.security.*;
import java.security.cert.*;

/**
 * A bootstrapping launcher, in particular, for a node.
 *
 * Figures out right classpath, creates a new classloader and 
 * then invokes the usual static main method on the specified class
 * (using the new classloader).
 *
 * Main job is to search for jar files, building up a collection
 * of paths to give to a special NodeClassLoader so that we don't
 * have to maintain many different script files.
 *
 * The following locations are examined, in order:
 *  -Dalp.class.path=...	(like a classpath)
 *  $CLASSPATH
 *  $ALP_INSTALL_PATH/lib/*.{jar,zip,plugin}
 *  $ALP_INSTALL_PATH/plugins/*.{jar,zip,plugin}
 *  -Dalp.system.path=whatever/*.{jar,zip,plugin}
 *  $ALP_INSTALL_PATH/sys/*.{jar,zip,plugin} 
 *
 * As an added bonus, Bootstrapper may be run as an application
 * which takes the fully-qualified class name of the class to run
 * as the first argument.  All other arguments are passed
 * along as a String array as the single argument to the class.
 * The class must provide a public static launch(String[]) or
 * main(String[]) method (searched for in that order).
 * 
 * The Boostrapper's classloader will not load any classes which
 * start with "java.", "javax.", "sun.", "com.sun." or "net.jini.".  This 
 * list may be extended by supplying a -Dalp.bootstrapper.exclusions=foo.:bar.
 * System property.  The value of the property should be a list of 
 * package prefixes separated by colon (":") characters.
 **/
public class Bootstrapper
{
  private static boolean isBootstrapped = false;

  public static void main(String[] args) {
  
    String[] launchArgs = new String[args.length - 1];
    System.arraycopy(args, 1, launchArgs, 0, launchArgs.length);
    launch(args[0], launchArgs);
  }

  /**
   * Search the likely spots for jar files and classpaths,
   * create a new classloader, and then invoke the named class
   * using the new classloader.
   * 
   * We will attempt first to invoke classname.launch(String[]) and
   * then classname.main(String []).
   **/
  public static void launch(String classname, String[] args){
    if (isBootstrapped) {
      throw new IllegalArgumentException("Circular Bootstrap!");
    }
    isBootstrapped = true;

    ArrayList l = new ArrayList();
    String base = System.getProperty("org.cougaar.install.path");

    accumulateClasspath(l, System.getProperty("org.cougaar.class.path"));
    accumulateClasspath(l, System.getProperty("java.class.path"));
    accumulateJars(l, new File(base,"lib"));
    accumulateJars(l, new File(base,"plugins"));

    String sysp = System.getProperty("org.cougaar.system.path");
    if (sysp!=null) {
      accumulateJars(l, new File(sysp));
    }

    accumulateJars(l,new File(base,"sys"));
    URL urls[] = (URL[]) l.toArray(new URL[l.size()]);
    
    try {
      BootstrapClassLoader cl = new BootstrapClassLoader(urls);

      Class realnode = cl.loadClass(classname);
      Class argl[] = new Class[1];
      argl[0] = String[].class;
      Method main;
      try {
        // try "launch" first
        main = realnode.getMethod("launch", argl);
      } catch (NoSuchMethodException nsm) {
        // if this one errors, we just let the exception throw up.
        main = realnode.getMethod("main", argl);
      }

      Object[] argv = new Object[1];
      argv[0] = args;
      main.invoke(null,argv);
    } catch (Exception e) {
      System.err.println("Failed to launch "+classname+": ");
      e.printStackTrace();
    }
  }

  static void accumulateJars(List l, File f) {
    File[] files = f.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return isJar(name);
        }
      });
    if (files == null) return;

    for (int i=0; i<files.length; i++) {
      try {
        l.add(newURL("file:"+files[i].getCanonicalPath()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  static void accumulateClasspath(List l, String path) {
    if (path == null) return;
    List files = explodePath(path);
    for (int i=0; i<files.size(); i++) {
      try {
        String n = (String) files.get(i);
        if (!isJar(n) && !n.endsWith("/")) {
          n = n+"/";
        }
        l.add(newURL(n));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  static final boolean isJar(String n) {
    return (n.endsWith(".jar") ||n.endsWith(".zip") ||n.endsWith(".plugin"));
  }

  static URL newURL(String p) throws MalformedURLException {
    try {
      URL u = new URL(p);
      return u;
    } catch (MalformedURLException ex) {
      return new URL("file:"+p);
    }
  }

  static final List explodePath(String s) {
    return explode(s, File.pathSeparatorChar);
  }
  static final List explode(String s, char sep) {
    ArrayList v = new ArrayList();
    int j = 0;                  //  non-white
    int k = 0;                  // char after last white
    int l = s.length();
    int i = 0;
    while (i < l) {
      if (sep==s.charAt(i)) {
        // is white - what do we do?
        if (i == k) {           // skipping contiguous white
          k++;
        } else {                // last char wasn't white - word boundary!
          v.add(s.substring(k,i));
          k=i+1;
        }
      } else {                  // nonwhite
        // let it advance
      }
      i++;
    }
    if (k != i) {               // leftover non-white chars
      v.add(s.substring(k,i));
    }
    return v;
  }

  /** Use slightly different rules for class loading:
   * Prefer classes loaded via this loader rather than 
   * the parent.
   **/

  static class BootstrapClassLoader extends URLClassLoader {
    private static final List exclusions = new ArrayList();
    static {
      exclusions.add("java.");
      exclusions.add("javax.");
      exclusions.add("com.sun.");
      exclusions.add("sun.");
      exclusions.add("net.jini.");
      String s = System.getProperty("org.cougaar.bootstrapper.exclusions");
      if (s != null) {
        List extras = explode(s, ':');
        if (extras != null) {
          exclusions.addAll(extras);
        }
      }
    }

    private boolean excludedP(String classname) {
      int l = exclusions.size();
      for (int i = 0; i<l; i++) {
        String s = (String)exclusions.get(i);
        if (classname.startsWith(s))
          return true;
      }
      return false;
    }

    public BootstrapClassLoader(URL urls[]) {
      super(urls);
    }
    protected synchronized Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
    {
      // First, check if the class has already been loaded
      Class c = findLoadedClass(name);
      if (c == null) {
        // make sure not to use this classloader to load 
        // java.*.  We patch java.io. to support persistence, so it
        // may be in our jar files, yet those classes must absolutely
        // be loaded by the same loader as the rest of core java.
        if (!excludedP(name)) {
          try {
            c = findClass(name);
          } catch (ClassNotFoundException e) {
            // If still not found, then call findClass in order
            // to find the class.
          }
        }
        if (c == null) {
          ClassLoader parent = getParent();
          if (parent == null) parent = getSystemClassLoader();
          c = parent.loadClass(name);
        }
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
  }

}




