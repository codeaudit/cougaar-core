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
import javax.naming.directory.*;
import javax.naming.spi.*;
import java.util.*;

import org.cougaar.core.society.NameServer;

public class NamingDirContext extends NamingContext implements DirContext {
  protected final static NameParser myParser = new NamingParser();
  
  protected NamingDirContext(NS ns, NameServer.Directory directory, Hashtable inEnv) {
    super(ns, directory, inEnv);

    myClassName = NamingDirContext.class.getName();
  }

  protected Context cloneContext() {
    return new NamingDirContext(myNS, myDirectory, myEnv);
  }

  protected Context createContext(NameServer.Directory dir) {
    return new NamingDirContext(myNS, dir, myEnv);
  }


  public Attributes getAttributes(String name) throws NamingException {
    return getAttributes(new CompositeName(name));
  }
  
  public Attributes getAttributes(Name name) throws NamingException {
    return getAttributes(name, null);  // same as attrIds == null
  }

  public Attributes getAttributes(String name, String[] attrIds)
    throws NamingException {
    return getAttributes(new CompositeName(name), attrIds);
  }
  
  public Attributes getAttributes(Name name, String[] attrIds) 
    throws NamingException {
    if (name.isEmpty()) {
      // Asking for attributes of this context.
      Collection attributes;
      try {
        attributes = getNS().getAttributes(getDirectory(), "");
      } catch (RemoteException re) {
        re.printStackTrace();
        attributes = new ArrayList();
      }

      return deepClone(matchingAttributes(attributes, attrIds));
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObject = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      if (nsObject == null) {
        throw new NameNotFoundException(name + " not found");
      }
      
      Collection attributes = null;
      try {
        attributes = getNS().getAttributes(getDirectory(), atom);
      } catch (RemoteException re) {
        re.printStackTrace();
      }

      // Atomic name: Find object in internal data structure
      if (attributes == null) {
        return new BasicAttributes();
      } else {
        return deepClone(matchingAttributes(attributes, attrIds));
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom +
                                      " does not name a DirContext");
      }
      
      return ((DirContext) nsObject).getAttributes(nm.getSuffix(1), attrIds);
    }
  }
  
  public void modifyAttributes(String name, int mod_op, Attributes attrs)
	throws NamingException {
    modifyAttributes(new CompositeName(name), mod_op, attrs);
  }
  
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
  
  public void modifyAttributes(String name, ModificationItem[] mods)
    throws NamingException {
    modifyAttributes(new CompositeName(name), mods);
  }
  
  public void modifyAttributes(Name name, ModificationItem[] mods)
    throws NamingException {
    if (name.isEmpty()) {
      // Updating attributes of this context.
      try {
        //Clone Attributes and ModificationItems so that parameters aren't 
        // modified by processing
        Collection cloneAttrs = 
          deepClone(getNS().getAttributes(getDirectory(), ""));
        ModificationItem[] cloneMods = deepClone(mods);

        getNS().putAttributes(getDirectory(), "", 
                              doMods(cloneAttrs, cloneMods));
      } catch (RemoteException re) {
        re.printStackTrace();
      }
      return;
    }
    
    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObject = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      if (nsObject == null) {
        throw new NameNotFoundException(name + " not found");
      }
      

      try {
        //Clone Attributes and ModificationItems so that parameters aren't 
        // modified by processing
        Collection cloneAttrs = 
          deepClone(getNS().getAttributes(getDirectory(), atom));
        ModificationItem[] cloneMods = deepClone(mods);

        getNS().putAttributes(getDirectory(), atom, 
                              doMods(cloneAttrs, cloneMods));
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom +
                                      " does not name a dircontext");
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
    Object nsObject = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObject == null) {
        NamingEnumeration bindings = listBindings("");
        while (bindings.hasMore()) {
          Binding binding = (Binding) bindings.next();
          
          System.out.println("\t" + binding.getName() + " " + binding.getObject());
        } 
        
        throw new NameNotFoundException(name + " not found");

      }
      
      // Get attributes
      Attributes attrs = getAttributes(name);
      
      // Call getObjectInstance for using any object factories
      try {
        return DirectoryManager.getObjectInstance(nsObject, new CompositeName().add(atom), 
                                                  this, myEnv, attrs);
      } catch (Exception e) {
        NamingException ne = new NamingException("getObjectInstance failed");
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
  
  public NamingEnumeration listBindings(Name name) throws NamingException {
    if (name.isEmpty()) {
      try {
        // listing this context
        return new ListOfDirBindings(getNS().entrySet(getDirectory()).iterator());
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

  // Override unbind() to deal with attributes
  // destroySubcontext() already uses unbind() so we just need to
  // override unbind() to affect both unbind() and destroySubcontext().
  public void unbind(Name name) throws NamingException {
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot unbind empty name");
    }

    // Sigh .... would like to use getNSObject but name may be in a sub context
    // so use lookup to make sure that we find the object.
    try {
      if (lookup(name) instanceof Context) {
        throw new OperationNotSupportedException("Use destroySubContext to remove a Context.");
      }
    } catch (NameNotFoundException nnfe) {
      // Object doesn't  so we can stop now 
      return;
    }

    try {
      // Get attributes that belong to name
      Attributes attrs = getAttributes(name);
      
      // Remove those attributes
      if (attrs.size() != 0) {
        modifyAttributes(name, DirContext.REMOVE_ATTRIBUTE, attrs);
      }
    } catch (NamingException e) {
      e.printStackTrace();
    }
    
    // Remove from namespace
    super.unbind(name);
  }
  
  // Override NamingContext version to account for attributes
  public void bind(Name name, Object obj) throws NamingException {
    bind(name, obj, new BasicAttributes());
  }
  
  public void bind(String name, Object obj, Attributes attrs)
    throws NamingException {
    bind(new CompositeName(name), obj, attrs);
  }
  
  /**
   * NOTE: Simplified implementation: add attributes and object nonatomically
   * Does not accept null obj (throws NullPointerException)
   */
  public void bind(Name name, Object obj, Attributes attrs)
    throws NamingException {
    System.out.println("Attempt to bind :" + name + " in " + myDirectory.getPath()); 
    if (name.isEmpty()) {
      throw new InvalidNameException("Cannot bind empty name");
    }
    
    if (obj instanceof Context) {
      throw new OperationNotSupportedException("Use createSubContext to create a Context.");
    }

    // Extract components that belong to this namespace
    Name nm = getMyComponents(name);
    String atom = nm.get(0);
    Object nsObject = getNSObject(getDirectory(), atom);

    if (nm.size() == 1) {
      // Atomic name: Find object in internal data structure
      if (nsObject != null) {
        throw new NameAlreadyBoundException(name + 
                                            " already exists. Use rebind to override");
      }
      
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
      try {
        getNS().put(getDirectory(), atom, res.getObject(), arrayList);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom + 
                                      " does not name a DirContext");
      }
      ((DirContext) nsObject).bind(nm.getSuffix(1), obj, attrs);
    }
  }
  
  // Override NamingContext version to account for attributes
  public void rebind(Name name, Object obj) throws NamingException {
    rebind(name, obj, new BasicAttributes());
  }
  
  public void rebind(String name, Object obj, Attributes attrs)
    throws NamingException {
    rebind(new CompositeName(name), obj, attrs);
  }
  
  /**
   * NOTE: Simplified implementation: remove object first, then add back
   * Does not accept null obj (throws NullPointerException)
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
      // Atomic name
      
      if (getNSObject(getDirectory(), atom) instanceof Context) {
        throw new OperationNotSupportedException("Can not use rebind to replace a Context.");
      }

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
      try {
        getNS().put(getDirectory(), atom, res.getObject(), arrayList);
      } catch (RemoteException re) {
        re.printStackTrace();
      }
    } else {
      // Intermediate name: Consume name in this context and continue
      Object nsObject = getNSObject(getDirectory(), atom);
      if (!(nsObject instanceof DirContext)) {
        throw new NotContextException(atom + " does not name a DirContext.");
      }
      ((DirContext) nsObject).rebind(nm.getSuffix(1), obj, attrs);
    }
  }
  
  public DirContext createSubcontext(String name, Attributes attrs)
    throws NamingException {
    return createSubcontext(new CompositeName(name), attrs);
  }
  
  /**
   * NOTE: Simplified implementation: add attributes and object nonatomically
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
  
  public DirContext getSchema(String name) throws NamingException {
    return getSchema(new CompositeName(name));
  }
  
  public DirContext getSchema(Name name) throws NamingException {
    throw new OperationNotSupportedException("Not implemented yet");
  }
  
  public DirContext getSchemaClassDefinition(String name)
    throws NamingException {
    return getSchemaClassDefinition(new CompositeName(name));
  }
  public DirContext getSchemaClassDefinition(Name name)
    throws NamingException {
    throw new OperationNotSupportedException("Not implemented yet");
  }
  
  public NamingEnumeration search(String name, Attributes matchingAttrs)
    throws NamingException {
    return search(new CompositeName(name), matchingAttrs);
  }
  
  public NamingEnumeration search(Name name, Attributes matchingAttrs) 
    throws NamingException {
    return search(name, matchingAttrs, null);
  }
  
  public NamingEnumeration search(String name, 
                                  Attributes matchingAttrs, String[] attrsRet)
    throws NamingException {
    return search(new CompositeName(name), matchingAttrs, attrsRet);
  }
  
  public NamingEnumeration search(Name name,
                                  Attributes matchingAttrs, String[] attrsRet)
    throws NamingException {
    
    // Vector for storing answers
    Vector answer = new Vector();
    Binding item;
    
    // Get context
    DirContext target = (DirContext)lookup(name);
    
    try {
      // List context
      NamingEnumeration objs = target.listBindings("");
      Attributes attrs;
      
      // For each item on list, examine its attributes
      while (objs.hasMore()) {
        item = (Binding)objs.next();
        if (item.getObject() instanceof DirContext) {
          attrs = ((DirContext)item.getObject()).getAttributes("");
        } else {
          attrs = target.getAttributes(item.getName());
        }
        if (contains(attrs, matchingAttrs)) {
          answer.addElement(new SearchResult(item.getName(), null,
                                             selectAttributes(attrs, attrsRet)));
        }
      } 
    } finally {
      target.close();
    }
    
    return new WrapEnum(answer.elements());
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
  
  
  /*
   * Returns an Attributes instance containing only attributeIDs given in
   * "attributeIDs" whose values come from the given DSContext.
   */
  private static Attributes selectAttributes(Attributes originals,
                                             String[] attrIDs) throws NamingException {
    
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
  
  public NamingEnumeration search(String name,
                                  String filter, SearchControls cons) throws NamingException {
    return search(new CompositeName(name), filter, cons);
  }
  
  public NamingEnumeration search(Name name,
                                  String filter, SearchControls cons) throws NamingException {
    throw new OperationNotSupportedException(
                                             "Filter search not supported");
  }
  
  public NamingEnumeration search(String name,
                                  String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {
    return search(new CompositeName(name), filterExpr, filterArgs, cons);
  }
  
  public NamingEnumeration search(Name name,
                                  String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {
    // Fill in expression
    String filter = format(filterExpr, filterArgs);
    
    System.out.println("filter string: " + filter);
    return search(name, filter, cons);
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
      switch (ch=str.charAt(i)) {
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

  public static void main(String[] args) {
    RMINameServer.create();
    RMINameServer rns = new RMINameServer();

    try {
      NS ns = new NSImpl();
      DirContext ctx = new NamingDirContext(ns, NSImpl.ROOT, new Hashtable());
      Attributes attributes = new BasicAttributes("fact", "the letter A");
      Attribute chocolateAttribute = new BasicAttribute("chocolate", "Merckens");

      DirContext a = ctx.createSubcontext("a", attributes);
      attributes.get("fact").add("the letter B");
      attributes.put(chocolateAttribute);
      attributes.get("fact").remove("the letter A");
      DirContext b = ctx.createSubcontext("b", attributes);
      attributes.get("chocolate").add("ScharffenBerger");
      attributes.get("fact").add("the letter C");
      DirContext c = b.createSubcontext("c", attributes);

      System.out.println("c's full name: " + c.getNameInNamespace());
      
      System.out.println("list: " );
      NamingEnumeration enum = ctx.list("");
      while (enum.hasMore()) {
        System.out.println("\t" + enum.next());
      }

      BasicAttributes searchAttributes = new BasicAttributes();
      
      Attribute searchAttribute = new BasicAttribute("fact", "the letter B");
      searchAttributes.put(searchAttribute);
      System.out.println("search initial context for: " + searchAttribute);
      enum = ctx.search("", searchAttributes);
      while (enum.hasMore()) {
        System.out.println(enum.next());
      }

      String  []returnAttrIDs = {"chocolate", "fact"};
      System.out.println("search b's context for: " + searchAttribute + 
                         " return attributes: " + returnAttrIDs[0] + " " +
                         returnAttrIDs[1]);
      enum = b.search("", searchAttributes, returnAttrIDs);
      while (enum.hasMore()) {
        System.out.println(enum.next());
      }

      
    c.bind("foo", "tex", new BasicAttributes("fact", "fooAttr"));
    c.bind("boo", "hex", new BasicAttributes("fact", "booAttr"));

    System.out.println("Contents of c");
    NamingEnumeration bindings = c.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println("\t" + binding.getName() + " " + binding.getObject());

      enum = c.getAttributes(binding.getName()).getAll();
      while (enum.hasMore()) {
        System.out.println("\t\t attribute:" + enum.next());
      }
    } 

     
     
    c.rebind("foo", "mex", new BasicAttributes("fact", "fiction"));
    System.out.println("Contents of c after rebinding foo");
    bindings = c.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println("\t" + binding.getName() + " " + binding.getObject());

      enum = c.getAttributes(binding.getName()).getAll();
      while (enum.hasMore()) {
        System.out.println("\t\t attribute:" + enum.next());
      }
    } 
    System.out.println("");

    c.modifyAttributes("foo", ADD_ATTRIBUTE, attributes);
    System.out.println("After modifying foo's attributes");
    enum = c.getAttributes("foo").getAll();
    while (enum.hasMore()) {
      System.out.println("\t\t attribute:" + enum.next());
    } 
    System.out.println("");

    ModificationItem mods[] = new ModificationItem[3];
    mods[0] = new ModificationItem(ADD_ATTRIBUTE, 
                                   new BasicAttribute("addAttribute", 
                                                      "tigger"));
    Attribute replaceAttribute = attributes.get("fact");
    replaceAttribute.add("the letter FOO");
    mods[1] = new ModificationItem(REPLACE_ATTRIBUTE, 
                                   replaceAttribute);

    mods[2] = new ModificationItem(REMOVE_ATTRIBUTE, 
                                   attributes.get("chocolate"));
    
    c.modifyAttributes("foo", mods);
    System.out.println("After modifying foo's attributes");
    enum = c.getAttributes("foo").getAll();
    while (enum.hasMore()) {
      System.out.println("\t\t attribute:" + enum.next());
    } 
    System.out.println("");


    c.rename("foo", "roo");
    System.out.println("Contents of c after renaming foo to roo");
    bindings = c.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println("\t" + binding.getName() + " " + binding.getObject());

      enum = c.getAttributes(binding.getName()).getAll();
      while (enum.hasMore()) {
        System.out.println("\t\t attribute:" + enum.next());
      }
    } 
    System.out.println("");

    c.unbind("roo");
    System.out.println("Contents of c after unbinding roo");
    bindings = c.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println("\t" + binding.getName() + " " + binding.getObject());

      enum = c.getAttributes(binding.getName()).getAll();
      while (enum.hasMore()) {
        System.out.println("\t\t attribute:" + enum.next());
      }
    } 
    System.out.println("");


    System.out.println("Contents of initial context with associated 'fact' attribute");
    bindings = ctx.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println(binding.getName() + " " + binding.getObject());
      returnAttrIDs = new String[1];
      returnAttrIDs[0] = "fact";
      enum = ctx.getAttributes(binding.getName(), returnAttrIDs).getAll();
      while (enum.hasMore()) {
        System.out.println("\t attribute:" + enum.next());
      }
    } 

    // Boundary conditions -
    Object o1 = ctx.lookup("/b/c/boo");
    Object o2 = ctx.lookup("b/c/boo");
    boolean same = (o1 == o2);
    System.out.println("/b/c/boo == b/c/boo: " + same);

    System.out.println("Attempt to remove a Context");
    try {
      ctx.unbind("b");
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("Attempt to rename a Context"); 
    try {
      ctx.rename("b", "d");
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    System.out.println("Contents of initial context with associated 'fact' attribute");
    bindings = ctx.listBindings("");
    while (bindings.hasMore()) {
      Binding binding = (Binding) bindings.next();

      System.out.println(binding.getName() + " " + binding.getObject());
      returnAttrIDs = new String[1];
      returnAttrIDs[0] = "fact";
      enum = ctx.getAttributes(binding.getName(), returnAttrIDs).getAll();
      while (enum.hasMore()) {
        System.out.println("\t attribute:" + enum.next());
      }
    } 

    } catch (NamingException ne) {
      ne.printStackTrace();
    } catch (RemoteException re) {
      re.printStackTrace();
    }

    System.exit(0);
  }

}







