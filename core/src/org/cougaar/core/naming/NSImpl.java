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

import org.cougaar.core.mts.SocketFactory;
import org.cougaar.core.service.*;

import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javax.naming.*;
import javax.naming.event.NamingEvent;
import javax.naming.directory.BasicAttribute;

import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.util.log.*;

/** Actual RMI remote object providing the implementation of NameServer.
 * Now implements a hierarchy of directory objects
 **/

// Need to add check that names within a given directory do not have embedded DirSeparators

public class NSImpl extends UnicastRemoteObject implements NS {
  private static final String ROOT_NAME = "";
  public static final NSKey ROOT = new NSDirKey(ROOT_NAME);

  /**
   * Map a context key from the client directly to the context map.
   * This is a shortcut to recursively descinding through the context
   * tree every time.
   **/
  private HashMap mapOfMaps = new HashMap(11);

  private CallbackQueue callbackQueue = new CallbackQueue();

  private Logger logger;

  public NSImpl() throws RemoteException {
    super(0, 
	  SocketFactory.getNameServiceSocketFactory(), 
	  SocketFactory.getNameServiceSocketFactory());
    logger = LoggerFactory.getInstance().createLogger(getClass());
    putDirectory((NSDirKey) ROOT, new NSDirMap(ROOT_NAME, null));
    callbackQueue.start();
  }

  public NSKey getRoot() {
    return ROOT;
  }

//   public void clear(NSKey dirKey) 
//     throws NameNotFoundException {
//     NSDirMap dirMap = getDirectory(dirKey);
//     dirMap.clear();
//   }

  public synchronized NSKey createSubDirectory(NSKey dirKey, 
                                               String subdirName)
    throws NamingException, NameAlreadyBoundException
  {
    return createSubDirectory(dirKey, subdirName, null);
  }

  public synchronized NSKey createSubDirectory(NSKey dirKey, 
                                               String subdirName,
                                               Collection attributes) 
    throws NamingException, NameAlreadyBoundException, NameNotFoundException, InvalidNameException
  {
    if ((subdirName == null) || (subdirName.equals(""))) {
      throw new InvalidNameException("Can not specify empty Context name");
    }

    // Don't be tempted to synchronize mapOfMaps here. It's not
    // necessary and will deadlock. The "bad" thing that can happen is
    // that the directory of which we are creating a subdirectory
    // might be deleted. This possibility is precluded because the
    // destroySubDirectory method cannot be entered due to
    // synchronization on the server.
    NSDirMap dirMap = getDirectory(dirKey);
    synchronized (dirMap) {
      String subdirPath = fullName(dirKey, subdirName);
      if (dirMap.get(subdirName) != null) {
        throw new NameAlreadyBoundException(subdirPath + " already exists.");
      }
      NSDirMap subdirMap = new NSDirMap(subdirPath, attributes);
      putDirectory(subdirMap.getKey(), subdirMap);
      dirMap.put(subdirName, subdirMap);
      if (dirMap.hasCallbacks()) fireCallbacks(dirMap, NamingEvent.OBJECT_ADDED, dirMap, subdirName, subdirMap.getKey(), null, null);
      return subdirMap.getKey();
    }
  }

  public synchronized void destroySubDirectory(NSKey dirKey, String subdirName) 
    throws ContextNotEmptyException, NameNotFoundException, NamingException
  {
    if ((subdirName == null) || (subdirName.equals(""))) {
      throw new InvalidNameException("Can not specify empty SubContext name");
    }
    // Don't be tempted to synchronize mapOfMaps here. It's not
    // necessary and will deadlock. The "bad" things that can happen
    // are that operations within the subdirectory being deleted will
    // return results that are no longer valid, but that is not
    // distinguishable from previously returned results that will also
    // become invalid once the directory has been removed.
    NSDirMap dirMap = getDirectory(dirKey);
    synchronized (dirMap) {
      NSItem nsItem = dirMap.get(subdirName);
      if (nsItem == null) {
        // No error if sub dir doesn't exist
        return;
      }
        
      if (!(nsItem instanceof NSDirMap)) {
        throw new NotContextException(fullName(dirKey, subdirName) + 
                                      " is not a Context.");
      }
      NSDirMap subdirMap = (NSDirMap) nsItem;
      synchronized (subdirMap) {
        if (subdirMap.size() != 0) {
          throw new ContextNotEmptyException(subdirMap.getFullPath() + 
                                             " is not empty.");
        }
        removeDirectory(subdirMap.getKey());
        dirMap.remove(subdirName);
      }
      if (dirMap.hasCallbacks()) fireCallbacks(dirMap, NamingEvent.OBJECT_REMOVED, dirMap, null, null, subdirName, subdirMap.getKey());
    }
  }
  
  public Collection entrySet(NSKey dirKey) 
    throws NameNotFoundException, NamingException
  {
    return getDirectory(dirKey).getEntries();
  }

  public Collection values(NSKey dirKey)
    throws NamingException, NameNotFoundException
  {
    return getDirectory(dirKey).getValues();
  }

  public String fullName(NSKey dirKey, String name) 
    throws NameNotFoundException, NamingException
  {
    NSDirMap dirMap = getDirectory(dirKey);
    if ((name == null) || (name.equals(""))) {
      return dirMap.getFullPath();
    } else {
      return dirMap.getFullName(name);
    }
  }

  /** Look up a key in the NameService directory **/
  public Object getKey(NSKey dirKey, String name)
    throws NameNotFoundException, NamingException
  {
    NSItem nsItem = getNSItem(dirKey, name);
    if (nsItem instanceof NSDirMap) {
      NSDirMap dirMap = (NSDirMap) nsItem;
      return dirMap.getKey();
    }
    return name;
  }

  /** Look up an object in the NameService directory **/
  public Object get(NSKey dirKey, String name)
    throws NameNotFoundException, NamingException
  {
    return getNSItem(dirKey, name).getObject();
  }

  private NSItem getNSItem(NSKey dirKey, String name)
    throws NameNotFoundException, NamingException
  {
    NSDirMap dirMap = getDirectory(dirKey);
    if ((name == null) || (name.equals(""))) {
      return dirMap;
    }
    NSItem found = dirMap.get(name);
    if (found == null) {
      throw new NameNotFoundException(fullName(dirKey, name) + 
                                      " not found.");
    }
    return found;
  }

  public Collection getAttributes(NSKey dirKey, String name) 
    throws NamingException, NameNotFoundException
  {
    return getNSItem(dirKey, name).getAttributes();
  }

  public boolean isEmpty(NSKey dirKey)
    throws NameNotFoundException, NamingException
  {
    return getDirectory(dirKey).size() == 0;
  }

  public Collection keySet(NSKey dirKey) 
    throws NameNotFoundException, NamingException
  {
    return getDirectory(dirKey).getKeys();
  }

  /** add an object to the directory **/
  public Object put(NSKey dirKey, String name, Object o, boolean overwrite)
    throws NameAlreadyBoundException, NameNotFoundException, 
           InvalidNameException, OperationNotSupportedException, NamingException
  {
    return put(dirKey, name, o, null, overwrite);
  }

  public Object put(NSKey dirKey, String name, Object o, 
                    Collection attributes, boolean overwrite) 
    throws NameAlreadyBoundException, NameNotFoundException, 
           InvalidNameException, OperationNotSupportedException, NamingException
  {
    if ((name == null) || (name.equals(""))) {
      throw new InvalidNameException("Illegal bind of empty name.");
    }

    if (o == null) {
      throw new OperationNotSupportedException("Can't bind to null Object.");
    }
    
    if (o instanceof NSKey) {
      throw new OperationNotSupportedException("Can't insert NSKey directly.");
    }

    NSDirMap dirMap = getDirectory(dirKey);
    synchronized (dirMap) { // Need to test existence and set atomically
      NSItem nsItem = dirMap.get(name);
      if (nsItem != null && !overwrite) {
        throw new NameAlreadyBoundException(fullName(dirKey, name) + 
                                            " already exists.");
      }
      if (nsItem instanceof NSDirMap) {
        throw new OperationNotSupportedException("Can't overWrite a Context entry - " +
                                                 fullName(dirKey, name) + 
                                                 ".");
      }
      NSObjectAndAttributes noa;
      Object oldObject;
      if (nsItem == null) {
        noa = new NSObjectAndAttributes(o, attributes);
        oldObject = null;
        dirMap.put(name, noa);
        if (dirMap.hasCallbacks()) fireCallbacks(dirMap, NamingEvent.OBJECT_ADDED, dirMap, name, o, null, null);
      } else {
        noa = (NSObjectAndAttributes) nsItem;
        noa.setAttributes(attributes);
        oldObject = noa.setObject(o);
        if (noa.hasCallbacks()) fireCallbacks(noa, NamingEvent.OBJECT_CHANGED, dirMap, name, o, name, oldObject);
      }
      return oldObject;
    }
  }

  public void putAttributes(NSKey dirKey, String name, 
                            Collection attributes)  
    throws NameNotFoundException, NamingException
  {
    NSDirMap dirMap = getDirectory(dirKey);
    NSItem nsItem = dirMap.get(name);
    if (nsItem == null) {
      throw new NameNotFoundException(fullName(dirKey, name) + 
                                      " not found.");
    }
    nsItem.setAttributes(attributes);
    Object o = nsItem.getObject();
    if (nsItem.hasCallbacks()) fireCallbacks(nsItem, NamingEvent.OBJECT_CHANGED, dirMap, name, o, name, o);
  }

  /** remove an object (and name) from the directory **/
  public Object remove(NSKey dirKey, String name) 
    throws NamingException, NameNotFoundException, InvalidNameException,
           OperationNotSupportedException
  {
    if ((name == null) || (name.equals(""))) {
      throw new InvalidNameException("Illegal remove empty name.");
    }

    NSDirMap dirMap = getDirectory(dirKey);
    synchronized (dirMap) { // Need to test existence and remove atomically
      NSItem nsItem = dirMap.get(name);
      if (nsItem == null) {
        return null;              // Already removed
      }
      if (nsItem instanceof NSDirMap) {
        // Use destroySubDirectory to remove directories
        throw new OperationNotSupportedException("Can't unbind context entry - " +
                                                 fullName(dirKey, name));
      }
      dirMap.remove(name);
      Object oldObject = nsItem.getObject();
      if (dirMap.hasCallbacks()) fireCallbacks(dirMap, NamingEvent.OBJECT_REMOVED, dirMap, null, null, name, oldObject);
      return oldObject;
    }
  }

  /** rename an object in the directory **/
  public Object rename(NSKey dirKey, String oldName,
                       String newName) 
    throws InvalidNameException, NameNotFoundException, 
      NameAlreadyBoundException, OperationNotSupportedException, NamingException
  {
    if ((oldName == null) || (oldName.equals("")) ||
        (newName == null) || (newName.equals(""))) {
      throw new InvalidNameException("Can't use empty name.");
    }

    NSDirMap dirMap = getDirectory(dirKey);
    synchronized (dirMap) { // Need to test existence and rename atomically
      if (dirMap.get(newName) != null) {
        throw new NameAlreadyBoundException(fullName(dirKey, newName) + 
                                            " already exists.");
      }

      NSItem nsItem = dirMap.get(oldName);
      if (nsItem == null)  {
        throw new NameNotFoundException(fullName(dirKey, oldName) + 
                                        " not found.");
      }

      if (nsItem instanceof NSDirMap) {
        throw new OperationNotSupportedException("Can't rename a context - " +
                                                 fullName(dirKey, oldName) + 
                                                 ".");
      }
      dirMap.remove(oldName);
      dirMap.put(newName, nsItem);
      Object o = nsItem.getObject();
      if (dirMap.hasCallbacks()) fireCallbacks(dirMap, NamingEvent.OBJECT_RENAMED, dirMap, newName, o, oldName, o);

      // Checked that nsItem != null above
      return nsItem.getObject();
    }
  }

  public int size(NSKey dirKey)
    throws NamingException, NameNotFoundException
  {
    return getDirectory(dirKey).size();
  }

  /**
   * Register interest in an object.
   **/
  public void registerInterest(NSKey dirKey, String[] names, NSCallback.Id cbid)
    throws RemoteException, NamingException
  {
    NSItem[] items = new NSItem[names.length];
    for (int i = 0; i < names.length; i++) {
      NSItem nsItem = getNSItem(dirKey, names[i]);
      if (nsItem == null) throw new NameNotFoundException(names[i] + " in " + dirKey);
      items[i] = nsItem;
    }
    for (int i = 0; i < names.length; i++) {
      NSItem nsItem = items[i];
      if (nsItem instanceof NSDirMap) {
        if ((cbid.interestType & NSCallback.NAMESPACE_INTEREST) != 0) {
          nsItem.addCallback(cbid);
        }
      } else {
        if ((cbid.interestType & NSCallback.OBJECT_INTEREST) != 0) {
          nsItem.addCallback(cbid);
        }
      }
    }
  }

  /**
   * Unregister interest in an object.
   **/
  public void unregisterInterest(NSKey dirKey, NSCallback.Id cbid)
    throws NamingException
  {
    getDirectory(dirKey).removeCallbackFromDirMap(cbid);
  }

  /**
   * Fire the callbacks for the target. Each callback identifies the
   * kind of callback it is and the name of the context in which it is
   * registered. All targets must be in the context of the callback or
   * one of its descendants so the names in the bindings are all
   * relative to the callback context.
   * @param target The item having the callbacks attached. For
   * namespace callbacks, this is the NSDirMap that changed. For
   * object callbacks, this is the NSObjectAndAttribute that changed.
   * @param type the interest type of the callback
   * @param dirMap in conjunction with the name params forms the names
   * of the bindings
   * @param newName In conjunction with dirMap, this gives the new
   * name of the item that was added or renamed
   * @param oldName In conjunction with dirMap, this gives the old
   * name of the item that was removed or renamed.
   * @param oldValue gives the old value of the item that was removed
   * or renamed
   * @param newValue gives the new value of the item that was added,
   * changed, or renamed.
   **/
  private void fireCallbacks(NSItem target, int type, NSDirMap dirMap,
                             String newName, Object newValue,
                             String oldName, Object oldValue)
  {
    Binding oldBinding = null;
    Binding newBinding = null;
    // Use non-relative binding since the listener context is unknown here
    if (oldName != null) {
      oldBinding = new Binding(dirMap.getFullName(oldName), oldValue, false);
    }
    if (newName != null) {
      newBinding =  new Binding(dirMap.getFullName(newName), newValue, false);
    }
    for (Iterator i = target.getCallbacks().iterator(); i.hasNext(); ) {
      NSCallback.Id cbid = (NSCallback.Id) i.next();
      NSNamingEvent evt = new NSNamingEvent(cbid, type, newBinding, oldBinding);
      callbackQueue.add(evt);
    }
  }

  /** return the directory specified in key.
   * as a shortcut, if key specifies a file, then the containing directory 
   * is returned.
   * All entries should start with DirSeparator
   **/
//   private String parseDirectory(String path) {
//     int end = path.lastIndexOf(DirSeparator);
//     if (end != -1) {
//       return path.substring(0, end);
//     } else {
//       return getDirectory(ROOT).getFullPath();
//     }
//   }

  private NSDirMap getDirectory(NSKey dirKey) throws NameNotFoundException {
    synchronized (mapOfMaps) {
      NSDirMap dirMap = (NSDirMap) mapOfMaps.get(dirKey);
      if (dirMap == null) {
        throw new NameNotFoundException("No context found for NSKey - " + 
                                        dirKey);
      }
      return dirMap;
    }
  }

  private void removeDirectory(NSKey dirKey) {
    synchronized (mapOfMaps) {
      mapOfMaps.remove(dirKey);
    }
  }


  private void putDirectory(NSDirKey dirKey, NSDirMap dirMap) {
    synchronized (mapOfMaps) {
      mapOfMaps.put(dirKey, dirMap);
    }
  }

  private static class NSEntry implements Map.Entry, Serializable {
    private Object _key;
    private Object _value;

    public NSEntry(Map.Entry entry) {
      _key = entry.getKey();
      NSItem nsItem = (NSItem) entry.getValue();
      _value = nsItem.getObject();
    }

    public Object getKey() { return _key; }
    public Object getValue() { return _value; }
    public Object setValue(Object value) {
      Object oldValue = _value;
      _value = value;
      return oldValue;
    }
  }

  /**
   * A key to the mapOfMaps (basically a gussied up String).
   **/
  private static class NSDirKey implements NSKey, Serializable {
    String _path;

    public NSDirKey(String path) {
      _path = path;
    }

    public int hashCode() {
      return _path.hashCode();
    }

    public boolean equals(Object o) {
      if (o == this) return true;
      if (o instanceof NSDirKey) {
        NSDirKey that = (NSDirKey) o;
        return this._path.equals(that._path);
      }
      return false;
    }
    public String toString() {
      return _path;
    }
  }

  /**
   * Base class of the two kinds of items we store: directory items
   * and object items. Most of the functionality excluding the actual
   * value is in this class.
   **/
  private static abstract class NSItem {
    protected Collection _attributes;
    protected Set _callbacks;

    public NSItem(Collection attributes) {
      if (attributes != null) {
        _attributes = attributes;
      } else {
        _attributes = new ArrayList();
      }
    }

    public Collection getAttributes() {
      return _attributes;
    }
    
    public void setAttributes(Collection newAttributes) {
      _attributes = newAttributes;
    }

    public abstract Object getObject();

    public abstract Object setObject(Object newObject) throws NamingException;

    public boolean hasCallbacks() {
      return _callbacks != null && !_callbacks.isEmpty();
    }

    public Set getCallbacks() {
      return _callbacks;
    }

    public void addCallback(NSCallback.Id cbid) {
      if (_callbacks == null) _callbacks = new HashSet();
      _callbacks.add(cbid);
    }

    public void removeCallback(NSCallback.Id cbid) {
      if (_callbacks != null) _callbacks.remove(cbid);
    }
  }

  private class NSDirMap extends NSItem {
    private String _fullPath;
    private Map _map = new HashMap(13);

    public NSDirMap(String fullPath, Collection attributes) {
      super(attributes);
      _fullPath = fullPath;
    }

    public void clear() {
      synchronized (_map) {
        _map.clear();
      }
    }

    public Collection getEntries() {
      synchronized (_map) {
        Set entrySet = _map.entrySet();
        List result = new ArrayList(entrySet.size());
        for (Iterator i = entrySet.iterator(); i.hasNext(); ) {
          Map.Entry entry = (Map.Entry) i.next();
          result.add(new NSEntry(entry));
        }
        return result;
      }
    }

    public void removeCallbackFromDirMap(NSCallback.Id cbid) {
      removeCallback(cbid);
      synchronized (_map) {
        for (Iterator i = _map.values().iterator(); i.hasNext(); ) {
          NSItem nsItem = (NSItem) i.next();
          nsItem.removeCallback(cbid);
        }
      }
    }

    public Collection getKeys() {
      synchronized (_map) {
        return new ArrayList(_map.keySet());
      }
    }

    public Collection getValues() {
      synchronized (_map) {
        Collection values = _map.values();
        List result = new ArrayList(values.size());
        for (Iterator i = values.iterator(); i.hasNext(); ) {
          NSItem nsItem = (NSItem) i.next();
          result.add(nsItem.getObject());
        }
        return result;
      }
    }

    public int size() {
      return _map.size();
    }

    public void put(String key, NSItem val) {
      if (logger.isDebugEnabled()) logger.debug("put " + getFullName(key), new Throwable());
      synchronized (_map) {
        _map.put(key, val);
      }
    }

    public NSItem get(String key) {
      if (logger.isDebugEnabled()) logger.debug("get " + getFullName(key), new Throwable());
      synchronized (_map) {
        return (NSItem) _map.get(key);
      }
    }

    public NSItem remove(String key) {
      if (logger.isDebugEnabled()) logger.debug("remove " + getFullName(key), new Throwable());
      synchronized (_map) {
        return (NSItem) _map.remove(key);
      }
    }

    /**
     * Get the external manifestation of the object. For NSDirMaps,
     * the external manifestation is the key to the entry in the map
     * of maps. Override the standard getObject (returning
     **/
    public Object getObject() {
      return getKey();
    }

    public Object setObject(Object newObject) throws NamingException {
      throw new NamingException("Attempt to rename context");
    }

    public String getFullPath() {
      return _fullPath;
    }

    public String getFullName(String name) {
      if (name == null || name.equals("")) return _fullPath;
      return _fullPath + NS.DirSeparator + name;
    }

    public NSDirKey getKey() {
      return new NSDirKey(_fullPath);
    }
  }

  private static class NSObjectAndAttributes extends NSItem {
    private Object _object;

    public NSObjectAndAttributes(Object object, Collection attributes) {
      super(attributes);
      _object = object;
    }

    public Object getObject() {
      return _object;
    }

    public Object setObject(Object newObject) {
      Object oldObject = _object;
      _object = newObject;
      return oldObject;
    }
  }

  private static class CallbackQueue extends Thread {
    CircularQueue queue = new CircularQueue();
    public CallbackQueue() {
      super("NS Callback Q");
    }

    public void add(NSNamingEvent evt) {
      synchronized (this) {
        queue.add(evt);
        notify();
      }
    }

    public void run() {
      Map lists = new HashMap();
      while (true) {
        synchronized (this) {
          while (queue.isEmpty()) {
            try {
              wait();
            } catch (InterruptedException ie) {
            }
          }
          while (!queue.isEmpty()) {
            NSNamingEvent evt = (NSNamingEvent) queue.next();
            NSCallback.Id cbid = evt.getCallbackId();
            List ids = (List) lists.get(cbid.cb);
            if (ids == null) {
              ids = new ArrayList();
              lists.put(cbid.cb, ids);
            }
            ids.add(evt);
          }
        }
        for (Iterator entries = lists.entrySet().iterator(); entries.hasNext(); ) {
          Map.Entry entry = (Map.Entry) entries.next();
          NSCallback cb = (NSCallback) entry.getKey();
          List ids = (List) entry.getValue();
          try {
            cb.dispatch(ids);
          } catch (Throwable t) {
            //          entry.cb.disable();How to do this?
          }
        }
        lists.clear();
      }
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
                         foo.fullName(clustersKey, "subdir"));
      NSKey subdir = 
        foo.createSubDirectory(clustersKey, "subdir");
      for (Iterator a = 
             foo.keySet(clustersKey).iterator(); 
           a.hasNext();){
        System.out.println("\t"+a.next());
      }

      System.out.println("destroySubDirectory: " + subdir);
      foo.destroySubDirectory(clustersKey, "subdir");
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
