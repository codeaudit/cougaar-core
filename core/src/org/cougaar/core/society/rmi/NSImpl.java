/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society.rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

import org.cougaar.core.society.NameServer;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** actual RMI remote object providing the implementation of NameServer.
 * Now implements a hierarchy of directory objects
 **/


public class NSImpl extends UnicastRemoteObject implements NS {

  public NSImpl() throws RemoteException {
    super();
  }

  // implement with a simple flat hashmap for now... set lookups will be slow
  private HashMap root = new HashMap(11);

  public void clear(String path) {
    synchronized (root) {
      Map m = findDirectory(path, true);
      if (m != null)
        m.clear();
    }
  }

  public boolean containsKey(String key) {
    synchronized (root) {
      Map m = findDirectory(key, false);
      if (m == null) return false;
      String s = getTail(key);
      Object o = m.get(s);
      return (o != null);
    }
  }

  public Collection entrySet(String directory) {
    ArrayList l = new ArrayList();
    synchronized (root) {
      Map m = findDirectory(directory, false);
      if (m == null) return l;
      for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry ent = (Map.Entry) i.next();
        Object key = ent.getKey();
        Object value = ent.getValue();
        if (value instanceof Map) {
          l.add(new NSEntry(key,new NSDirectory(directory+key)));
        } else {
          l.add(new NSEntry(key, value));
        }
      }
    }
    return l;
  }

  /** Look up an object in the NameService directory **/
  public Object get(String path) {
    synchronized (root) {
      Map m = findDirectory(path, false);
      if (m == null) return null; // directory not found
      String s = getTail(path);
      Object o = m.get(s);
      if (o instanceof Map) {
        return new NSDirectory(path);
      } else {
        return o;
      }
    }
  }

  public boolean isEmpty(String directory) {
    synchronized (root) {
      Map m = findDirectory(directory, true);
      return ( (m == null) || (m.size() == 0) );
    }
  }

  public Collection keySet(String directory) {
    ArrayList l = new ArrayList();
    synchronized (root) {
      Map m = findDirectory(directory, false);
      if (m == null) return l;
      for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
        String key = (String) i.next();
        l.add(key);
      }
    }
    return l;
  }

  /** add an object to the directory **/
  public Object put(String path, Object o) {
    synchronized (root) {
      Map m = findDirectory(path, true);
      if (m == null) return null;
      String s = getTail(path);
      return m.put(s, o);
    }
  }

  /** remove an object (and name) from the directory **/
  public Object remove(String path) {
    synchronized (root) {
      Map m = findDirectory(path, false);
      if (m == null) return null;
      String s = getTail(path);
      return m.remove(s);
    }
  }

  public int size(String directory) {
    synchronized (root) {
      Map m = findDirectory(directory, false);
      if (m == null) return -1;
      return m.size();
    }
  }

  public Collection values(String directory) {
    ArrayList l = new ArrayList();
    synchronized (root) {
      Map m = findDirectory(directory, false);
      if (m == null) return l;
      for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry ent = (Map.Entry) i.next();
        Object value = ent.getValue();
        if (value instanceof Map) {
          l.add(new NSDirectory(directory+ent.getKey()));
        } else {
          l.add(value);
        }
      }
    }
    return l;
  }

  private String getTail(String path) {
    int i = path.lastIndexOf("/");
    if (i != -1) {
      i++;
      int l = path.length();
      if (i == l) {
        return null;
      } else {
        return path.substring(i);
      }
    } else {
      return null;
    }
  }

  /** return the directory specified in key.
   * as a shortcut, if key specifies a file, then the containing directory 
   * is returned.
   * All entries should begin with "/"
   **/
  private Map findDirectory(String path, boolean createp) {
    int p =0;
    int i;
    Map base = null;
    while ((i=path.indexOf("/", p)) != -1) {
      if (i==p) {
        base = root;
      } else {
        if (base == null) return null; // don't support relative addrs
        String key = path.substring(p, i);
        Object v = base.get(key);
        if (v == null && createp) {
          Map m = new HashMap(11);
          base.put(key, m);
          v = m;
        }
        if (v instanceof Map) {
          base = (Map) v;
        } else {
          return null;
        } 
      }
      p = i+1;
    }
    return base;
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
      _path = path+"/";
    }
    public String getPath() { return _path; }
    public String toString() { return _path; }
  }

  public static void main(String arg[]) {
    try {
      NSImpl foo = new NSImpl();

      foo.put("/foo", "Foo");
      foo.put("/bar", "Bar");
      foo.put("/clusters/a", "a");
      foo.put("/clusters/b", "b");

      System.out.println("foo = "+foo.get("/foo"));
      System.out.println("bar = "+foo.get("/bar"));
      System.out.println("a = "+foo.get("/clusters/a"));
      System.out.println("b = "+foo.get("/clusters/b"));

      System.out.println("/=");
      for (Iterator a = foo.values("/").iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }
      System.out.println("/clusters/=");
      for (Iterator a = foo.values("/clusters/").iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }
      System.exit(1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
