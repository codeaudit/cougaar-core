/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.naming;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

import javax.naming.directory.*;
import org.cougaar.core.society.NameServer;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** Actual RMI remote object providing the implementation of NameServer.
 * Now implements a hierarchy of directory objects
 **/

// Need to add check that names within a given directory do not have embedded DirSeparators

public class NSImpl extends UnicastRemoteObject implements NS {
  public static final NameServer.Directory ROOT = new NSDirectory(DirSeparator);

  // implement with a simple flat hashmap for now... set lookups will be slow
  private HashMap mapOfMaps = new HashMap(11);
  
  public NSImpl() throws RemoteException {
    super();
    putDirMap(ROOT, new NSDirMap());
  }

  public NameServer.Directory getRoot() {
    return ROOT;
  }

  public void clear(String path) {
    String dirName = parseDirectory(path);
    clear(new NSDirectory(dirName));
  }

  public void clear(NameServer.Directory directory) {
    Map dirMap = getDirectory(directory, false);
    
    if (dirMap != null) {
      synchronized (dirMap) {
        dirMap.clear();
      }
    }
  }

  public boolean containsKey(String key) {
    return containsKey(new NSDirectory(parseDirectory(key)), getTail(key));
  }

  public boolean containsKey(NameServer.Directory directory, String key) {
    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        return (dirMap.get(key) != null);
      }
    } 

    return false;
  }

  public NameServer.Directory createSubDirectory(NameServer.Directory directory, 
                                                 String subDirName) {
    return createSubDirectory(directory, subDirName, null);
  }

  public NameServer.Directory createSubDirectory(NameServer.Directory directory, 
                                                 String subDirName,
                                                 Collection attributes) {
    Map dirMap = getDirectory(directory, false);
    NameServer.Directory subDirectory = null;

    if (dirMap != null) {
      synchronized (dirMap) {
        NSObjectAndAttributes o = 
          (NSObjectAndAttributes) dirMap.get(subDirName);
        if (o == null) {
          subDirectory = new NSDirectory(fullName(directory, subDirName));
          o = new NSObjectAndAttributes(subDirectory, null);
          dirMap.put(subDirName, o);
          
          putDirMap(subDirectory, new NSDirMap(attributes));
        }
      }
    }

    return subDirectory;
  }

  public void destroySubDirectory(NameServer.Directory directory) { 
    Map dirMap = getDirectory(directory, false);

    if ((dirMap != null) && (dirMap.size() == 0)) {
      synchronized (mapOfMaps) {
        mapOfMaps.remove(directory);
      }


      // strip trailing DirSeparators
      String dirName = directory.getPath();
      while (dirName.endsWith(DirSeparator)) {
        dirName = dirName.substring(0, dirName.length() - 1);
      }

      Map parentMap = getDirectory(new NSDirectory(parseDirectory(dirName)), 
                                   false);
      if (dirMap != null) {
        synchronized (parentMap) {
          parentMap.remove(getTail(dirName));
        }
      }
    }
  }

  public Collection entrySet(String directory) {
    return entrySet(new NSDirectory(directory));
  }

  public Collection entrySet(NameServer.Directory directory) {
    ArrayList l = new ArrayList();
    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        for (Iterator i = dirMap.entrySet().iterator(); i.hasNext(); ) {
          Map.Entry ent = (Map.Entry) i.next();
          Object key = ent.getKey();
          Object value = ent.getValue();
          
          if (value instanceof NSObjectAndAttributes) {
            value = ((NSObjectAndAttributes) value).getObject();
          }
          l.add(new NSEntry(key, value));
        }
      }
    }
    return l;
  }

  public String fullName(NameServer.Directory directory, String name) {
    return directory.getPath() + name;
  }

  /** Look up an object in the NameService directory **/
  public Object get(String path) {
    return get(new NSDirectory(parseDirectory(path)), getTail(path));
  }

  public Object get(NameServer.Directory directory, String name) {
    Map dirMap = getDirectory(directory, false);

    if ((name == null) || (name.equals(""))) {
      return directory;
    }

    Object found = null;

    if (dirMap != null) {
      synchronized (dirMap) {
        found = dirMap.get(name);
        if (found != null) {
          found = ((NSObjectAndAttributes) found).getObject();
        }
      }
    }

    return found;
  }

  public Collection getAttributes(String path) {
    return getAttributes(new NSDirectory(parseDirectory(path)), getTail(path));
  }
    
  public Collection getAttributes(NameServer.Directory directory, String name) {
    if ((name == null) || (name.equals(""))) {
      return getDirAttributes(directory);
    }

    Map dirMap = getDirectory(directory, false);
    Collection attr = null;

    if (dirMap != null) {
      synchronized (dirMap) {
        NSObjectAndAttributes found = (NSObjectAndAttributes) dirMap.get(name);
        if (found != null) {
          attr = found.getAttributes();
        }
      }
    }

    return attr;
  }



  public boolean isEmpty(String directory) {
    return isEmpty(new NSDirectory(parseDirectory(directory)));
  }

  public boolean isEmpty(NameServer.Directory directory) {
    Map dirMap = getDirectory(directory, false);
   
    if (dirMap != null) {
      synchronized (dirMap) {
        return (dirMap.size() == 0);
      }
    } else {
      return true;
    }
  }

  public Collection keySet(String directory) {
    return keySet(new NSDirectory(parseDirectory(directory)));
  }

  public Collection keySet(NameServer.Directory directory) {
    ArrayList l = new ArrayList();
    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        for (Iterator i = dirMap.keySet().iterator(); i.hasNext(); ) {
          String key = (String) i.next();
          l.add(key);
        }
      }
    }
    return l;
  }

  /** add an object to the directory **/
  public Object put(String path, Object o) {
    return put(path, o, null);
  }

  public Object put(String path, Object o, Collection attributes) {
    return put(new NSDirectory(parseDirectory(path)), getTail(path), o, attributes);
  }

  /** add an object to the directory **/
  public Object put(NameServer.Directory directory, String name, Object o) {
    return put(directory, name, o, null);
  }

  public Object put(NameServer.Directory directory, String name, Object o, 
                    Collection attributes) {
    if ((name == null) || (name.equals(""))) {
      return null;
    }

    if (o instanceof NameServer.Directory) {
      return null;
    }

    Map dirMap = getDirectory(directory, true);

    if (dirMap != null) {
      synchronized (dirMap) {
        NSObjectAndAttributes found = 
          (NSObjectAndAttributes) dirMap.put(name, 
                                             new NSObjectAndAttributes(o, attributes));

        if (found != null) {
          return found.getObject();
        } else {
          return null;
        }
      }
    } else {
      return null;
    }
  }

  public void putAttributes(String path, Collection attributes) {
    putAttributes(new NSDirectory(parseDirectory(path)), getTail(path), attributes);
  }

  public void putAttributes(NameServer.Directory directory, String name, 
                            Collection attributes)  {
    if ((name == null) || name.equals("")) {
      putDirAttributes(directory, attributes);
      return;
    }

    Map dirMap = getDirectory(directory, true);

    if (dirMap != null) {
      synchronized (dirMap) {
        NSObjectAndAttributes found = (NSObjectAndAttributes) dirMap.get(name);
        if (found != null) {
          found.setAttributes(attributes);
        } else {
          
          // ??? Is it okay to have a null object?
          dirMap.put(name, new NSObjectAndAttributes(null, attributes));
        }
      }
    }
  }

  /** remove an object (and name) from the directory **/
  public Object remove(String path) {
    return remove(new NSDirectory(parseDirectory(path)), getTail(path));
  }

  /** remove an object (and name) from the directory **/
  public Object remove(NameServer.Directory directory, String name) {
    if ((name == null) || (name.equals(""))) {
      return null;
    }

    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        Object found = dirMap.get(name);
        if ((found == null) || 
            (found instanceof NameServer.Directory)) {
          // Use destroySubDirectory to remove directories
          return null;
        } else {
          NSObjectAndAttributes remove = 
            (NSObjectAndAttributes) dirMap.remove(name);
          if (remove != null) {
            return remove.getObject();
          } else {
            return null;
          }
        }
      }
    } else {
      return null;
    }
  }

  /** rename an object in the directory **/
  public Object rename(NameServer.Directory directory, String oldName,
                       String newName) {
    if ((oldName == null) || (oldName.equals("")) ||
        (newName == null) || (newName.equals(""))) {
      return null;
    }

    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        Object found = dirMap.get(oldName);
        if ((found == null) || 
            (found instanceof NameServer.Directory)) {
          // Use destroySubDirectory to remove directories
          return null;
        } else {
          NSObjectAndAttributes nsObj = 
            (NSObjectAndAttributes) dirMap.remove(oldName);
          dirMap.put(newName, nsObj);
          if (nsObj != null) {
            return nsObj.getObject();
          } else {
            return null;
          }
        }
      }
    } else {
      return null;
    }
  }

  public int size(String directory) {
    return size(new NSDirectory(parseDirectory(directory)));
  }

  public int size(NameServer.Directory directory) {
    Map dirMap = getDirectory(directory, false);

    if (dirMap != null) {
      synchronized (dirMap) {
        return dirMap.size();
      }
    } else {
      return -1;
    }
  }

  public Collection values(String directory) {
    return values(new NSDirectory(parseDirectory(directory)));
  }

  public Collection values(NameServer.Directory directory) {
    Map dirMap = getDirectory(directory, false);
    ArrayList l = new ArrayList();

    if (dirMap != null) {
      synchronized (dirMap) {
        for (Iterator i = dirMap.entrySet().iterator(); i.hasNext(); ) {
          Map.Entry ent = (Map.Entry) i.next();
          Object value = ent.getValue();
          
          if (value instanceof NSObjectAndAttributes) {
            l.add(((NSObjectAndAttributes) value).getObject());
          } else {
            l.add(value);
          }
        }
      }
    }
    return l;
  }

  private String getTail(String path) {
    int i = path.lastIndexOf(DirSeparator);
    if (i != -1) {
      i++;
      int l = path.length();
      if (i == l) {
        return "";
      } else {
        return path.substring(i);
      }
    } else {
      // Couldn't find an dir spec. Retaining previous behaviour - nkg 07/27/2001
      return null;
    }
  }

  /** return the directory specified in key.
   * as a shortcut, if key specifies a file, then the containing directory 
   * is returned.
   * All entries should start with DirSeparator
   **/
  private String parseDirectory(String path) {
    int end = path.lastIndexOf(DirSeparator);
    if (end != -1) {
      return path.substring(0, end);
    } else {
      return ROOT.getPath();
    }
  }

  private NSDirMap getDirectory(NameServer.Directory directory, boolean  create) {
    synchronized (mapOfMaps) {
      NSDirMap currentMap = getDirMap(directory);
      
      if ((currentMap == null) && (create)) {
        // Build multiple levels to get there? 
        StringTokenizer tokenizer = new StringTokenizer(directory.getPath(), DirSeparator);
        NameServer.Directory currentDir = ROOT;
        currentMap = getDirMap(ROOT);
        while (tokenizer.hasMoreTokens()) {
          String dirName = tokenizer.nextToken();
          Object o = currentMap.get(dirName);
          if (o == null) {
            NameServer.Directory subDir = new NSDirectory(fullName(currentDir, dirName));
            currentMap.put(dirName, subDir);
            
            currentMap = new NSDirMap();
            currentDir = subDir;
            putDirMap(currentDir, currentMap);
          } else if (o instanceof NameServer.Directory) {
            currentDir = (NameServer.Directory) o;
            currentMap = getDirMap(currentDir);
          } else {
            // entry exists but isn't a directory - bail now
            System.out.println("bailing on " + currentDir + " object " + o + " " + o.getClass());
            break;
          }
        }
      }
      return currentMap;
    }
  }
        
  private NSDirMap getDirMap(NameServer.Directory directory) {
    return (NSDirMap) mapOfMaps.get(directory);
  }

  private Collection getDirAttributes(NameServer.Directory directory) {
    NSDirMap dirMap = getDirMap(directory);

    if (dirMap != null) {
      return dirMap.getAttributes();
    } else { 
      return null;
    }
  }

  private void putDirAttributes(NameServer.Directory directory, Collection attributes) {
    NSDirMap dirMap = getDirMap(directory);

    if (dirMap != null) {
      dirMap.setAttributes(attributes);
    }
  }

  private void putDirMap(NameServer.Directory directory, NSDirMap dirMap) {
    mapOfMaps.put(directory, dirMap);
  }

  private static class NSEntry implements Map.Entry, Serializable {
    private Object _key;
    private Object _value;

    public NSEntry(Object key, Object value) {
      _key=key;
      _value=value;
    }

    public Object getKey() { return _key; }
    public Object getValue() {return _value; }
    public Object setValue(Object value) { _value=value; return null;}
  }


  private static class NSDirectory implements NameServer.Directory, Serializable {
    private String _path;
    public NSDirectory(String path) {
      _path = path.trim();
      if (!path.endsWith(DirSeparator)) { 
        _path = path + DirSeparator;
      }
    }

    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else {
        return ((obj instanceof NameServer.Directory) &&
                (((NameServer.Directory) obj).getPath().equals(getPath())));
      }
    }

    public int hashCode() {
      return getPath().hashCode();
    }

    public String getPath() { return _path; }
    public String toString() { return _path; }
  }

  private class NSDirMap extends HashMap {
    Collection _attributes;

    public NSDirMap() {
      this(null);
    }

    public NSDirMap(Collection attributes) {
      super(11);

      if (attributes == null) {
        _attributes = new ArrayList();
      } else {
        _attributes = attributes;
      }
    }

    public void setAttributes(Collection attributes) {
      _attributes = attributes;
    }

    public Collection getAttributes() {
      return _attributes;
    }
  }

  private class NSObjectAndAttributes {
    private Object _object;
    private Collection _attributes;

    public NSObjectAndAttributes(Object object, Collection attributes) {
      _object = object;

      if (attributes != null) {
        _attributes=attributes;
      } else {
        _attributes = new ArrayList();
      }
    }

    public Collection getAttributes() {
      if (getObject() instanceof NSDirectory) {
        return getDirAttributes((NSDirectory) getObject());
      } else {
        return _attributes;
      }
    }
    
    public void setAttributes(Collection newAttributes) {
      if (getObject() instanceof NSDirectory) {
        putDirAttributes((NSDirectory) getObject(), newAttributes);
      } else {
        _attributes = newAttributes;
      }
    }

    public Object getObject() {
      return _object;
    }
  }

  public static void main(String arg[]) {
    try {
      NSImpl foo = new NSImpl();

      Collection attrs = new ArrayList();
      attrs.add(new BasicAttribute("fact", "the letter A"));
      foo.put(NS.DirSeparator + "foo", "Foo", new ArrayList(attrs));

      attrs.clear();
      attrs.add(new BasicAttribute("fact", "the letter B"));
      foo.put(NS.DirSeparator + "bar", "Bar", new ArrayList(attrs));

      attrs.add(new BasicAttribute("fact", " the letter C"));
      foo.put(NS.DirSeparator + "clusters" + NS.DirSeparator + "a", "aString", new ArrayList(attrs));
      
      attrs.add(new BasicAttribute("fact", " the letter D"));
      foo.put(NS.DirSeparator + "clusters" + NS.DirSeparator + "b", "bString", new ArrayList(attrs));

      System.out.println("foo = "+foo.get(NS.DirSeparator + "foo")+" " + foo.getAttributes(NS.DirSeparator + "foo"));
      System.out.println("bar = "+foo.get(NS.DirSeparator + "bar")+ " " + foo.getAttributes(NS.DirSeparator + "bar"));
      System.out.println("a = "+foo.get(NS.DirSeparator + "clusters" + NS.DirSeparator + "a") + " " + 
                         foo.getAttributes(NS.DirSeparator + "clusters" + NS.DirSeparator + "a"));
      System.out.println("b = "+foo.get(NS.DirSeparator + "clusters" + NS.DirSeparator + "b") + " " + 
                         foo.getAttributes(NS.DirSeparator + "clusters" + NS.DirSeparator + "b"));

      System.out.println(": keys =");
      for (Iterator a = foo.keySet(ROOT).iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(": values =");
      for (Iterator a = foo.values(ROOT).iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(": entries =");
      for (Iterator a = foo.entrySet(ROOT).iterator(); a.hasNext();){
        Map.Entry entry = (Map.Entry) a.next();
        System.out.println("\t"+entry.getKey()+ " " + entry.getValue());
      }

      System.out.println(NS.DirSeparator + "clusters" + NS.DirSeparator + " keys =");
      for (Iterator a = 
             foo.keySet(NS.DirSeparator + "clusters" + NS.DirSeparator).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(NS.DirSeparator + "clusters" + NS.DirSeparator + " values =");
      for (Iterator a = 
             foo.values(NS.DirSeparator + "clusters" + NS.DirSeparator).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(NS.DirSeparator + "clusters" + NS.DirSeparator + " entries =");
      for (Iterator a =
             foo.entrySet(NS.DirSeparator + "clusters" + NS.DirSeparator).iterator(); 
           a.hasNext();){
        Map.Entry entry = (Map.Entry) a.next();
        System.out.println("\t"+entry.getKey()+ " " + entry.getValue());
      }

      System.out.println("createSubDirectory: " + NS.DirSeparator + 
                         "clusters" + NS.DirSeparator + "subDir");
      NameServer.Directory clustersDir = 
        (NameServer.Directory) foo.get(NS.DirSeparator + "clusters" + 
                                       NS.DirSeparator);
      NameServer.Directory subDir = 
        foo.createSubDirectory(clustersDir, "subDir");
      for (Iterator a = 
             foo.keySet(NS.DirSeparator + "clusters" + NS.DirSeparator).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println("destroySubDirectory: " + subDir);
      foo.destroySubDirectory(subDir);
      for (Iterator a = 
             foo.keySet(NS.DirSeparator + "clusters" + NS.DirSeparator).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.exit(1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
