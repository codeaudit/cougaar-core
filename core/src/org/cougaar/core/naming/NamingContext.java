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

import java.rmi.RemoteException;

import javax.naming.*;
import javax.naming.spi.*;
import java.util.*;

/**
 * Implementation of javax.naming.directory.Context for 
 * Cougaar Naming Service
 */

public class NamingContext implements Context {
  protected Hashtable myEnv;
  protected NS myNS;
  protected NSKey myNSKey;
  protected String myDirectoryName;
  protected String myFullPath;
  protected final static NameParser myParser = new NamingParser();

  protected String myClassName = NamingContext.class.getName();  

  protected NamingContext(NS ns, NSKey nsKey, Hashtable inEnv) {
    myEnv = (inEnv != null) ? (Hashtable)(inEnv.clone()) : null;
    myNS = ns;
    myNSKey = nsKey;
  }

  protected NS getNS() {
    return myNS;
  }

  protected NSKey getNSKey() {
    return myNSKey;
  }
  
  /**
   * Retrieves the named object.
   * See {@link #lookup(Name)} for details.
   * @param name
   *		the name of the object to look up
   * @return	the object bound to <tt>name</tt>
   * @throws	NamingException if a naming exception is encountered
   */
  public Object lookup(String name) throws NamingException {
    return lookup(new CompositeName(name));
  }
  
  /**
   * Retrieves the named object.
   * If <tt>name</tt> is empty, returns a new instance of this context
   * (which represents the same naming context as this context, but its
   * environment may be modified independently and it may be accessed
   * concurrently).
   *
   * @param name
   *		the name of the object to look up
   * @return	the object bound to <tt>name</tt>
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #lookup(String)
   */
  public Object lookup(Name name) throws NamingException {
    if (name.isEmpty()) {
      // Asking to look up this context itself.  Create and return
      // a new instance with its own independent environment.
      return cloneContext();
    } 
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    Object nsObj  = getNSObject(getNSKey(), atom);

    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObj == null) {
        throw new OperationNotSupportedException("Null object not supported.");
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
  
  /**
   * Binds a name to an object.
   * See {@link #bind(Name, Object)} for details.
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a Context
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	javax.naming.directory.InvalidAttributesException
   *	 	if object did not supply all mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   */
  public void bind(String name, Object obj) throws NamingException {
    bind(new CompositeName(name), obj);
  }
  
  /**
   * Binds a name to an object.
   * All intermediate contexts and the target context (that named by all
   * but terminal atomic component of the name) must already exist.
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a Context
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	javax.naming.directory.InvalidAttributesException
   *	 	if object did not supply all mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   *
   * @see #bind(String, Object)
   * @see #rebind(Name, Object)
   */
  public void bind(Name name, Object obj) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    if (obj instanceof Context) {
      throw new OperationNotSupportedException("Use createSubContext to create a Context.");
    }

    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    if (nm.size() == 1) {
      // Call getStateToBind for using any state factories
      obj = NamingManager.getStateToBind(obj, 
                                         new CompositeName().add(atom), 
                                         this, myEnv);
      
      boolean overWrite = false;
      // Add object to internal data structure
      try {
        Object nsObj = getNS().put(getNSKey(), atom, obj, overWrite);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.bind() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), atom);

      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      
      ((Context) nsObj).bind(nm.getSuffix(1), obj);
    }
  }
  
  /**
   * Binds a name to an object, overwriting any existing binding.
   * See {@link #rebind(Name, Object)} for details.
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a Context
   * @throws	javax.naming.directory.InvalidAttributesException
   *	 	if object did not supply all mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   */
  public void rebind(String name, Object obj) throws NamingException {
    rebind(new CompositeName(name), obj);
  }

  /**
   * Binds a name to an object, overwriting any existing binding.
   * All intermediate contexts and the target context (that named by all
   * but terminal atomic component of the name) must already exist.
   *
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a Context
   * @throws	javax.naming.directory.InvalidAttributesException
   *	 	if object did not supply all mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   *
   * @see #rebind(String, Object)
   * @see #bind(Name, Object)
   */
  public void rebind(Name name, Object obj) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    if (obj instanceof Context) {
      throw new OperationNotSupportedException("Can not use rebind with a Context.");
    }

    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    if (nm.size() == 1) {
      // Atomic name
      
      if (getNSObject(getNSKey(), atom) instanceof Context) {
        throw new OperationNotSupportedException("Can not use rebind to replace a Context.");
      }

      // Call getStateToBind for using any state factories
      obj = NamingManager.getStateToBind(obj, 
                                         new CompositeName().add(atom), 
                                         this, myEnv);
      
      boolean overWrite = true;
      // Add object to internal data structure
      try {
        getNS().put(getNSKey(), atom, obj, overWrite);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.rebind() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
        
      ((Context) nsObj).rebind(nm.getSuffix(1), obj);
    }
  }
  
  /**
   * Unbinds the named object.
   * See {@link #unbind(Name)} for details.
   *
   * @param name
   *		the name to unbind; may not be empty
   * @throws	NameNotFoundException if an intermediate context does not exist
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if associated object is a 
   *            Context
   */
  public void unbind(String name) throws NamingException {
    unbind(new CompositeName(name));
  }
  
  /**
   * Unbinds the named object.
   * Removes the terminal atomic name in <code>name</code>
   * from the target context--that named by all but the terminal
   * atomic part of <code>name</code>.
   *
   * <p> This method is idempotent.
   * It succeeds even if the terminal atomic name
   * is not bound in the target context, but throws
   * <tt>NameNotFoundException</tt>
   * if any of the intermediate contexts do not exist.
   *
   * <p> Any attributes associated with the name are removed.
   * Intermediate contexts are not changed.
   *
   * @param name
   *		the name to unbind; may not be empty
   * @throws	NameNotFoundException if an intermediate context does not exist
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if associated object is a 
   *            Context
   * @see #unbind(String)
   */
  public void unbind(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Can not unbind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    // Remove object from internal data structure
    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      try {
        getNS().remove(getNSKey(), atom);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.unbind() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      ((Context) nsObj).unbind(nm.getSuffix(1));
    }
  }
  
  /**
   * Binds a new name to the object bound to an old name, and unbinds
   * the old name.
   * See {@link #rename(Name, Name)} for details.
   *
   * @param oldName
   *		the name of the existing binding; may not be empty
   * @param newName
   *		the name of the new binding; may not be empty
   * @throws	NameAlreadyBoundException if <tt>newName</tt> is already bound
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if associated object is a 
   *            Context
   */
  public void rename(String oldName, String newName) throws NamingException {
    rename(new CompositeName(oldName), new CompositeName(newName));
  }
  
  /**
   * Binds a new name to the object bound to an old name, and unbinds
   * the old name.  Both names are relative to this context.
   * Any attributes associated with the old name become associated
   * with the new name.
   * Intermediate contexts of the old name are not changed.
   *
   * @param oldName
   *		the name of the existing binding; may not be empty
   * @param newName
   *		the name of the new binding; may not be empty
   * @throws	NameAlreadyBoundException if <tt>newName</tt> is already bound
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if associated object is a 
   *            Context
   *
   * @see #rename(String, String)
   * @see #bind(Name, Object)
   * @see #rebind(Name, Object)
   */
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
    
    String oldAtom = oldnm.get(0);
    String newAtom = newnm.get(0);
    
    if (oldnm.size() == 1) {
      // Atomic name: Add object to internal data structure
      // Check if new name exists
      try {
        if (getNS().rename(getNSKey(), oldAtom, newAtom) == null) {
          throw new NamingException("Unable to rename " + oldName.toString());
        }
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.rename() failed for " + oldName);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Simplistic implementation: support only rename within same context
      if (!oldAtom.equals(newAtom)) {
        throw new OperationNotSupportedException(
                                                 "Do not support rename across different contexts");
      }
      
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), oldAtom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(oldAtom +
                                      " does not name a context");
      }
      ((Context) nsObj).rename(oldnm.getSuffix(1), newnm.getSuffix(1));
    }
  }
  
  /**
   * Enumerates the names bound in the named context, along with the
   * class names of objects bound to them.
   * See {@link #list(Name)} for details.
   *
   * @param name
   *		the name of the context to list
   * @return	an enumeration of the names and class names of the
   *		bindings in this context.  Each element of the
   *		enumeration is of type <tt>NameClassPair</tt>.
   * @throws	NamingException if a naming exception is encountered
   */
  public NamingEnumeration list(String name) throws NamingException {
    return list(new CompositeName(name));
  }
  
  /**
   * Enumerates the names bound in the named context, along with the
   * class names of objects bound to them.
   * The contents of any subcontexts are not included.
   *
   * <p> If a binding is added to or removed from this context,
   * its effect on an enumeration previously returned is undefined.
   *
   * @param name
   *		the name of the context to list
   * @return	an enumeration of the names and class names of the
   *		bindings in this context.  Each element of the
   *		enumeration is of type <tt>NameClassPair</tt>.
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #list(String)
   * @see #listBindings(Name)
   * @see NameClassPair
   */
  public NamingEnumeration list(Name name) throws NamingException {
    if (name.isEmpty()) {
      try {
        // listing this context
        return new ListOfNames(getNS().entrySet(getNSKey()).iterator());
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.list() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } 
    
    // Perhaps 'name' names a context
    Object target = lookup(name);
    if (target instanceof Context) {
      return ((Context) target).list("");
    }
    throw new NotContextException(name + " cannot be listed");
  }
  
  /**
   * Enumerates the names bound in the named context, along with the
   * objects bound to them.
   * See {@link #listBindings(Name)} for details.
   *
   * @param name
   *		the name of the context to list
   * @return	an enumeration of the bindings in this context.
   *		Each element of the enumeration is of type
   *		<tt>Binding</tt>.
   * @throws	NamingException if a naming exception is encountered
   */
  public NamingEnumeration listBindings(String name) throws NamingException {
    return listBindings(new CompositeName(name));
  }
  
  /**
   * Enumerates the names bound in the named context, along with the
   * objects bound to them.
   * The contents of any subcontexts are not included.
   *
   * <p> If a binding is added to or removed from this context,
   * its effect on an enumeration previously returned is undefined.
   *
   * @param name
   *		the name of the context to list
   * @return	an enumeration of the bindings in this context.
   *		Each element of the enumeration is of type
   *		<tt>Binding</tt>.
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #listBindings(String)
   * @see #list(Name)
   * @see Binding
   */
  public NamingEnumeration listBindings(Name name) throws NamingException {
    if (name.isEmpty()) {
      try {
        // listing this context
        return new ListOfBindings(getNS().entrySet(getNSKey()).iterator());
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.listBindings() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } 
    
    // Perhaps 'name' names a context
    Object target = lookup(name);
    if (target instanceof Context) {
      return ((Context) target).listBindings("");
    }
    throw new NotContextException(name + " cannot be listed");
  }
  
  /**
   * Destroys the named context and removes it from the namespace.
   * See {@link #destroySubcontext(Name)} for details.
   *
   * @param name
   *		the name of the context to be destroyed; may not be empty
   * @throws	NameNotFoundException if an intermediate context does not exist
   * @throws	NotContextException if the name is bound but does not name a
   *		context, or does not name a context of the appropriate type
   * @throws	ContextNotEmptyException if the named context is not empty
   * @throws	NamingException if a naming exception is encountered
   */
  public void destroySubcontext(String name) throws NamingException {
    destroySubcontext(new CompositeName(name));
  }
  
  /**
   * Destroys the named context and removes it from the namespace.
   * Any attributes associated with the name are also removed.
   * Intermediate contexts are not destroyed.
   *
   * <p> This method is idempotent.
   * It succeeds even if the terminal atomic name
   * is not bound in the target context, but throws
   * <tt>NameNotFoundException</tt>
   * if any of the intermediate contexts do not exist.
   *
   * @param name
   *		the name of the context to be destroyed; may not be empty
   * @throws	NameNotFoundException if an intermediate context does not exist
   * @throws	NotContextException if the name is bound but does not name a
   *		context, or does not name a context of the appropriate type
   * @throws	ContextNotEmptyException if the named context is not empty
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #destroySubcontext(String)
   */
  public void destroySubcontext(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException(
                                     "Cannot destroy context using empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    if (nm.size() == 1) {
      // Try to remove sub context from internal data structure
      try {
        getNS().destroySubDirectory(getNSKey(), atom);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.destroySubContext() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      ((Context) nsObj).destroySubcontext(nm.getSuffix(1));
    }
  }

  
  /**
   * Creates and binds a new context.
   * See {@link #createSubcontext(Name)} for details.
   *
   * @param name
   *		the name of the context to create; may not be empty
   * @return	the newly created context
   *
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	javax.naming.directory.InvalidAttributesException
   *		if creation of the subcontext requires specification of
   *		mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   */
  public Context createSubcontext(String name) throws NamingException {
    return createSubcontext(new CompositeName(name));
  }
  
  /**
   * Creates and binds a new context.
   * Creates a new context with the given name and binds it in
   * the target context (that named by all but terminal atomic
   * component of the name).  All intermediate contexts and the
   * target context must already exist.
   *
   * @param name
   *		the name of the context to create; may not be empty
   * @return	the newly created context
   *
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	javax.naming.directory.InvalidAttributesException
   *		if creation of the subcontext requires specification of
   *		mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #createSubcontext(String)
   * @see javax.naming.directory.DirContext#createSubcontext
   */
  public Context createSubcontext(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    
    if (nm.size() == 1) {
      // Try to add child to internal data structure
      try {
        NSKey nsKey = getNS().createSubDirectory(getNSKey(), atom);
        if (nsKey != null) {
          return createContext(nsKey);
        } else {
          throw new NamingException("Unable to create subcontext.");
        }
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.createSubcontext() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObj = getNSObject(getNSKey(), atom);
      if (!(nsObj instanceof Context)) {
        throw new NotContextException(atom + 
                                      " does not name a context");
      }
      return ((Context) nsObj).createSubcontext(nm.getSuffix(1));
    }
  }
  
  /**
   * Retrieves the named object, following links except
   * for the terminal atomic component of the name.
   * See {@link #lookupLink(Name)} for details.
   *
   * @param name
   *		the name of the object to look up
   * @return	the object bound to <tt>name</tt>, not following the
   *		terminal link (if any)
   * @throws	NamingException if a naming exception is encountered
   */
  public Object lookupLink(String name) throws NamingException {
    return lookupLink(new CompositeName(name));
  }
  
  /**
   * Retrieves the named object, following links except
   * for the terminal atomic component of the name.
   * If the object bound to <tt>name</tt> is not a link,
   * returns the object itself.
   *
   * @param name
   *		the name of the object to look up
   * @return	the object bound to <tt>name</tt>, not following the
   *		terminal link (if any).
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #lookupLink(String)
   */
  public Object lookupLink(Name name) throws NamingException {
    return lookup(name);
  }
  
  /**
   * Retrieves the parser associated with the named context.
   * See {@link #getNameParser(Name)} for details.
   *
   * @param name
   *		the name of the context from which to get the parser
   * @return	a name parser that can parse compound names into their atomic
   *		components
   * @throws	NamingException if a naming exception is encountered
   */
  public NameParser getNameParser(String name) throws NamingException {
    return getNameParser(new CompositeName(name));
  }
  
  /**
   * Retrieves the parser associated with the named context. Current
   * implementation does not support federated namespaces.
   *
   * @param name
   *		the name of the context from which to get the parser
   * @return	a name parser that can parse compound names into their atomic
   *		components
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #getNameParser(String)
   * @see CompoundName
   */
  public NameParser getNameParser(Name name) throws NamingException {
    // Do lookup to verify name exists
    Object obj = lookup(name);
    if (obj instanceof Context) {
      ((Context)obj).close();
    }
    return myParser;
  }
  
  /**
   * Composes the name of this context with a name relative to
   * this context.
   * See {@link #composeName(Name, Name)} for details.
   *
   * @param name
   *		a name relative to this context
   * @param prefix
   *		the name of this context relative to one of its ancestors
   * @return	the composition of <code>prefix</code> and <code>name</code>
   * @throws	NamingException if a naming exception is encountered
   */
  public String composeName(String name, String prefix)
    throws NamingException {
    Name result = composeName(new CompositeName(name),
                              new CompositeName(prefix));
    return result.toString();
  }
  
  /**
   * Composes the name of this context with a name relative to
   * this context.
   * Given a name (<code>name</code>) relative to this context, and
   * the name (<code>prefix</code>) of this context relative to one
   * of its ancestors, this method returns the composition of the
   * two names using the syntax appropriate for the naming
   * system(s) involved.  That is, if <code>name</code> names an
   * object relative to this context, the result is the name of the
   * same object, but relative to the ancestor context.  None of the
   * names may be null.
   * <p>
   * For example, if this context is named "org/research", then
   * <pre>
   *	composeName("user/jane", "org/research")	</pre>
   * might return <code>"org/research/user/jane"</code> while
   * <pre>
   *	composeName("user/jane", "research")	</pre>
   * returns <code>"research/user/jane"</code>.
   *
   * @param name
   *		a name relative to this context
   * @param prefix
   *		the name of this context relative to one of its ancestors
   * @return	the composition of <code>prefix</code> and <code>name</code>
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #composeName(String, String)
   */
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
  
  /**
   * Adds a new environment property to the environment of this
   * context.  If the property already exists, its value is overwritten.
   * See class description for more details on environment properties.
   *
   * @param propName
   *		the name of the environment property to add; may not be null
   * @param propVal
   *		the value of the property to add; may not be null
   * @return	the previous value of the property, or null if the property was
   *		not in the environment before
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #getEnvironment()
   * @see #removeFromEnvironment(String)
   */
  public Object addToEnvironment(String propName, Object propVal)
    throws NamingException {
    if (myEnv == null) {
      myEnv = new Hashtable(5, 0.75f);
    } 
    return myEnv.put(propName, propVal);
  }
  
  /**
   * Removes an environment property from the environment of this
   * context.  See class description for more details on environment
   * properties.
   *
   * @param propName
   *		the name of the environment property to remove; may not be null
   * @return	the previous value of the property, or null if the property was
   *		not in the environment
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #getEnvironment()
   * @see #addToEnvironment(String, Object)
   */
  public Object removeFromEnvironment(String propName) 
    throws NamingException {
    if (myEnv == null)
      return null;
    
    return myEnv.remove(propName);
  }
  
  /**
   * Retrieves the environment in effect for this context.
   * See class description for more details on environment properties.
   *
   * <p> The caller should not make any changes to the object returned:
   * their effect on the context is undefined.
   * The environment of this context may be changed using
   * <tt>addToEnvironment()</tt> and <tt>removeFromEnvironment()</tt>.
   *
   * @return	the environment of this context; never null
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #addToEnvironment(String, Object)
   * @see #removeFromEnvironment(String)
   */
  public Hashtable getEnvironment() throws NamingException {
    if (myEnv == null) {
      // Must return non-null
      return new Hashtable(3, 0.75f);
    } else {
      return (Hashtable)myEnv.clone();
    }
  }
  
  /**
   * Retrieves the full name of this context within its own namespace.
   *
   * <p> Many naming services have a notion of a "full name" for objects
   * in their respective namespaces.  For example, an LDAP entry has
   * a distinguished name, and a DNS record has a fully qualified name.
   * This method allows the client application to retrieve this name.
   * The string returned by this method is not a JNDI composite name
   * and should not be passed directly to context methods.
   * In naming systems for which the notion of full name does not
   * make sense, <tt>OperationNotSupportedException</tt> is thrown.
   *
   * @return	this context's name in its own namespace; never null
   * @throws	OperationNotSupportedException if the naming system does
   *		not have the notion of a full name
   * @throws	NamingException if a naming exception is encountered
   *
   * @since 1.3
   */
  public String getNameInNamespace() throws NamingException {
    if (myFullPath == null) {
      try {
        myFullPath = getNS().fullName(getNSKey(), "");

        // Strip leading dir separator
        while (myFullPath.startsWith(NS.DirSeparator)) {
          myFullPath = myFullPath.substring(1, myFullPath.length());
        }


      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.getNameInNamespace() failed for " + this);
        ne.setRootCause(re);
        throw ne;
      }
    }

    return myFullPath;
  }
  
  public String toString() {
    if (myDirectoryName == null) {
      try {
        Name name = myParser.parse(getNameInNamespace());      
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
  
  /**
   * Closes this context.
   * This method releases this context's resources immediately, instead of
   * waiting for them to be released automatically by the garbage collector.
   *
   * <p> Current implementation does nothing.
   *
   * @throws	NamingException if a naming exception is encountered
   */
  public void close() throws NamingException {
  }


  protected Context cloneContext() {
    return new NamingContext(myNS, myNSKey, myEnv);
  }

  protected Context createContext(NSKey nsKey) {
    return new NamingContext(myNS, nsKey, myEnv);
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


  protected Object getNSObject(NSKey nsKey, String name) 
    throws NameNotFoundException, NamingException {
    Object nsObject = null;

    try {
      nsObject = getNS().get(getNSKey(), name);
    } catch (RemoteException re) {
      NamingException ne = 
        new NamingException("NamingContext.getNSObject() failed for " + name);
      ne.setRootCause(re);
      throw ne;
    }
    
    if (nsObject instanceof NSKey) {
      return createContext((NSKey) nsObject);
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
      if (entry.getValue() instanceof NSKey) {
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
      
      if (entry.getValue() instanceof NSKey) {
        return new Binding((String) entry.getKey(), 
                           createContext((NSKey) entry.getValue()));
      } else {
        return new Binding((String) entry.getKey(), entry.getValue());
      }
    }
  }  
}




