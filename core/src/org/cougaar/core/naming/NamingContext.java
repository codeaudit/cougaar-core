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

import java.rmi.RemoteException;

import javax.naming.*;
import javax.naming.spi.*;
import java.util.*;

import org.cougaar.core.society.NameServer;

public class NamingContext implements Context {
  protected Hashtable myEnv;
  protected NS myNS;
  protected NameServer.Directory myDirectory;
  protected String myDirectoryName;
  protected final static NameParser myParser = new NamingParser();

  protected String myClassName = NamingContext.class.getName();  

  public NamingContext(NS ns, NameServer.Directory directory, Hashtable inEnv) {
    myEnv = (inEnv != null) ? (Hashtable)(inEnv.clone()) : null;
    myNS = ns;
    myDirectory = directory;
  }

  public NS getNS() {
    return myNS;
  }

  public NameServer.Directory getDirectory() {
    return myDirectory;
  }
  
  public Object lookup(String name) throws NamingException {
    return lookup(new CompositeName(name));
  }
  
  public Object lookup(Name name) throws NamingException {
    if (name.isEmpty()) {
      // Asking to look up this context itself.  Create and return
      // a new instance with its own independent environment.
      return cloneCtx();
    } 
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    Object nsObj  = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObj == null) {
        throw new NameNotFoundException(name + " not found");
      }
      
      // Call getObjectInstance for using any object factories
      try {
        return NamingManager.getObjectInstance(nsObj, 
                                               new CompositeName().add(atom), 
                                               this, myEnv);
      } catch (Exception e) {
        NamingException ne = new NamingException(
                                                 "getObjectInstance failed");
        ne.setRootCause(e);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom +
                                      " does not name a context");
      }
      
      return ((Context) nsObj).lookup(nm.getSuffix(1));
    }
  }
  
  public void bind(String name, Object obj) throws NamingException {
    bind(new CompositeName(name), obj);
  }
  
  public void bind(Name name, Object obj) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObj = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObj != null) {
        throw new NameAlreadyBoundException(
                                            "Use rebind to override");
      }
      
      // Call getStateToBind for using any state factories
      obj = NamingManager.getStateToBind(obj, 
                                         new CompositeName().add(atom), 
                                         this, myEnv);
      
      // Add object to internal data structure
      try {
        getNS().put(getDirectory(), atom, obj);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      
      ((Context) nsObj).bind(nm.getSuffix(1), obj);
    }
  }
  
  public void rebind(String name, Object obj) throws NamingException {
    rebind(new CompositeName(name), obj);
  }
  
  public void rebind(Name name, Object obj) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    if (nm.size() == 1) {
      // Atomic name
      
      // Call getStateToBind for using any state factories
      obj = NamingManager.getStateToBind(obj, 
                                         new CompositeName().add(atom), 
                                         this, myEnv);
      
      // Add object to internal data structure
      try {
        getNS().put(getDirectory(), atom, obj);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      
      Object nsObj = getNSObject(getDirectory(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
        
      ((Context) nsObj).rebind(nm.getSuffix(1), obj);
    }
  }
  
  public void unbind(String name) throws NamingException {
    unbind(new CompositeName(name));
  }
  
  public void unbind(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot unbind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    // Remove object from internal data structure
    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      try {
        getNS().remove(getDirectory(), atom);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getDirectory(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      ((Context) nsObj).unbind(nm.getSuffix(1));
    }
  }
  
  public void rename(String oldName, String newName) throws NamingException {
    rename(new CompositeName(oldName), new CompositeName(newName));
  }
  
  public void rename(Name oldName, Name newName) throws NamingException {
    if (oldName.isEmpty() || newName.isEmpty()) {
      throw new InvalidNameException("Cannot rename empty name");
    }
    
    // Extract components that belong to this namespace
    Name oldnm = getMyComponents(oldName);
    Name newnm = getMyComponents(newName);
    
    // Simplistic implementation: support only rename within same context
    if (oldnm.size() != newnm.size()) {
      throw new OperationNotSupportedException(
                                               "Do not support rename across different contexts");
    }
    
    String oldatom = oldnm.get(0);
    String newatom = newnm.get(0);
    
    if (oldnm.size() == 1) {
      // Atomic name: Add object to internal data structure
      // Check if new name exists
      if (getNSObject(getDirectory(), newatom) != null) {
        throw new NameAlreadyBoundException(newName.toString() +
                                            " is already bound");
      }
      
      try {
        // Check if old name is bound
        Object oldBinding = getNS().remove(getDirectory(), oldatom);
        if (oldBinding == null) {
          throw new NameNotFoundException(oldName.toString() + " not bound");
        }
        
        getNS().put(getDirectory(), newatom, oldBinding);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Simplistic implementation: support only rename within same context
      if (!oldatom.equals(newatom)) {
        throw new OperationNotSupportedException(
                                                 "Do not support rename across different contexts");
      }
      
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getDirectory(), oldatom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(oldatom +
                                      " does not name a context");
      }
      ((Context) nsObj).rename(oldnm.getSuffix(1), newnm.getSuffix(1));
    }
  }
  
  public NamingEnumeration list(String name) throws NamingException {
    return list(new CompositeName(name));
  }
  
  public NamingEnumeration list(Name name) throws NamingException {
    if (name.isEmpty()) {
      try {
        // listing this context
        return new ListOfNames(getNS().entrySet(getDirectory()).iterator());
      } catch (RemoteException re) {
        re.printStackTrace();
        return null;
      }
    } 
    
    // Perhaps 'name' names a context
    Object target = lookup(name);
    if (target instanceof Context) {
      return ((Context) target).list("");
    }
    throw new NotContextException(name + " cannot be listed");
  }
  
  public NamingEnumeration listBindings(String name) throws NamingException {
    return listBindings(new CompositeName(name));
  }
  
  public NamingEnumeration listBindings(Name name) throws NamingException {
    if (name.isEmpty()) {
      try {
        // listing this context
        return new ListOfBindings(getNS().entrySet(getDirectory()).iterator());
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } 
    
    // Perhaps 'name' names a context
    Object target = lookup(name);
    if (target instanceof Context) {
      return ((Context) target).listBindings("");
    }
    throw new NotContextException(name + " cannot be listed");
  }
  
  public void destroySubcontext(String name) throws NamingException {
    destroySubcontext(new CompositeName(name));
  }
  
  public void destroySubcontext(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException(
                                     "Cannot destroy context using empty name");
    }
    
    // Simplistic implementation: not checking for nonempty context first
    // Use same implementation as unbind
    unbind(name);
  }
  
  public Context createSubcontext(String name) throws NamingException {
    return createSubcontext(new CompositeName(name));
  }
  
  public Context createSubcontext(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObj = getNSObject(getDirectory(), atom);
    
    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObj != null) {
        throw new NameAlreadyBoundException(
                                            "Use rebind to override");
      }
      
      // Add child to internal data structure
      try {
        NameServer.Directory dir = getNS().createSubDirectory(getDirectory(), atom);
        if (dir != null) {
          return createContext(dir);
        } else {
          return null;
        }
      } catch (RemoteException re) {
        re.printStackTrace();
        return null;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      return ((Context) nsObj).createSubcontext(nm.getSuffix(1));
    }
  }
  
  public Object lookupLink(String name) throws NamingException {
    return lookupLink(new CompositeName(name));
  }
  
  public Object lookupLink(Name name) throws NamingException {
    return lookup(name);
  }
  
  public NameParser getNameParser(String name) throws NamingException {
    return getNameParser(new CompositeName(name));
  }
  
  public NameParser getNameParser(Name name) throws NamingException {
    // Do lookup to verify name exists
    Object obj = lookup(name);
    if (obj instanceof Context) {
      ((Context)obj).close();
    }
    return myParser;
  }
  
  public String composeName(String name, String prefix)
    throws NamingException {
    Name result = composeName(new CompositeName(name),
                              new CompositeName(prefix));
    return result.toString();
  }
  
  public Name composeName(Name name, Name prefix) throws NamingException {
    Name result;
    
    // Both are compound names, compose using compound name rules
    if (!(name instanceof CompositeName) &&
        !(prefix instanceof CompositeName)) {
      result = (Name)(prefix.clone());
      result.addAll(name);
      return new CompositeName().add(result.toString());
    }
    
    // Simplistic implementation: do not support federation
    throw new OperationNotSupportedException(
                                             "Do not support composing composite names");
  }
  
  public Object addToEnvironment(String propName, Object propVal)
    throws NamingException {
    if (myEnv == null) {
      myEnv = new Hashtable(5, 0.75f);
    } 
    return myEnv.put(propName, propVal);
  }
  
  public Object removeFromEnvironment(String propName) 
    throws NamingException {
    if (myEnv == null)
      return null;
    
    return myEnv.remove(propName);
  }
  
  public Hashtable getEnvironment() throws NamingException {
    if (myEnv == null) {
      // Must return non-null
      return new Hashtable(3, 0.75f);
    } else {
      return (Hashtable)myEnv.clone();
    }
  }
  
  public String getNameInNamespace() throws NamingException {
    try {
      return getNS().fullName(getDirectory(), "");
    } catch (RemoteException re) {
      re.printStackTrace();
      return null;
    }
  }
  
  public String toString() {
    if (myDirectoryName == null) {
      try {
        Name name = myParser.parse(getDirectory().getPath());      
        if (!name.isEmpty()) {
          myDirectoryName = name.get(name.size() - 1);
        } else {
          myDirectoryName = "ROOT CONTEXT";
        }
      } catch (NamingException ne) {
        ne.printStackTrace();
      }
    }

    return myDirectoryName;
  }
  
  public void close() throws NamingException {
  }


  protected Context cloneCtx() {
    return new NamingContext(myNS, myDirectory, myEnv);
  }

  protected Context createContext(NameServer.Directory dir) {
    return new NamingContext(myNS, dir, myEnv);
  }

  /**
   * Utility method for processing composite/compound name.
   * @param name The non-null composite or compound name to process.
   * @return The non-null string name in this namespace to be processed.
   */
  protected Name getMyComponents(Name name) throws NamingException {
    if (name instanceof CompositeName) {
      /* As it turns out '/' is hard coded as the separator for components of
       * CompositeNames. Since we're using '/' as the CompoundName separator, we
       * can't recognize tell the difference between the two. Since we don't support
       * multiple component CompositeNames, we'll just assume that we don't have any.
       */
      return myParser.parse(name.toString());
    } else {
      // Already parsed
      return name;
    }
  }


  protected Object getNSObject(NameServer.Directory directory, String name) {
    Object nsObject = null;

    try {
      nsObject = getNS().get(getDirectory(), name);
    } catch (RemoteException re) {
      re.printStackTrace();
    }
    
    if (nsObject instanceof NameServer.Directory) {
      return createContext((NameServer.Directory) nsObject);
    } else {
      return nsObject;
    }
  }
    
    // Class for enumerating name/class pairs
  protected class ListOfNames implements NamingEnumeration {
    protected Iterator myEntries;
    
    ListOfNames(Iterator entries) {
      myEntries = entries;
    }
    
    public boolean hasMoreElements() {
      try {
        return hasMore();
      } catch (NamingException e) {
        return false;
      }
    }
    
    public boolean hasMore() throws NamingException {
      return myEntries.hasNext();
    }
    
    public Object next() throws NamingException {
      Map.Entry entry = (Map.Entry) myEntries.next();
      String name = (String) entry.getKey();
      String className;
      if (entry.getValue() instanceof NameServer.Directory) {
        className = myClassName;
      } else {
        className = entry.getValue().getClass().getName();
      }

      return new NameClassPair(name, className);
    }
    
    public Object nextElement() {
      try {
        return next();
      } catch (NamingException e) {
        throw new NoSuchElementException(e.toString());
      }
    }
    
    public void close() {
    }
  }
  // Class for enumerating bindings
  protected class ListOfBindings extends ListOfNames {
    
    ListOfBindings(Iterator entries) {
      super(entries);
    }
    
    public Object next() throws NamingException {
      Map.Entry entry = (Map.Entry) myEntries.next();
      
      if (entry.getValue() instanceof NameServer.Directory) {
        return new Binding((String) entry.getKey(), 
                           createContext((NameServer.Directory) entry.getValue()));
      } else {
        return new Binding((String) entry.getKey(), entry.getValue());
      }
    }
  }  
}




