/*
 * <Copyright>
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
import javax.naming.directory.*;
import javax.naming.spi.*;
import java.util.*;

/**
 * Implementation of javax.naming.directory.DirContext for 
 * Cougaar Naming Service
 */

public class NamingDirContext extends NamingContext implements DirContext {
  protected final static NameParser myParser = new NamingParser();
  protected final static SearchStringParser filterParser = new SearchStringParser();
  protected final static SearchControls defaultSearchControls = new SearchControls();
  
  protected NamingDirContext(NS ns, NSKey nsKey, Hashtable inEnv) {
    super(ns, nsKey, inEnv);

    myClassName = NamingDirContext.class.getName();
  }

  protected Context cloneContext() {
    return new NamingDirContext(myNS, myNSKey, myEnv);
  }

  protected Context createContext(NSKey nsKey) {
    return new NamingDirContext(myNS, nsKey, myEnv);
  }

  /**
   * Retrieves all of the attributes associated with a named object.
   * See {@link #getAttributes(Name)} for details.
   *
   * @param name
   *		the name of the object from which to retrieve attributes
   * @return	the set of attributes associated with <code>name</code>
   *
   * @throws	NamingException if a naming exception is encountered
   */
  public Attributes getAttributes(String name) throws NamingException {
    return getAttributes(new CompositeName(name));
  }
  

  /**
   * Retrieves all of the attributes associated with a named object.
   *
   * @param name
   *		the name of the object from which to retrieve attributes
   * @return	the set of attributes associated with <code>name</code>.
   *		Returns an empty attribute set if name has no attributes;
   *		never null.
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #getAttributes(String)
   * @see #getAttributes(Name, String[])
   */
  public Attributes getAttributes(Name name) throws NamingException {
    return getAttributes(name, null);  // same as attrIds == null
  }


  /**
   * Retrieves selected attributes associated with a named object.
   *
   * @param name
   *		The name of the object from which to retrieve attributes
   * @param attrIds
   *		the identifiers of the attributes to retrieve.
   * 		null indicates that all attributes should be retrieved;
   *     	an empty array indicates that none should be retrieved.
   * @return	the requested attributes; never null
   *
   * @throws	NamingException if a naming exception is encountered
   */
  public Attributes getAttributes(String name, String[] attrIds)
    throws NamingException {
    return getAttributes(new CompositeName(name), attrIds);
  }

  /**
   * Retrieves selected attributes associated with a named object.
   *
   * <p> If the object does not have an attribute
   * specified, the directory will ignore the nonexistent attribute
   * and return those requested attributes that the object does have.
   *
   * @param name
   *		the name of the object from which to retrieve attributes
   * @param attrIds
   *		the identifiers of the attributes to retrieve.
   * 		null indicates that all attributes should be retrieved;
   *     	an empty array indicates that none should be retrieved.
   * @return	the requested attributes; never null
   *
   * @throws	NamingException if a naming exception is encountered
   */
  public Attributes getAttributes(Name name, String[] attrIds) 
    throws NamingException {
    if (name.isEmpty()) {
      // Asking for attributes of this context.
      Collection attributes;
      try {
        attributes = getNS().getAttributes(getNSKey(), "");
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingDirContext.getAttributes failed for Context - " + this); 
        ne.setRootCause(re);
        throw ne;
      }

      return deepClone(matchingAttributes(attributes, attrIds));
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    if (nm.size() == 1) {
      Collection attributes = null;
      try {
        attributes = getNS().getAttributes(getNSKey(), atom);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.getAttributes() failed for " + 
                              name);
        ne.setRootCause(re);
        throw ne;
      }

      // Atomic name: Find object in internal data structure
      if (attributes == null) {
        return new BasicAttributes();
      } else {
        return deepClone(matchingAttributes(attributes, attrIds));
      }
    } else {
      Object nsObject = getNSObject(getNSKey(), atom);
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom +
                                      " does not name a DirContext");
      }
      
      return ((DirContext) nsObject).getAttributes(nm.getSuffix(1), attrIds);
    }
  }

  /**
   * Modifies the attributes associated with a named object.
   * See {@link #modifyAttributes(Name, int, Attributes)} for details.
   *
   * @param name
   *		the name of the object whose attributes will be updated
   * @param mod_op
   *		the modification operation, one of:
   *			<code>ADD_ATTRIBUTE</code>,
   *			<code>REPLACE_ATTRIBUTE</code>,
   *			<code>REMOVE_ATTRIBUTE</code>.
   * @param attrs
   *		the attributes to be used for the modification; map not be null
   *
   * @throws	AttributeModificationException if the modification cannot
   *		be completed successfully
   * @throws	NamingException if a naming exception is encountered
   */
  public void modifyAttributes(String name, int mod_op, Attributes attrs)
	throws NamingException {
    modifyAttributes(new CompositeName(name), mod_op, attrs);
  }
  
  /**
   * Modifies the attributes associated with a named object.
   * The order of the modifications is not specified.  Where
   * possible, the modifications are performed atomically.
   *
   * @param name
   *		the name of the object whose attributes will be updated
   * @param mod_op
   *		the modification operation, one of:
   *			<code>ADD_ATTRIBUTE</code>,
   *			<code>REPLACE_ATTRIBUTE</code>,
   *			<code>REMOVE_ATTRIBUTE</code>.
   * @param attrs
   *		the attributes to be used for the modification; may not be null
   *
   * @throws	AttributeModificationException if the modification cannot
   *		be completed successfully
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #modifyAttributes(Name, ModificationItem[])
   */
  public void modifyAttributes(Name name, int mod_op, Attributes attrs)
    throws NamingException {
    if (attrs == null || attrs.size() == 0) {
      throw new IllegalArgumentException("Cannot modify without an attribute");
    }
    
    // Turn it into a modification list and pass it on
    NamingEnumeration attrEnum = attrs.getAll();
    ModificationItem[] mods = new ModificationItem[attrs.size()];
    for (int i = 0; i < mods.length && attrEnum.hasMoreElements(); i++) {
      mods[i] = new ModificationItem(mod_op, (Attribute)(attrEnum.next()));
    }
    
    modifyAttributes(name, mods);
  }
  
    /**
     * Modifies the attributes associated with a named object using an
     * an ordered list of modifications.
     * See {@link #modifyAttributes(Name, ModificationItem[])} for details.
     *
     * @param name
     *		the name of the object whose attributes will be updated
     * @param mods
     *		an ordered sequence of modifications to be performed;
     *		may not be null
     *
     * @throws	AttributeModificationException if the modifications
     *		cannot be completed successfully
     * @throws	NamingException if a naming exception is encountered
     */
  public void modifyAttributes(String name, ModificationItem[] mods)
    throws NamingException {
    modifyAttributes(new CompositeName(name), mods);
  }

  /**
   * Modifies the attributes associated with a named object using an
   * an ordered list of modifications.
   * The modifications are performed
   * in the order specified.  Each modification specifies a
   * modification operation code and an attribute on which to
   * operate.  Where possible, the modifications are
   * performed atomically.
   *
   * @param name
   *		the name of the object whose attributes will be updated
   * @param mods
   *		an ordered sequence of modifications to be performed;
   *		may not be null
   *
   * @throws	AttributeModificationException if the modifications
   *		cannot be completed successfully
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #modifyAttributes(Name, int, Attributes)
   * @see ModificationItem
   */  
  public void modifyAttributes(Name name, ModificationItem[] mods)
    throws NamingException {
    if (name.isEmpty()) {
      // Updating attributes of this context.
      try {
        //Clone Attributes and ModificationItems so that parameters aren't 
        // modified by processing
        Collection cloneAttrs = 
          deepClone(getNS().getAttributes(getNSKey(), ""));
        ModificationItem[] cloneMods = deepClone(mods);

        getNS().putAttributes(getNSKey(), "", 
                              doMods(cloneAttrs, cloneMods));
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingDirContext.modifyAttributes() failed for DirContext - " + this);
        ne.setRootCause(re);
        throw ne;
      }
      return;
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    if (nm.size() == 1) {
      try {
        //Clone Attributes and ModificationItems so that parameters aren't 
        // modified by processing
        Collection cloneAttrs = 
          deepClone(getNS().getAttributes(getNSKey(), atom));
        ModificationItem[] cloneMods = deepClone(mods);

        getNS().putAttributes(getNSKey(), atom, 
                              doMods(cloneAttrs, cloneMods));
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingDirContext.modifyAttributes() failed for " + atom);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      Object nsObject = getNSObject(getNSKey(), atom);
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom +
                                      " does not name a DirContext");
      }
      
      ((DirContext) nsObject).modifyAttributes(nm.getSuffix(1), mods);
    }
  }
  
  
  // Some overrides that take directory into account
  
  // Need to get attributes and use DirectoryManager.getObjectInstance
  public Object lookup(Name name) throws NamingException {
    if (name.isEmpty()) {
      // Asking to look up this context itself.  Create and return
      // a new instance with its own independent environment.
      return cloneContext();
    } 
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObject = getNSObject(getNSKey(), atom);

    if (nm.size() == 1) {
      // Get attributes
      Attributes attrs = getAttributes(name);
      
      // Call getObjectInstance for using any object factories
      try {
        return DirectoryManager.getObjectInstance(nsObject,
                                                  new CompositeName().add(atom), 
                                                  this, myEnv, attrs);
      } catch (Exception e) {
        NamingException ne = 
          new NamingException("NamingDirContext.lookup failed for " + atom);
        ne.setRootCause(e);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof Context)) {
        throw new NotContextException(atom +
                                      " does not name a context");
      }
      
      return ((Context) nsObject).lookup(nm.getSuffix(1));
    }
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
        return new ListOfDirBindings(getNS().entrySet(getNSKey()).iterator());
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingDirContext.listBinding() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }  
    } 
    
    // Perhaps 'name' names a context
    Object target = lookup(name);
    if (target instanceof Context) {
      return ((Context) target).listBindings("");
    } else {
      throw new NotContextException(name + " is not a Context");
    }
  }


  /**
   * Binds a name to an object.
   * Override NamingContext version to account for attributes
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
   * @see #bind(Name, Object, javax.naming.directory.Attributes)
   */
  public void bind(Name name, Object obj) throws NamingException {
    bind(name, obj, new BasicAttributes());
  }
  
  /**
   * Binds a name to an object, along with associated attributes.
   * See {@link #bind(Name, Object, Attributes)} for details.
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a Context
   * @param attrs
   *		the attributes to associate with the binding
   *
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	InvalidAttributesException if some "mandatory" attributes
   *		of the binding are not supplied
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   */
  public void bind(String name, Object obj, Attributes attrs)
    throws NamingException {
    bind(new CompositeName(name), obj, attrs);
  }
  
  /**
   * Binds a name to an object, along with associated attributes.
   * If <tt>attrs</tt> is null, the resulting binding will not have
   * any attributes.
   * If <tt>attrs</tt> is non-null, the resulting binding will have
   * <tt>attrs</tt> as its attributes; any attributes associated with
   * <tt>obj</tt> are ignored.
   *
   * <p>NOTE: The object must not be a <tt>Context</tt> and must not be null.
   * Does not accept null obj (throws NullPointerException)

   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; must not be null or a Context
   * @param attrs
   *		the attributes to associate with the binding
   *
   * @throws	NameAlreadyBoundException if name is already bound
   * @throws	InvalidAttributesException if some "mandatory" attributes
   *		of the binding are not supplied
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   *
   */
  public void bind(Name name, Object obj, Attributes attrs)
    throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind to an empty name");
    }
    
    if (obj instanceof Context) {
      throw new OperationNotSupportedException("Use createSubContext to create a Context.");
    }

    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);

    if (nm.size() == 1) {
      // Call getStateToBind for using any state factories
      DirStateFactory.Result res = 
        DirectoryManager.getStateToBind(obj, new CompositeName().add(atom), this, 
                                        myEnv, attrs);
      Attributes cloneAttributes = deepClone(res.getAttributes());
      NamingEnumeration cloneEnum= cloneAttributes.getAll();
      ArrayList arrayList = new ArrayList(cloneAttributes.size());

      while (cloneEnum.hasMore()) {
        arrayList.add(((Attribute) cloneEnum.next()).clone());
      }
      
      // Add object to internal data structure
      boolean overWrite = false;
      try {
        getNS().put(getNSKey(), atom, res.getObject(), arrayList, overWrite);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingDirContext.bind() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      } catch (IllegalArgumentException iae) {
        NamingException ne = 
          new NamingException("NamingDirContext.bind() failed for " + name);
        ne.setRootCause(iae);
        throw ne;
      }
    } else {
      Object nsObject = getNSObject(getNSKey(), atom);

      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom + 
                                      " does not name a DirContext");
      }
      ((DirContext) nsObject).bind(nm.getSuffix(1), obj, attrs);
    }
  }
  

  /**
   * Binds a name to an object, overwriting any existing binding. 
   * All intermediate contexts and the target context (that named by all
   * but terminal atomic component of the name) must already exist.
   *
   * <p>NOTE: The object must not be a <tt>Context</tt> and must not be null.
   *
   * <p>Override NamingContext version to account for clearing associated 
   * attributes
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; must not be null or a <tt>Context<tt>
   * @throws	javax.naming.directory.InvalidAttributesException
   *	 	if object did not supply all mandatory attributes
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   *
   */
  public void rebind(Name name, Object obj) throws NamingException {
    rebind(name, obj, new BasicAttributes());
  }
  
  /**
   * Binds a name to an object, along with associated attributes,
   * overwriting any existing binding.
   * See {@link #rebind(Name, Object, Attributes)} for details.
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a <tt>Context<tt>
   * @param attrs
   *		the attributes to associate with the binding
   *
   * @throws	InvalidAttributesException if some "mandatory" attributes
   *		of the binding are not supplied
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   */
  public void rebind(String name, Object obj, Attributes attrs)
    throws NamingException {
    rebind(new CompositeName(name), obj, attrs);
  }
  
  /**
   * Binds a name to an object, along with associated attributes,
   * overwriting any existing binding.
   * If <tt>attrs</tt> is null any existing attributes associated with the
   * object already bound in the directory remain unchanged.
   * If <tt>attrs</tt> is non-null, any existing attributes associated with
   * the object already bound in the directory are removed and <tt>attrs</tt>
   * is associated with the named object. 
   *
   * @param name
   *		the name to bind; may not be empty
   * @param obj
   *		the object to bind; may not be null or a <tt>Context<tt>
   * @param attrs
   *		the attributes to associate with the binding
   *
   * @throws	InvalidAttributesException if some "mandatory" attributes
   *		of the binding are not supplied
   * @throws	NamingException if a naming exception is encountered
   * @throws    OperationNotSupportedException if obj is a Context
   *
   * @see #bind(Name, Object, Attributes)
   */
  public void rebind(Name name, Object obj, Attributes attrs)
    throws NamingException {
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
      // Call getStateToBind for using any state factories
      DirStateFactory.Result res = 
        DirectoryManager.getStateToBind(obj, new CompositeName().add(atom), this, myEnv, 
                                        attrs);
      Attributes cloneAttributes = deepClone(res.getAttributes());
      NamingEnumeration cloneEnum= cloneAttributes.getAll();
      ArrayList arrayList = new ArrayList(cloneAttributes.size());

      while (cloneEnum.hasMore()) {
        arrayList.add(((Attribute) cloneEnum.next()).clone());
      }

      // Add object to internal data structure
      boolean overWrite = true;
      try {
        getNS().put(getNSKey(), atom, res.getObject(), arrayList, overWrite);
      } catch (RemoteException re) {
        NamingException ne = 
          new NamingException("NamingContext.rebind() failed for " + name);
        ne.setRootCause(re);
        throw ne;
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      // Atomic name
      Object nsObject = getNSObject(getNSKey(), atom);

      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom + " does not name a DirContext.");
      }
      ((DirContext) nsObject).rebind(nm.getSuffix(1), obj, attrs);
    }
  }
  
  /**
   * Creates and binds a new context, along with associated attributes.
   * See {@link #createSubcontext(Name, Attributes)} for details.
   *
   * @param name
   *		the name of the context to create; may not be empty
   * @param attrs
   *		the attributes to associate with the newly created context
   * @return	the newly created context
   *
   * @throws	NameAlreadyBoundException if the name is already bound
   * @throws	InvalidAttributesException if <code>attrs</code> does not
   *		contain all the mandatory attributes required for creation
   * @throws	NamingException if a naming exception is encountered
   */
  public DirContext createSubcontext(String name, Attributes attrs)
    throws NamingException {
    return createSubcontext(new CompositeName(name), attrs);
  }
  
  /**
   * Creates and binds a new context, along with associated attributes.
   * This method creates a new subcontext with the given name, binds it in
   * the target context (that named by all but terminal atomic
   * component of the name), and associates the supplied attributes
   * with the newly created object.
   * All intermediate and target contexts must already exist.
   * If <tt>attrs</tt> is null, this method is equivalent to
   * <tt>NamingContext.createSubcontext()</tt>.
   *
   * @param name
   *		the name of the context to create; may not be empty
   * @param attrs
   *		the attributes to associate with the newly created context
   * @return	the newly created context
   *
   * @throws	NameAlreadyBoundException if the name is already bound
   * @throws	InvalidAttributesException if <code>attrs</code> does not
   *		contain all the mandatory attributes required for creation
   * @throws	NamingException if a naming exception is encountered
   *
   * @see NamingContext#createSubcontext(Name)
   */
  public DirContext createSubcontext(Name name, Attributes attrs)
    throws NamingException {
    // First create context
    DirContext ctx = (DirContext)createSubcontext(name);
    
    
    // Add attributes
    if (attrs != null && attrs.size() > 0) {
      ctx.modifyAttributes("", DirContext.ADD_ATTRIBUTE, attrs);
    }
    return ctx;
  }
  
  /**
   * Not Supported - will throw OperationNotSupportedException if called
   */
  public DirContext getSchema(String name) throws NamingException {
    return getSchema(new CompositeName(name));
  }
  
  /**
   * Not Supported - will throw OperationNotSupportedException if called
   */
  public DirContext getSchema(Name name) throws NamingException {
    throw new OperationNotSupportedException("Not implemented yet");
  }
  
  /**
   * Not Supported - will throw OperationNotSupportedException if called
   */
  public DirContext getSchemaClassDefinition(String name)
    throws NamingException {
    return getSchemaClassDefinition(new CompositeName(name));
  }

  /**
   * Not Supported - will throw OperationNotSupportedException if called
   */
  public DirContext getSchemaClassDefinition(Name name)
    throws NamingException {
    throw new OperationNotSupportedException("Not implemented yet");
  }
  
  /**
   * Searches in a single context for objects that contain a
   * specified set of attributes.
   * See {@link #search(Name, Attributes)} for details.
   *
   * @param name
   *		the name of the context to search
   * @param matchingAttributes
   *		the attributes to search for
   * @return	an enumeration of <tt>SearchResult</tt> objects
   * @throws	NamingException if a naming exception is encountered
   */
  public NamingEnumeration search(String name, Attributes matchingAttrs)
    throws NamingException
  {
    return search(new CompositeName(name), matchingAttrs);
  }
  

  /**
   * Searches in a single context for objects that contain a
   * specified set of attributes.
   * This method returns all the attributes of such objects.
   * It is equivalent to supplying null as
   * the <tt>atributesToReturn</tt> parameter to the method
   * <code>search(Name, Attributes, String[])</code>.
   * <br>
   * See {@link #search(Name, Attributes, String[])} for a full description.
   *
   * @param name
   *		the name of the context to search
   * @param matchingAttributes
   *		the attributes to search for
   * @return	an enumeration of <tt>SearchResult</tt> objects
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #search(Name, Attributes, String[])
   */
  public NamingEnumeration search(String name, 
                                  Attributes matchingAttrs,
                                  String[] attrsRet)
    throws NamingException
  {
    return search(new CompositeName(name), matchingAttrs, attrsRet);
  }

  /**
   * Searches in the named context or object for entries that satisfy the
   * given search filter.  Performs the search as specified by
   * the search controls.
   * See {@link #search(Name, String, SearchControls)} for details.
   *
   * @param name
   *		the name of the context or object to search
   * @param filter
   *		the filter expression to use for the search; may not be null
   * @param cons
   *		the search controls that control the search.  If null,
   *		the default search controls are used (equivalent
   *		to <tt>(new SearchControls())</tt>).
   *
   * @return	an enumeration of <tt>SearchResult</tt>s for
   * 		the objects that satisfy the filter.
   * @throws	InvalidSearchFilterException if the search filter specified is
   *		not supported or understood by the underlying directory
   * @throws	InvalidSearchControlsException if the search controls
   * 		contain invalid settings
   * @throws	NamingException if a naming exception is encountered
   */
  public NamingEnumeration search(String name,
                                  String filter,
                                  SearchControls cons)
    throws NamingException
  {
    return search(new CompositeName(name), filterParser.parse(filter), cons);
  }
  
  /**
   * Searches in the named context or object for entries that satisfy the
   * given search filter.  Performs the search as specified by
   * the search controls.
   * See {@link #search(Name, String, Object[], SearchControls)} for details.
   *
   * @param name
   *		the name of the context or object to search
   * @param filterExpr
   *		the filter expression to use for the search.
   *		The expression may contain variables of the
   *		form "<code>{i}</code>" where <code>i</code>
   *		is a nonnegative integer.  May not be null.
   * @param filterArgs
   *		the array of arguments to substitute for the variables
   *		in <code>filterExpr</code>.  The value of
   *		<code>filterArgs[i]</code> will replace each
   *		occurrence of "<code>{i}</code>".
   *		If null, equivalent to an empty array.
   * @param cons
   *		the search controls that control the search.  If null,
   *		the default search controls are used (equivalent
   *		to <tt>(new SearchControls())</tt>).
   * @return	an enumeration of <tt>SearchResult</tt>s of the objects
   *		that satisy the filter; never null
   *
   * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
   *		<code>{i}</code> expressions where <code>i</code> is outside
   *		the bounds of the array <code>filterArgs</code>
   * @throws	InvalidSearchControlsException if <tt>cons</tt> contains
   *		invalid settings
   * @throws	InvalidSearchFilterException if <tt>filterExpr</tt> with
   *		<tt>filterArgs</tt> represents an invalid search filter
   * @throws	NamingException if a naming exception is encountered
   */
  public NamingEnumeration search(String name,
                                  String filterExpr,
                                  Object[] filterArgs,
                                  SearchControls cons)
    throws NamingException
  {
    return search(new CompositeName(name),
                  filterExpr,
                  filterArgs,
                  cons);
  }

  /**
   * Searches in a single context for objects that contain a
   * specified set of attributes.
   * This method returns all the attributes of such objects.
   * It is equivalent to supplying null as
   * the <tt>atributesToReturn</tt> parameter to the method
   * <code>search(Name, Attributes, String[])</code>.
   * <br>
   * See {@link #search(Name, Attributes, String[])} for a full description.
   *
   * @param name
   *		the name of the context to search
   * @param matchingAttributes
   *		the attributes to search for
   * @return	an enumeration of <tt>SearchResult</tt> objects
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #search(Name, Attributes, String[])
   */
  public NamingEnumeration search(Name name, Attributes matchingAttrs) 
    throws NamingException
  {
    return search(name,
                  new FilterMatchingAttributes(matchingAttrs),
                  defaultSearchControls);
  }
  
  /**
   * Searches in a single context for objects that contain a
   * specified set of attributes, and retrieves selected attributes.
   * The search is performed using the default
   * <code>SearchControls</code> settings.
   * <p>
   * For an object to be selected, each attribute in
   * <code>matchingAttributes</code> must match some attribute of the
   * object.  If <code>matchingAttributes</code> is empty or
   * null, all objects in the target context are returned.
   *<p>
   * An attribute <em>A</em><sub>1</sub> in
   * <code>matchingAttributes</code> is considered to match an
   * attribute <em>A</em><sub>2</sub> of an object if
   * <em>A</em><sub>1</sub> and <em>A</em><sub>2</sub> have the same
   * identifier, and each value of <em>A</em><sub>1</sub> is equal
   * to some value of <em>A</em><sub>2</sub>.  This implies that the
   * order of values is not significant, and that
   * <em>A</em><sub>2</sub> may contain "extra" values not found in
   * <em>A</em><sub>1</sub> without affecting the comparison.  It
   * also implies that if <em>A</em><sub>1</sub> has no values, then
   * testing for a match is equivalent to testing for the presence
   * of an attribute <em>A</em><sub>2</sub> with the same
   * identifier.
   * <p>
   * When changes are made to this <tt>DirContext</tt>,
   * the effect on enumerations returned by prior calls to this method
   * is undefined.
   *<p>
   * If the object does not have the attribute
   * specified, the directory will ignore the nonexistent attribute
   * and return the requested attributes that the object does have.
   *<p>
   * A directory might return more attributes than was requested
   * (see <strong>Attribute Type Names</strong> in the class description),
   * but is not allowed to return arbitrary, unrelated attributes.
   *<p>
   * See also <strong>Operational Attributes</strong> in the class
   * description.
   *
   * @param name
   *		the name of the context to search
   * @param matchingAttributes
   *		the attributes to search for.  If empty or null,
   *		all objects in the target context are returned.
   * @param attributesToReturn
   *		the attributes to return.  null indicates that
   *		all attributes are to be returned;
   *		an empty array indicates that none are to be returned.
   * @return
   *		a non-null enumeration of <tt>SearchResult</tt> objects.
   *		Each <tt>SearchResult</tt> contains the attributes
   *		identified by <code>attributesToReturn</code>
   *		and the name of the corresponding object, named relative
   * 		to the context named by <code>name</code>.
   * @throws	NamingException if a naming exception is encountered
   *
   * @see SearchControls
   * @see SearchResult
   * @see #search(Name, String, Object[], SearchControls)
   */
  public NamingEnumeration search(Name name,
                                  Attributes matchingAttrs,
                                  String[] attrsRet)
    throws NamingException
  {
    SearchControls cons = new SearchControls();
    cons.setReturningAttributes(attrsRet);
    return search(name, new FilterMatchingAttributes(matchingAttrs), cons);
  }

  /**
   * Searches in the named context or object for entries that satisfy the
   * given search filter.  Performs the search as specified by
   * the search controls.
   *<p>
   * The interpretation of <code>filterExpr</code> is based on RFC
   * 2254.  It may additionally contain variables of the form
   * <code>{i}</code> -- where <code>i</code> is an integer -- that
   * refer to objects in the <code>filterArgs</code> array.  The
   * interpretation of <code>filterExpr</code> is otherwise
   * identical to that of the <code>filter</code> parameter of the
   * method <code>search(Name, String, SearchControls)</code>.
   *<p>
   * When a variable <code>{i}</code> appears in a search filter, it
   * indicates that the filter argument <code>filterArgs[i]</code>
   * is to be used in that place.  Such variables may be used
   * wherever an <em>attr</em>, <em>value</em>, or
   * <em>matchingrule</em> production appears in the filter grammar
   * of RFC 2254, section 4.  When a string-valued filter argument
   * is substituted for a variable, the filter is interpreted as if
   * the string were given in place of the variable, with any
   * characters having special significance within filters (such as
   * <code>'*'</code>) having been escaped according to the rules of
   * RFC 2254.
   *<p>
   * This method returns an enumeration of the results.
   * Each element in the enumeration contains the name of the object
   * and other information about the object (see <code>SearchResult</code>).
   * The name is either relative to the target context of the search
   * (which is named by the <code>name</code> parameter), or
   * it is a URL string. If the target context is included in
   * the enumeration (as is possible when
   * <code>cons</code> specifies a search scope of
   * <code>SearchControls.OBJECT_SCOPE</code> or
   * <code>SearchControls.SUBSTREE_SCOPE</code>),
   * its name is the empty string.
   *<p>
   * The <tt>SearchResult</tt> may also contain attributes of the matching
   * object if the <tt>cons</tt> argument specifies that attributes be
   * returned.
   *<p>
   * If the object does not have a requested attribute, that
   * nonexistent attribute will be ignored.  Those requested
   * attributes that the object does have will be returned.
   *<p>
   * A directory might return more attributes than were requested
   * (see <strong>Attribute Type Names</strong> in the class description)
   * but is not allowed to return arbitrary, unrelated attributes.
   *<p>
   * If a search filter with invalid variable substitutions is provided
   * to this method, the result is undefined.
   * When changes are made to this DirContext,
   * the effect on enumerations returned by prior calls to this method
   * is undefined.
   *<p>
   * See also <strong>Operational Attributes</strong> in the class
   * description.
   *
   * @param name
   *		the name of the context or object to search
   * @param filterExpr
   *		the filter expression to use for the search.
   *		The expression may contain variables of the
   *		form "<code>{i}</code>" where <code>i</code>
   *		is a nonnegative integer.  May not be null.
   * @param filterArgs
   *		the array of arguments to substitute for the variables
   *		in <code>filterExpr</code>.  The value of
   *		<code>filterArgs[i]</code> will replace each
   *		occurrence of "<code>{i}</code>".
   *		If null, equivalent to an empty array.
   * @param cons
   *		the search controls that control the search.  If null,
   *		the default search controls are used (equivalent
   *		to <tt>(new SearchControls())</tt>).
   * @return	an enumeration of <tt>SearchResult</tt>s of the objects
   *		that satisy the filter; never null
   *
   * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
   *		<code>{i}</code> expressions where <code>i</code> is outside
   *		the bounds of the array <code>filterArgs</code>
   * @throws	InvalidSearchControlsException if <tt>cons</tt> contains
   *		invalid settings
   * @throws	InvalidSearchFilterException if <tt>filterExpr</tt> with
   *		<tt>filterArgs</tt> represents an invalid search filter
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #search(Name, Attributes, String[])
   * @see java.text.MessageFormat
   */
  public NamingEnumeration search(Name name,
                                  String filterExpr,
                                  Object[] filterArgs,
                                  SearchControls cons)
    throws NamingException
  {
    // Fill in expression
    String filter = format(filterExpr, filterArgs);
    
    return search(name, filter, cons);
  }

  /**
   * Searches in the named context or object for entries that satisfy the
   * given search filter.  Performs the search as specified by
   * the search controls.
   * <p>
   * The format and interpretation of <code>filter</code> follows RFC 2254
   * with the
   * following interpretations for <code>attr</code> and <code>value</code>
   * mentioned in the RFC.
   * <p>
   * <code>attr</code> is the attribute's identifier.
   * <p>
   * <code>value</code> is the string represention the attribute's value.
   *<p>
   * Any non-ASCII characters in the filter string should be
   * represented by the appropriate Java (Unicode) characters, and
   * not encoded as UTF-8 octets.  Alternately, the
   * "backslash-hexcode" notation described in RFC 2254 may be used.
   * <p>
   * RFC 2254 defines certain operators for the filter, including substring
   * matches, equality, approximate match, greater than, less than.  These
   * operators are mapped to operators with corresponding semantics in the
   * underlying directory. For example, for the equals operator, suppose
   * the directory has a matching rule defining "equality" of the
   * attributes in the filter. This rule would be used for checking
   * equality of the attributes specified in the filter with the attributes
   * of objects in the directory. Similarly, if the directory has a
   * matching rule for ordering, this rule would be used for
   * making "greater than" and "less than" comparisons.
   *<p>
   * Not all of the operators defined in RFC 2254 are applicable to all
   * attributes.  When an operator is not applicable, the exception
   * <code>InvalidSearchFilterException</code> is thrown.
   * <p>
   * The result is returned in an enumeration of <tt>SearchResult</tt>s.
   * Each <tt>SearchResult</tt> contains the name of the object
   * and other information about the object (see SearchResult).
   * The name is either relative to the target context of the search
   * (which is named by the <code>name</code> parameter), or
   * it is a URL string. If the target context is included in
   * the enumeration (as is possible when
   * <code>cons</code> specifies a search scope of
   * <code>SearchControls.OBJECT_SCOPE</code> or
   * <code>SearchControls.SUBSTREE_SCOPE</code>), its name is the empty
   * string. The <tt>SearchResult</tt> may also contain attributes of the
   * matching object if the <tt>cons</tt> argument specified that attributes
   * be returned.
   *<p>
   * If the object does not have a requested attribute, that
   * nonexistent attribute will be ignored.  Those requested
   * attributes that the object does have will be returned.
   *<p>
   * A directory might return more attributes than were requested
   * (see <strong>Attribute Type Names</strong> in the class description)
   * but is not allowed to return arbitrary, unrelated attributes.
   *<p>
   * See also <strong>Operational Attributes</strong> in the class
   * description.
   *
   * @param name
   *		the name of the context or object to search
   * @param filter
   *		the filter expression to use for the search; may not be null
   * @param cons
   *		the search controls that control the search.  If null,
   *		the default search controls are used (equivalent
   *		to <tt>(new SearchControls())</tt>).
   * @return	an enumeration of <tt>SearchResult</tt>s of
   * 		the objects that satisfy the filter; never null
   *
   * @throws	InvalidSearchFilterException if the search filter specified is
   *		not supported or understood by the underlying directory
   * @throws	InvalidSearchControlsException if the search controls
   * 		contain invalid settings
   * @throws	NamingException if a naming exception is encountered
   *
   * @see #search(Name, String, Object[], SearchControls)
   * @see SearchControls
   * @see SearchResult
   */
  public NamingEnumeration search(Name name,
                                  String filter,
                                  SearchControls cons)
    throws NamingException
  {
    return search(name, filterParser.parse(filter), cons);
  }

  private NamingEnumeration search(Name name, Filter filter, SearchControls cons)
    throws NamingException
  {
    // Vector for storing answers
    Vector answer = new Vector();
    search(name, filter, cons, answer);
    return new WrapEnum(answer.elements());
  }

  /**
   * The meat of the searcher. All filters have been converted to a
   * single interface, a SearchControls has been created if necessary,
   * and name strings have been converted to Names. If the search
   * scope is one of the two possibilities that includes the named
   * object in the result, then the attributes of the named object are
   * retrieved and tested. Then, it the search scope is one of the two
   * that requires enumerating the names in a context, then that is
   * handled.
   **/
  private void search(Name name, Filter filter, SearchControls cons, Vector answer)
    throws NamingException
  {
    Attributes attrs;
    switch (cons.getSearchScope()) {
    case (SearchControls.OBJECT_SCOPE):
    case (SearchControls.SUBTREE_SCOPE):
      attrs = getAttributes(name);
      if (filter.match(attrs)) {
        Object boundValue = null;
        if (cons.getReturningObjFlag()) {
          boundValue = lookup(name);
        }
        answer.addElement(new SearchResult(name.toString(), boundValue,
                                           selectAttributes(attrs, cons)));
      }
      break;
    }
    boolean recurse = false;
    switch (cons.getSearchScope()) {
    case (SearchControls.SUBTREE_SCOPE):
      recurse = true;
      // fall thru
    case (SearchControls.ONELEVEL_SCOPE):
      Object obj = lookup(name); // Should be a namingdircontext
      if (obj instanceof NamingDirContext) {
        NamingDirContext ndc = (NamingDirContext) obj;
        ndc.search(name, filter, cons, answer, recurse);
      } else {
        throw new NamingException("Not a NamingDirContext");
      }
      break;
    }
  }

  /**
   * Search the contents of a context for matches, recurse if specified.
   **/
  private void search(Name prefix,
                      Filter filter,
                      SearchControls cons,
                      Vector answer,
                      boolean recurse)
    throws NamingException
  {
    NamingEnumeration enum = list("");
    // For each item on list, examine its attributes
    while (enum.hasMore()) {
      NameClassPair item = (NameClassPair) enum.next();
      Attributes attrs = getAttributes(item.getName());
      Name name = (Name) prefix.clone(); // Prepare to construct the name
      name.add(item.getName()); // The name of this object relative to original ctx
      if (filter.match(attrs)) {
        Object boundValue = null;
        if (cons.getReturningObjFlag()) {
          boundValue = lookup(item.getName());
        }
        answer.addElement(new SearchResult(name.toString(), boundValue,
                                           selectAttributes(attrs, cons)));
      }
      if (recurse && item.getClassName().equals(getClass().getName())) {
        NamingDirContext subContext = (NamingDirContext) lookup(item.getName());
        subContext.search(name, filter, cons, answer, recurse);
        subContext.close();
      }
    }
  }

  /** 
   * Returns true if superset contains subset.
   */
  private static boolean contains(Attributes superset, Attributes subset) 
    throws NamingException {
    if ((subset == null) && (subset.size() == 0)) 
      return true;  // an empty set is always a subset
    
    NamingEnumeration m = subset.getAll();
    while (m.hasMore()) {
      if (superset == null) {
        return false;  // contains nothing
      }
      Attribute target = (Attribute) m.next();
      Attribute fromSuper = superset.get(target.getID());
      if (fromSuper == null) {
        return false;
      } else {
        // check whether attribute values match
        if (target.size() > 0) {
          NamingEnumeration vals = target.getAll();
          while (vals.hasMore()) {
            if (!fromSuper.contains(vals.next())) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }
  
  private class FilterMatchingAttributes implements Filter {
    private Attributes matchingAttrs;

    FilterMatchingAttributes(Attributes matchingAttrs) {
      this.matchingAttrs = matchingAttrs;
    }

    public boolean match(Attributes attrs) throws NamingException {
      return contains(attrs, matchingAttrs);
    }

    public void toString(StringBuffer b) {
      b.append("(&");
      try {
        for (NamingEnumeration enum = matchingAttrs.getAll(); enum.hasMore(); ) {
          Attribute attr = (Attribute) enum.next();
          b.append('(');
          b.append(attr.getID());
          b.append('=');
          b.append(attr.get());
          b.append(')');
        }
      } catch (NamingException ne) {
        ne.printStackTrace();   // Really don't expect this.
      }
      b.append(')');
    }
  }
  
  
  /*
   * Returns an Attributes instance containing only attributeIDs given in
   * "attributeIDs" whose values come from the given DSContext.
   */
  private static Attributes selectAttributes(Attributes originals,
                                             SearchControls cons)
    throws NamingException
{
  String[] attrIDs = cons.getReturningAttributes();
    if (attrIDs == null)
      return originals;
    
    Attributes result = new BasicAttributes();
    
    for(int i=0; i<attrIDs.length; i++) {
      Attribute attr = originals.get(attrIDs[i]);
      if(attr != null) {
        result.put(attr);
      }
    }
    
    return result;
  }

  class WrapEnum implements NamingEnumeration {
    Enumeration enum;
    
    WrapEnum(Enumeration enum) {
      this.enum = enum;
    }
    
    public boolean hasMore() throws NamingException {
      return hasMoreElements();
    }
    
    public boolean hasMoreElements() {
      return enum.hasMoreElements();
    }
    
    public Object next() throws NamingException {
      return nextElement();
    }
    
    public Object nextElement() {
      return enum.nextElement();
    }
    
    public void close() throws NamingException {
      enum = null;
    }
  }
  
  // Utility for turning a filter expression with arguments into a filter string
  
  /**
   * Formats the expression <tt>expr</tt> using arguments from the array
   * <tt>args</tt>.
   *
   * <code>{i}</code> specifies the <code>i</code>'th element from 
   * the array <code>args</code> is to be substituted for the
   * string "<code>{i}</code>".
   *
   * To escape '{' or '}' (or any other character), use '\'.
   *
   * Uses getEncodedStringRep() to do encoding.
   */
  
  private static String format(String expr, Object[] args) 
    throws NamingException {
    
    int param;
    int where = 0, start = 0;
    StringBuffer answer = new StringBuffer(expr.length());
    
    while ((where = findUnescaped('{', expr, start)) >= 0) {
      int pstart = where + 1; // skip '{'
      int pend = expr.indexOf('}', pstart);
      
      if (pend < 0) {
        throw new InvalidSearchFilterException("unbalanced {: " + expr);
      }
      
      // at this point, pend should be pointing at '}'
      try {
        param = Integer.parseInt(expr.substring(pstart, pend));
      } catch (NumberFormatException e) {
        throw new InvalidSearchFilterException(
                                               "integer expected inside {}: " + expr);
      }
      
      if (param >= args.length) {
        throw new InvalidSearchFilterException(
                                               "number exceeds argument list: " + param);
      }
      
      answer.append(expr.substring(start, where)).append(
                                                         getEncodedStringRep(args[param]));
      start = pend + 1; // skip '}'
    }
    
    if (start < expr.length())
      answer.append(expr.substring(start));
    
    return answer.toString();
  }
  
  /**
   * Finds the first occurrence of <tt>ch</tt> in <tt>val</tt> starting
   * from position <tt>start</tt>. It doesn't count if <tt>ch</tt>
   * has been escaped by a backslash (\)
   */
  private static int findUnescaped(char ch, String val, int start) {
    int len = val.length();
    
    while (start < len) {
      int where = val.indexOf(ch, start);
      // if at start of string, or not there at all, or if not escaped
      if (where == start || where == -1 || val.charAt(where-1) != '\\')
        return where;
      
      // start search after escaped star
      start = where + 1;
    }
    return -1;
  }
  
  
  // Writes the hex representation of a byte to a StringBuffer.
  private static void hexDigit(StringBuffer buf, byte x) {
    char c;
    
    c = (char) ((x >> 4) & 0xf);
    if (c > 9)
      c = (char) ((c-10) + 'A');
    else
      c = (char)(c + '0');
    
    buf.append(c);
    c = (char) (x & 0xf);
    if (c > 9)
      c = (char)((c-10) + 'A');
    else
      c = (char)(c + '0');
    buf.append(c);
  }
  
  
  /**
   * Returns the string representation of an object (such as an attr value).
   * If obj is a byte array, encode each item as \xx, where xx is hex encoding
   * of the byte value.
   * Else, if obj is not a String, use its string representation (toString()).
   * Special characters in obj (or its string representation) are then 
   * encoded appropriately according to RFC 2254.
   * 	*   	\2a
   *		(	\28
   *		)	\29
   *		\	\5c
   *		NUL	\00
   */
  private static String getEncodedStringRep(Object obj) throws NamingException {
    String str;
    if (obj == null)
      return null;
    
    if (obj instanceof byte[]) {
      // binary data must be encoded as \hh where hh is a hex char
      byte[] bytes = (byte[])obj;
      StringBuffer b1 = new StringBuffer(bytes.length*3);
      for (int i = 0; i < bytes.length; i++) {
        b1.append('\\');
        hexDigit(b1, bytes[i]);
      }
      return b1.toString();
    }
    if (!(obj instanceof String)) {
      str = obj.toString();
    } else {
      str = (String)obj;
    }
    int len = str.length();
    StringBuffer buf = new StringBuffer(len);
    char ch;
    for (int i = 0; i < len; i++) {
      ch=str.charAt(i);         // avoid jikes bug with assigniments in switch
      switch (ch) {
      case '*': 
        buf.append("\\2a");
        break;
      case '(':
        buf.append("\\28");
        break;
      case ')':
        buf.append("\\29");
        break;
      case '\\':
        buf.append("\\5c");
        break;
      case 0:
        buf.append("\\00");
        break;
      default:
        buf.append(ch);
      }
    }
    return buf.toString();
  }
  
  
  private Attributes deepClone(Attributes orig) throws NamingException {
    if (orig.size() == 0) {
      return (Attributes)orig.clone();
    }
    
    BasicAttributes copy = new BasicAttributes();
    
    NamingEnumeration enum = orig.getAll();
    while (enum.hasMore()) {
      copy.put((Attribute)((Attribute)enum.next()).clone());
    }
    
    return copy;
  }

  // Presumes orig is a collection of Attributes
  private Collection deepClone(Collection orig) throws NamingException {
    try {
      Collection clone = (Collection) orig.getClass().newInstance();

      if (orig.size() == 0) {
        return clone;
      }
      
      for (Iterator iterator = orig.iterator(); iterator.hasNext();) {
        Attribute attribute = (Attribute) iterator.next();
        clone.add(attribute.clone());
      }
      
      return clone;
    } catch (InstantiationException ie) {
      ie.printStackTrace();
      return null;
    } catch (IllegalAccessException iae) {
      iae.printStackTrace();
      return null;
    }
  }

  private Hashtable deepClone(Hashtable orig) throws NamingException {
    if (orig.size() == 0) {
      return (Hashtable)orig.clone();
    }
    
    Hashtable copy = new Hashtable();
    
    Enumeration enum = orig.keys();
    while (enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      Attributes attrs = (Attributes) orig.get(key);
      
      copy.put(key, deepClone(attrs));
    }
    
    return copy;
  }

  private ModificationItem[] deepClone(ModificationItem []orig) throws NamingException {

    ModificationItem []clone = new ModificationItem[orig.length];

    for (int index = 0; index < orig.length; index++) {
      clone[index] = 
        new ModificationItem(orig[index].getModificationOp(),
                             (Attribute) orig[index].getAttribute().clone());
    }

    return clone;
  }

  protected Attributes matchingAttributes(Collection attributes, String []attrIds) {
    Attributes matching = new BasicAttributes();

    if (attributes == null) {
      return matching;
    }

    for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
      Attribute attribute = (Attribute) iterator.next();
      
      if (attrIds == null) {
        matching.put((Attribute) attribute.clone());
      } else {
        for (int index = 0; index < attrIds.length; index++) {
          if (attribute.getID().equals(attrIds[index])) {
            matching.put((Attribute) attribute.clone());
            break;
          }
        }
      }
    }

    return matching;
  }

  
  /**
   * Apply modifications to attrs in place. 
   * NOTE: Simplified implementation:
   *       - Modifications NOT performed atomically
   * 	   - Attribute names case-sensitive 
   *       - All attributes can be multivalued
   */
  private Collection doMods(Collection attrs, ModificationItem[] mods) 
    throws NamingException {
    Attribute attr;
    String attrId;

    if (attrs == null) {
      attrs = new ArrayList();
    }

    
   for (int i = 0; i < mods.length; i++) {
      attr = mods[i].getAttribute();
      attrId = attr.getID();
      Attribute origAttr = findAttribute(attrs, attrId);

      switch (mods[i].getModificationOp()) {
      case ADD_ATTRIBUTE: {
        if (origAttr == null) {
          // No previous attribute, just add
          attrs.add(attr);
        } else {
          // Append values of new attribute
          NamingEnumeration newVals = attr.getAll();
          Object val;
          while (newVals.hasMore()) {
            val = newVals.next();
            if (!origAttr.contains(val)) {
              origAttr.add(val);
            }
          }
        }
        break;
      }
      
      case REPLACE_ATTRIBUTE:
        if (origAttr != null) {
          attrs.remove(origAttr);
        }

        if (attr.size() != 0) {
          attrs.add(attr);
        }
        break;
        
      case REMOVE_ATTRIBUTE:
        if (origAttr == null) {
          break;
        }

        if (attr.size() == 0) {
          // Remove entire attribute
          attrs.remove(origAttr);
        } else {
          // Remove specified values
          NamingEnumeration remVals = attr.getAll();
          while (remVals.hasMore()) {
            origAttr.remove(remVals.next());
          }
          if (origAttr.size() == 0) {
            attrs.remove(origAttr);
          }
        }
        break;
      }
    }

   return attrs;
  }

  protected Attribute findAttribute(Collection attributes, String attrId) {
    for (Iterator iterator = attributes.iterator();
         iterator.hasNext();) {
      Attribute attribute = (Attribute) iterator.next();
      if (attribute.getID().equals(attrId)) {
        return attribute;
      }
    }

    return null;
  }

  // Class for enumerating bindings
  protected class ListOfDirBindings extends ListOfBindings {
    
    ListOfDirBindings(Iterator entries) {
      super(entries);
    }
  }  
}
