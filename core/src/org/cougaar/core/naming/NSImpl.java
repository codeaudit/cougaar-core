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

package org.cougaar.core.naming;

import org.cougaar.core.service.*;

import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javax.naming.*;
import javax.naming.directory.BasicAttribute;

import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** Actual RMI remote object providing the implementation of NameServer.
 * Now implements a hierarchy of directory objects
 **/

// Need to add check that names within a given directory do not have embedded DirSeparators

public class NSImpl extends UnicastRemoteObject implements NS {
  private static final String ROOT_NAME = "";
  public static final NSKey ROOT = new NSDirKey(ROOT_NAME);

  // implement with a simple flat hashmap for now... set lookups will be slow
  private HashMap mapOfMaps = new HashMap(11);

  public NSImpl() throws RemoteException {
    super(0, NamingSocketFactory.getInstance(), NamingSocketFactory.getInstance());
    putDirMap((NSDirKey) ROOT, new NSDirMap(ROOT_NAME));
  }

  public NSKey getRoot() {
    return ROOT;
  }

  public void clear(NSKey dirKey) 
    throws NameNotFoundException {
    Map dirMap = getDirectory(dirKey);
    
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
        dirMap.clear();
    }
  }

  public synchronized NSKey createSubDirectory(NSKey dirKey, 
                                               String subDirName)
    throws NamingException, NameAlreadyBoundException {
    return createSubDirectory(dirKey, subDirName, null);
  }

  public synchronized NSKey createSubDirectory(NSKey dirKey, 
                                               String subDirName,
                                               Collection attributes) 
    throws NamingException, NameAlreadyBoundException, NameNotFoundException, InvalidNameException {
    if ((subDirName == null) || (subDirName.equals(""))) {
      throw new InvalidNameException("Can not specify empty Context name");
    }

    synchronized (mapOfMaps) {
      // Don't want any other thread modifying mapOfMaps while we're creating
      // the directory.
      NSDirMap dirMap = getDirectory(dirKey);
      if (dirMap == null) {
        throw new NameNotFoundException("No context found for NSKey - " + 
                                        dirKey);
      }
      
      synchronized (dirMap) {
        NSObjectAndAttributes nsObj = 
          (NSObjectAndAttributes) dirMap.get(subDirName);

        if (nsObj != null) {
          throw new NameAlreadyBoundException(fullName(dirKey, subDirName)  +
                                              " already exists.");
        }

        String subDirPath = fullName(dirKey, subDirName);
        NSDirMap subDirMap = new NSDirMap(subDirPath, attributes);
        putDirMap(subDirMap.getKey(), subDirMap);
        
        nsObj = new NSObjectAndAttributes(subDirMap.getKey(), null);
        dirMap.put(subDirName, nsObj);
        return subDirMap.getKey();
      }
    }
  }

  synchronized public void destroySubDirectory(NSKey dirKey, String subDirName) 
    throws ContextNotEmptyException, NameNotFoundException, NamingException {
    if ((subDirName == null) || (subDirName.equals(""))) {
      throw new InvalidNameException("Can not specify empty SubContext name");
    }

    synchronized (mapOfMaps) {
      // Don't leave any chance of someone getting a hold of the SubContext
      // while we're in the process of deleting.
      NSDirMap dirMap = getDirectory(dirKey);
      if (dirMap == null) {
        throw new NameNotFoundException("No context found for NSKey - " + 
                                        dirKey);
      }
      
      
      synchronized (dirMap) {
        NSObjectAndAttributes nsObj = 
          (NSObjectAndAttributes) dirMap.get(subDirName);
        
        if (nsObj == null) {
          // No error if sub dir doesn't exist
          return;
        }
        
        if (!(nsObj.getObject() instanceof NSKey)) {
          throw new NotContextException(fullName(dirKey, subDirName) + 
                                         " is not a Context.");
        }
        
        NSKey subDirKey = (NSKey) nsObj.getObject();
        NSDirMap subDirMap = getDirectory(subDirKey);
        
        if (subDirMap != null) {
          synchronized (subDirMap) {
            if (subDirMap.size() != 0) {
              throw new ContextNotEmptyException(subDirMap.getFullPath() + 
                                                 " is not empty.");
            }
            mapOfMaps.remove(subDirKey);
          }
        }
        
        dirMap.remove(subDirName);
      } 
    }
  }
  
  public Collection entrySet(NSKey dirKey) 
    throws NameNotFoundException, NamingException{
    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    ArrayList l = new ArrayList();
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

    return l;
  }

  public String fullName(NSKey dirKey, String name) 
    throws NameNotFoundException, NamingException {
    NSDirMap dirMap = getDirectory(dirKey);
    
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    if ((name == null) || (name.equals(""))) {
      return  dirMap.getFullPath();
    } else {
      return dirMap.getFullPath() + NS.DirSeparator + name;
    }
  }

  /** Look up an object in the NameService directory **/
  public Object get(NSKey dirKey, String name)
    throws NameNotFoundException, NamingException {
    Map dirMap = getDirectory(dirKey);
    Object found = null;

    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    if ((name == null) || (name.equals(""))) {
      return dirKey;
    }
    
    synchronized (dirMap) {
      found = dirMap.get(name);
      if (found != null) {
        found = ((NSObjectAndAttributes) found).getObject();
        return found;
      } else {
        throw new NameNotFoundException(fullName(dirKey, name) + 
                                        " not found.");
      }
    }
  }

  public Collection getAttributes(NSKey dirKey, String name) 
    throws NamingException, NameNotFoundException {

    if ((name == null) || (name.equals(""))) {
      return getDirAttributes(dirKey);
    }

    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      NSObjectAndAttributes found = (NSObjectAndAttributes) dirMap.get(name);
      if (found != null) {
        return found.getAttributes();
      } else {
        throw new NameNotFoundException(fullName(dirKey, name) + 
                                        " not found.");
      }
    }
  }

  public boolean isEmpty(NSKey dirKey)
    throws NameNotFoundException, NamingException {
    Map dirMap = getDirectory(dirKey);

    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }
   
    synchronized (dirMap) {
      return (dirMap.size() == 0);
    }
  }

  public Collection keySet(NSKey dirKey) 
    throws NameNotFoundException, NamingException {
    ArrayList l = new ArrayList();
    Map dirMap = getDirectory(dirKey);

    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      for (Iterator i = dirMap.keySet().iterator(); i.hasNext(); ) {
        String key = (String) i.next();
        l.add(key);
      }
    }

    return l;
  }

  /** add an object to the directory **/
  public Object put(NSKey dirKey, String name, Object o, boolean overwrite)
    throws NameAlreadyBoundException, NameNotFoundException, 
    InvalidNameException, OperationNotSupportedException, NamingException{
    return put(dirKey, name, o, null, overwrite);
  }

  public Object put(NSKey dirKey, String name, Object o, 
                    Collection attributes, boolean overwrite) 
    throws NameAlreadyBoundException, NameNotFoundException, 
    InvalidNameException, OperationNotSupportedException, NamingException{
    if ((name == null) || (name.equals(""))) {
      throw new InvalidNameException("Can't bind to empty name.");
    }

    if (o == null) {
      throw new OperationNotSupportedException("Can't bind to null Object.");
    }
    
    if (o instanceof NSKey) {
      throw new OperationNotSupportedException("Can't insert NSKey directly.");
    }

    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      NSObjectAndAttributes nsObj = (NSObjectAndAttributes) dirMap.get(name);

      if (nsObj != null) {
        if (!overwrite) {
          throw new NameAlreadyBoundException(fullName(dirKey, name) + 
                                            " already exists.");
        } 
        if (nsObj.getObject() instanceof NSKey) {
          throw new OperationNotSupportedException("Can't overWrite an NSKey - " +
                                                   fullName(dirKey, name) + 
                                                   ".");
        }
      }

      NSObjectAndAttributes found = 
        (NSObjectAndAttributes) dirMap.put(name, 
                                           new NSObjectAndAttributes(o, attributes));
      
      if (found != null) {
        return found.getObject();
      } else {
        return null;
      }
    }
  }

  public void putAttributes(NSKey dirKey, String name, 
                            Collection attributes)  
    throws NameNotFoundException, NamingException {
    if ((name == null) || name.equals("")) {
      putDirAttributes(dirKey, attributes);
      return;
    }

    Map dirMap = getDirectory(dirKey);

    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    
    synchronized (dirMap) {
      NSObjectAndAttributes found = (NSObjectAndAttributes) dirMap.get(name);
      if (found != null) {
        found.setAttributes(attributes);
      } else {
          throw new NameNotFoundException(fullName(dirKey, name) + 
                                          " does not exist");
      }
    }
  }

  /** remove an object (and name) from the directory **/
  public Object remove(NSKey dirKey, String name) 
    throws NamingException, NameNotFoundException, InvalidNameException, OperationNotSupportedException {
    if ((name == null) || (name.equals(""))) {
      throw new InvalidNameException("Unable to remove object bound to empty name.");
    }

    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      NSObjectAndAttributes nsObj = (NSObjectAndAttributes) dirMap.get(name);
      if (nsObj == null) {
        return null;
      } else {
        if (nsObj.getObject() instanceof NSKey) {
          // Use destroySubDirectory to remove directories
          throw new OperationNotSupportedException("Can't unbind an NSKey - " +
                                                   fullName(dirKey, name));
        }

        dirMap.remove(name);
        
        return ((NSObjectAndAttributes) nsObj).getObject();
      }
    }
  }

  /** rename an object in the directory **/
  public Object rename(NSKey dirKey, String oldName,
                       String newName) 
    throws InvalidNameException, NameNotFoundException, 
      NameAlreadyBoundException, OperationNotSupportedException, NamingException {
    if ((oldName == null) || (oldName.equals("")) ||
        (newName == null) || (newName.equals(""))) {
      throw new InvalidNameException("Can't use empty name.");
    }


    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      NSObjectAndAttributes nsObj = 
        (NSObjectAndAttributes) dirMap.get(newName);
      if (nsObj != null) {
        throw new NameAlreadyBoundException(fullName(dirKey, newName) + 
                                            " already exists.");
      }

      nsObj = (NSObjectAndAttributes) dirMap.get(oldName);
      if (nsObj == null)  {
        throw new NameNotFoundException(fullName(dirKey, oldName) + 
                                        " not found.");
      }

      if (nsObj.getObject() instanceof NSKey) {
        throw new OperationNotSupportedException("Can't rebind an NSKey - " +
                                                 fullName(dirKey, oldName) + 
                                                 ".");
      }


      dirMap.remove(oldName);
      dirMap.put(newName, nsObj);

      // Checked that nsObj != null above
      return nsObj.getObject();
    }
  }

  public int size(NSKey dirKey) 
    throws NamingException, NameNotFoundException {
    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    synchronized (dirMap) {
      return dirMap.size();
    }
  }

  public Collection values(NSKey dirKey)
    throws NamingException, NameNotFoundException {
    Map dirMap = getDirectory(dirKey);
    if (dirMap == null) {
      throw new NameNotFoundException("No context found for NSKey - " + 
                                      dirKey);
    }

    ArrayList l = new ArrayList();

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
      return getDirectory(ROOT).getFullPath();
    }
  }

  private NSDirMap getDirectory(NSKey dirKey) {
    synchronized (mapOfMaps) {
      NSDirMap currentMap = getDirMap(dirKey);
      return currentMap;
    }
  }
        
  private NSDirMap getDirMap(NSKey dirKey) {
    synchronized (mapOfMaps) {
      return (NSDirMap) mapOfMaps.get(dirKey);
    }
  }

  private Collection getDirAttributes(NSKey dirKey) {
    NSDirMap dirMap = getDirMap(dirKey);

    if (dirMap != null) {
      synchronized (dirMap) {
        return dirMap.getAttributes();
      }
    } else { 
      return null;
    }
  }

  private void putDirAttributes(NSKey dirKey, Collection attributes) {
    NSDirMap dirMap = getDirMap(dirKey);

    if (dirMap != null) {
      synchronized (dirMap) {
        dirMap.setAttributes(attributes);
      }
    }
  }

  private void putDirMap(NSDirKey dirKey, NSDirMap dirMap) {
    synchronized (mapOfMaps) {
      mapOfMaps.put(dirKey, dirMap);
    }
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


  private static class NSDirKey implements NSKey, Serializable {
    int _hashCode;

    public NSDirKey(String path) {
      _hashCode = path.hashCode();
    }

    public int hashCode() {
      return _hashCode;
    }

    public boolean equals(Object o) {
      if (!(o instanceof NSDirKey)) {
        return false;
      } else {
        return _hashCode == ((NSDirKey) o).hashCode();
      }
    }
  }

  private class NSDirMap extends HashMap {
    private Collection _attributes;
    private String _fullPath;

    public NSDirMap(String fullPath) {
      this(fullPath, null);
    }

    public NSDirMap(String fullPath, Collection attributes) {
      super(11);

      _fullPath = fullPath;

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

    public String getFullPath() {
      return _fullPath;
    }

    public NSDirKey getKey() {
      return new NSDirKey(_fullPath);
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
      if (getObject() instanceof NSDirKey) {
        return getDirAttributes((NSDirKey) getObject());
      } else {
        return _attributes;
      }
    }
    
    public void setAttributes(Collection newAttributes) {
      if (getObject() instanceof NSDirKey) {
        putDirAttributes((NSDirKey) getObject(), newAttributes);
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
      foo.put(ROOT, "foo", "Foo", new ArrayList(attrs), false);

      attrs.clear();
      attrs.add(new BasicAttribute("fact", "the letter B"));
      foo.put(ROOT, "bar", "Bar", new ArrayList(attrs), false);

      NSKey clustersKey = foo.createSubDirectory(ROOT, "clusters");
      attrs.add(new BasicAttribute("fact", " the letter C"));
      foo.put(clustersKey, "a", "aString", new ArrayList(attrs), false);
      
      attrs.add(new BasicAttribute("fact", " the letter D"));
      foo.put(clustersKey, "b", "bString", new ArrayList(attrs), false);

      System.out.println("foo = "+ foo.get(ROOT, "foo")+ " " + foo.getAttributes(ROOT, "foo"));
      System.out.println("bar = "+foo.get(ROOT, "bar")+ " " + foo.getAttributes(ROOT, "bar"));
      System.out.println("a = "+foo.get(clustersKey, "a") + " " + 
                         foo.getAttributes(clustersKey,"a"));
      System.out.println("b = "+foo.get(clustersKey, "b") + " " + 
                         foo.getAttributes(clustersKey, "b"));

      System.out.println(foo.fullName(ROOT, "") + " : keys =");
      for (Iterator a = foo.keySet(ROOT).iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(foo.fullName(ROOT, "") + " : values =");
      for (Iterator a = foo.values(ROOT).iterator(); a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(foo.fullName(ROOT, "") + " : entries =");
      for (Iterator a = foo.entrySet(ROOT).iterator(); a.hasNext();){
        Map.Entry entry = (Map.Entry) a.next();
        System.out.println("\t"+entry.getKey()+ " " + entry.getValue());
      }

      System.out.println(foo.fullName(clustersKey, "") + " : keys =");
      for (Iterator a = 
             foo.keySet(clustersKey).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(foo.fullName(clustersKey, "") + " : values =");
      for (Iterator a = 
             foo.values(clustersKey).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println(foo.fullName(clustersKey, "") + " : entries =");
      for (Iterator a =
             foo.entrySet(clustersKey).iterator(); 
           a.hasNext();){
        Map.Entry entry = (Map.Entry) a.next();
        System.out.println("\t"+entry.getKey()+ " " + entry.getValue());
      }

      System.out.println("createSubDirectory: " + 
                         foo.fullName(clustersKey, "subDir"));
      NSKey subDir = 
        foo.createSubDirectory(clustersKey, "subDir");
      for (Iterator a = 
             foo.keySet(clustersKey).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println("destroySubDirectory: " + subDir);
      foo.destroySubDirectory(clustersKey, "subDir");
      for (Iterator a = 
             foo.keySet(clustersKey).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.exit(1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
