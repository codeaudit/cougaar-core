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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

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
 * <p>
 * Figures out right classpath, creates a new classloader and
 * then invokes the usual static main method on the specified class
 * (using the new classloader).
 * <p>
 * Main job is to search for jar files, building up a collection
 * of paths to give to a special NodeClassLoader so that we don't
 * have to maintain many different script files.
 * <p>
 * <pre>
 * The following locations are examined, in order:
 *  -Dorg.cougaar.class.path=...	(like a classpath)
 *  $COUGAAR_INSTALL_PATH/lib/*.{jar,zip,plugin}
 *  $COUGAAR_INSTALL_PATH/plugins/*.{jar,zip,plugin}
 *  -Dorg.cougaar.system.path=whatever/*.{jar,zip,plugin}
 *  $COUGAAR_INSTALL_PATH/sys/*.{jar,zip,plugin}
 * </pre>
 * <p>
 * As an added bonus, Bootstrapper may be run as an application
 * which takes the fully-qualified class name of the class to run
 * as the first argument.  All other arguments are passed
 * along as a String array as the single argument to the class.
 * The class must provide a public static launch(String[]) or
 * main(String[]) method (searched for in that order).
 *
 * The Boostrapper's classloader will not load any classes which
 * start with "java.". This
 * list may be extended by supplying a -Dorg.cougaar.core.society.bootstrapper.exclusions=foo.:bar.
 * System property.  The value of the property should be a list of
 * package prefixes separated by colon (":") characters.
 * <p>
 * A common problem is the attempt to use "patch" jar files to repair a few
 * classes of some much larger archive.  There are two problems with this
 * use pattern: (1) the order that Bootstrapper will find jar files in a
 * directory is undefined - there is no guarantee that the patch will take
 * precedence over the original.  Also, (2) classloaders will refuse to
 * load classes of a given package from multiple jar files - if the patch jar
 * does not contain the whole package, the classloader will likely be
 * unable to load the rest of the classes.  Both problems tend to
 * crop up when you can least afford this confusion.
 * <p>
 * The System property <em>org.cougaar.core.node.bootstrapper.loud</em>
 * controls debugging output of the bootstrapping classloader.  When set to
 * "true" will output the list of jar/zip files used to load classes (in order).
 * When set to "shout" will additionally print the location of the jar/zip file
 * used to load each and every class.
 **/
public class Bootstrapper
{
  private static int loudness = 0;
  static {
    String s = System.getProperty("org.cougaar.core.node.bootstrapper.loud");
    if ("true".equals(s)) {
      loudness = 1;
    } else if ("shout".equals(s)) {
      loudness = 2;
    } else if ("false".equals(s)) {
      loudness = 0;
    }
  }

  private static boolean isBootstrapped = false;

  public static void main(String[] args) {
    String[] launchArgs = new String[args.length - 1];
    System.arraycopy(args, 1, launchArgs, 0, launchArgs.length);
    launch(args[0], launchArgs);
  }

  /**
   * Reads the properties from specified url
   **/
   public static void readProperties(String propertiesURL){
       if (propertiesURL != null) {
	Properties props = System.getProperties();
	try {    // open url, load into props
	  URL url = new URL(propertiesURL);
	  InputStream stream = url.openStream();
	  props.load(stream);
	  stream.close();
	} catch (MalformedURLException me) {
	  System.err.println(me);
	} catch (IOException ioe) {
	  System.err.println(ioe);
	}
      }
       System.out.println("done");
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

    readProperties(System.getProperty("org.cougaar.properties.url"));

    ArrayList l = new ArrayList();
    String base = System.getProperty("org.cougaar.install.path");

    accumulateClasspath(l, System.getProperty("org.cougaar.class.path"));
    // no longer accumulate classpath
    //accumulateClasspath(l, System.getProperty("java.class.path"));
    // we'll defer to system's classpath if we don't find it anywhere
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
      Thread.currentThread().setContextClassLoader(cl);

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
          n = canonical(n); // Convert n to a canonical path, if possible
        }
        l.add(newURL(n));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  static String canonical(String filename) {
    String ret = filename;
    if (!filename.startsWith("file:")) {
      File f = new File (filename);
      try {
        ret = f.getCanonicalPath() + File.separator;
      } catch (IOException ioe) {
        // file must not exist...
      }
    }
//    System.out.println(filename+" CHANGED  to "+ret);
    return ret;
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

  static class BootstrapClassLoader extends XURLClassLoader {
    private static final List exclusions = new ArrayList();
    static {
      exclusions.add("java.");  // avoids javaiopatch.jar
      // let base do it instead
      //exclusions.add("javax.");
      //exclusions.add("com.sun.");
      //exclusions.add("sun.");
      //exclusions.add("net.jini.");
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
      if (loudness>0) {
        synchronized(System.err) {
          System.err.println();
          System.err.println("Bootstrapper URLs: ");
          for (int i=0; i<urls.length; i++) {
            System.err.println("\t"+urls[i]);
          }
          System.err.println();
        }
      }
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
        if (loudness>1 && c != null) {
          java.security.ProtectionDomain pd = c.getProtectionDomain();
          if (pd != null) {
            java.security.CodeSource cs = pd.getCodeSource();
            if (cs != null) {
              System.err.println("BCL: "+c+" loaded from "+cs.getLocation());
            }
          }
        }
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
  }

}




