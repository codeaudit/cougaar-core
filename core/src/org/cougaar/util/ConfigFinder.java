/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.w3c.dom.Document;

/**
 * ConfigFinder provides utilitites to search for a named file in
 * several specified locations, returning the first location where a
 * file by that name is found.
 *
 * Files are found and opened by the open() method. open() tries to
 * find the file using each of the elements of org.cougaar.config.path. The
 * elements of org.cougaar.config.path are separated by semicolons and
 * interpreted as URLs. The URLs in org.cougaar.config.path are interpreted
 * relative to the directory specified by org.cougaar.install.path. Several
 * special tokens may appear in these URLs:
 *
 * $INSTALL signifies file:<org.cougaar.install.path>
 * $CONFIG signifies <org.cougaar.config>
 * $CWD signifies <user.dir>
 * $HOME signifies <user.home>
 *
 * The default value for org.cougaar.config.path is defined in the static
 * variable defaultConfigPath:
 *   $CWD;$HOME/.alp;$INSTALL/configs/$CONFIG;$INSTALL/configs/common
 *
 * If a value is specified for org.cougaar.config.path that ends with a
 * semicolon, the above default is appended to the specified
 * value. The URLs in org.cougaar.config.path are interpreted relative to
 * $INSTALL. URLs may be absolute in which case some or all of the
 * base URL may be ignored.
 **/
public final class ConfigFinder {
  /** this is the default string used if org.cougaar.config.path is not defined.
   * it is also appended to the end if org.cougaar.config.path ends with a ';'.
   **/
  public static final String defaultConfigPath = 
    "$CWD;$HOME/.alp;$INSTALL/configs/$CONFIG;$INSTALL/configs/common";

  private List configPath = new ArrayList();
  private Map properties = null;

  private boolean verbose = false;
  public void setVerbose(boolean b) { verbose = b; }

  public ConfigFinder() {
    this(defaultConfigPath, defaultProperties);
  }

  public ConfigFinder(String s) {
    this(s, defaultProperties);
  }

  public ConfigFinder(String s, Map props) {
    properties = props;
    if (s == null) {
      s = defaultConfigPath;
    } else {
      s = s.replace('\\', '/'); // Make sure its a URL and not a file path
    }

    // append the default if we end with a ';'
    if (s.endsWith(";")) s += defaultConfigPath;

    Vector v = StringUtility.parseCSV(s, ';');
    int l = v.size();
    for (int i = 0; i < l; i++) {
      appendPathElement((String) v.elementAt(i));
    }
  }

  private void appendPathElement(URL url) {
    configPath.add(url);
  }

  /** return the index of the first non-alphanumeric character 
   * at or after i.
   **/
  private int indexOfNonAlpha(String s, int i) {
    int l = s.length();
    for (int j = i; j<l; j++) {
      char c = s.charAt(j);
      if (!Character.isLetterOrDigit(c)) return j;
    }
    return -1;
  }

  private String substituteProperties(String s) {
    int i = s.indexOf('$');
    if (i >= 0) {
      int j = indexOfNonAlpha(s,i+1);
      String s0 = s.substring(0,i);
      String s2 = (j<0)?"":s.substring(j);
      String k = s.substring(i+1,(j<0)?s.length():j);
      Object o = properties.get(k);
      if (o == null) {
        throw new IllegalArgumentException("No such path property \""+k+"\"");
      }
      return substituteProperties(s0+o.toString()+s2);
    }
    return s;
  }

  private void appendPathElement(String el) {
    String s = el;
    try {
      s = substituteProperties(el);
      s = s.replace('\\', '/').replace('\\', '/'); // These should be URL-like
      try {
        if (!s.endsWith("/")) s += "/";
        appendPathElement(new URL(s));
      }
      catch (MalformedURLException mue) {
        File f = new File(s);
        if (f.isDirectory()) {
          appendPathElement(new File(s).getCanonicalFile().toURL());
        } // else skip it.
      }
    } 
    catch (Exception e) {
      System.err.println("Failed to interpret " + el + " as url: " + e);
    }
  }

  /**
   * Locate an actual file in the config path. This will skip over
   * elements of org.cougaar.config.path that are not file: urls.
   **/
  public File locateFile(String aFilename) {
    for (int i = 0 ; i < configPath.size() ; i++) {
      URL url = (URL) configPath.get(i);
      if (url.getProtocol().equals("file")) {
        try {
          URL fileURL = new URL(url, aFilename);
          File result = new File(fileURL.getFile());
          if (verbose) { System.err.print("Looking for "+result+": "); }
          if (result.exists()) {
            if (verbose) { System.err.println("Found it. File " + aFilename + 
                               " is " + fileURL); }
            return result;
          } else {
            if (verbose) { System.err.println(); }
          }
        }
        catch (MalformedURLException mue) {
          continue;
        }
      }
    }
    return null;
  }

  /**
   * Opens an InputStream to access the named file. The file is sought
   * in all the places specified in configPath.
   * @throws IOException if the resource cannot be found.
   **/
  public InputStream open(String aURL) throws IOException {
    for (int i = 0 ; i < configPath.size() ; i++) {
      URL base = (URL) configPath.get(i);
      try {
        URL url = new URL(base, aURL);
        if (verbose) { System.err.print("Trying "+url+": "); }
        InputStream is = url.openStream();
        if (is == null) continue; // Don't return null
        if (verbose) { System.err.println("Found it. File " + aURL + " is " + url); }
        return is;
      }
      catch (MalformedURLException mue) {
        if (verbose) { System.err.println(); }
        continue;
      }
      catch (IOException ioe) {
        if (verbose) { System.err.println(); }
        continue;
      }
    }

    StringTokenizer st = new StringTokenizer (aURL, "-.");
    String sb = st.nextToken() + ".zip";
    try {
      File file = locateFile(sb);
      if (file != null) return openZip(aURL, file.toString());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (NullPointerException npe) {
      System.out.println("Can't locate File " + aURL + " or " + sb);
      npe.printStackTrace();
    }

    throw new FileNotFoundException(aURL);
  }

  public InputStream openZip (String aURL, String aZIP) 
    throws IOException
  {
    ZipFile zip = null;
    InputStream retval = null;
    try {
      zip = new ZipFile(aZIP);
      Enumeration zipfiles = zip.entries();
      while (zipfiles.hasMoreElements()){
	ZipEntry file = (ZipEntry)zipfiles.nextElement();
	try {
	  if (file.getName().equals(aURL)) {
	    retval = zip.getInputStream(file);
	  } else if (file.getName().endsWith (".cfg")) {
	    InputStream is = zip.getInputStream(file);
	    BufferedReader in = new BufferedReader(new InputStreamReader(is));
	    while (in.ready()) {
	      String text = in.readLine();
	      appendPathElement(text.substring(0, text.lastIndexOf(File.separator)));
	    }
	  }
	} catch (IOException ioe) {	
	  continue;
	}
      }
      return retval;
    } catch (ZipException ioe) {
    }
    throw new FileNotFoundException(aZIP);
  }


  public Document parseXMLConfigFile(String xmlfile) throws IOException {
    DOMParser parser = new DOMParser();
    parser.setEntityResolver(new ConfigResolver());

    InputStream istream = null;
    InputSource is = null;
    try {
      istream = open(xmlfile);
      if (istream == null) {
         throw new RuntimeException("Got null InputStream opening file " + xmlfile);
      }
      is = new InputSource(istream);
      if (is == null) {
         throw new RuntimeException("Got null InputSource from input stream for file " + xmlfile);
      }
      parser.parse(is);
    } catch (SAXException e) {
      System.out.println("Error parsing file: " + xmlfile);
      e.printStackTrace();
    }    

    return parser.getDocument();
  }

  // Singleton pattern
  private static ConfigFinder defaultConfigFinder;
  private static Map defaultProperties;
  static {
    Map m = new HashMap();
    defaultProperties = m;

    File ipf = new File(System.getProperty("org.cougaar.install.path", "."));
    try { ipf = ipf.getCanonicalFile(); } catch (IOException ioe) {}
    String ipath = ipf.toString();
    m.put("INSTALL", ipath);

    m.put("HOME", System.getProperty("user.home"));
    m.put("CWD", System.getProperty("user.dir"));

    File csf = new File(ipath, "configs");
    try { csf = csf.getCanonicalFile(); } catch (IOException ioe) {}
    String cspath = csf.toString();
    m.put("CONFIGS", cspath);

    String cs = System.getProperty("org.cougaar.config", "common");
    if (cs != null)
      m.put("CONFIG", cs);

    defaultConfigFinder = new ConfigFinder(System.getProperty("org.cougaar.config.path"));
  }

  public static ConfigFinder getInstance() {
    return defaultConfigFinder;
  }

  class ConfigResolver implements EntityResolver {
    public InputSource resolveEntity (String publicId, String systemId) {

      
      URL url = null;

      try {
	url = new URL(systemId);
      } catch(Exception e) {}

      String filename = url.getFile();

      // Convert any '\'s to '/'s.
      filename = filename.replace('\\', '/');

      filename = filename.substring(filename.lastIndexOf("/") + 1, filename.length());

      InputSource is = null;
      try {
	InputStream istream = open(filename);
        if (istream == null) {
	  throw new RuntimeException("Got null input stream opening file " + filename);
        }
        is = new InputSource(istream);
      } catch(IOException e) {
        System.err.println("Error getting input source for file " + filename);
        e.printStackTrace();
       }
      
      if(is == null) {
        throw new RuntimeException("Null InputSource for file " + filename);
      }

      return is;
   } 
  }

  /**
   * Point test for ConfigFinder.  prints the first line of the
   * URL passed as each argument.
   **/
  public static void main(String argv[]) {
    ConfigFinder ff = getInstance();
    ff.setVerbose(true);
    for (int i = 0; i <argv.length; i++) {
      String url = argv[i];
      try {
        InputStream is = ff.open(url);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String s = br.readLine();
        System.out.println("url = "+url+" read: "+s);
      } catch (IOException ioe) {
        System.out.println("url = "+url+" exception: "+ioe);
      }
    }
  }
      
}
