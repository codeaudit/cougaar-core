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

import java.io.IOException;
import java.io.PrintStream;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.util.Hashtable;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

/**
 * A JNDI DirContext that stores Cougaar objects in an LDAP directory.
 * It mostly just delegates to a real LDAP DirContext, but in some
 * cases, it has to process the arguments a little.  The main points are:
 * <pre>
 * String names are converted from /foo/bar to dc=bar, dc=foo (see LdapNameParser)
 * The DirStateFactory is called on the outside chance that someone wants to use it.
 * Remote (RMPI) objects are wrapped in a MarshalledObject so the whole RMI object 
 *   doesn't get serialized - just the stub.  See getObjectToBind() and getBoundObject())
 **/
public class LdapDirContext implements DirContext {
    private DirContext realDirContext;
    private Attribute objClasses;
    private NameParser nameParser;

    public LdapDirContext(DirContext realDirContext) {
        this.realDirContext = realDirContext;
        
        nameParser = new LdapNameParser();

        objClasses = new BasicAttribute("objectClass");
        objClasses.add("top");
        objClasses.add("dcObject");
        objClasses.add("organization");
        objClasses.add("inetOrgPerson");
     }

    public Object addToEnvironment(String str, Object obj) throws NamingException {
        return realDirContext.addToEnvironment(str, obj);
    }
    
    public void bind(String str, Object obj) throws NamingException {
        bind (nameParser.parse(str), obj);
    }
    
    public void bind(Name name, Object obj) throws NamingException {
        DirStateFactory.Result res = getObjectToBind(name, obj, null);
        realDirContext.bind(name, res.getObject());
    }
    
    public void bind(String str, Object obj, Attributes attributes) throws NamingException {
        bind (nameParser.parse(str), obj, attributes);
    }
    
    public void bind(Name name, Object obj, Attributes attributes) throws NamingException {
        DirStateFactory.Result res = getObjectToBind(name, obj, attributes);
        realDirContext.bind(name, res.getObject(), res.getAttributes());
    }
    
    protected Object clone() throws CloneNotSupportedException {
        Object retValue;
        
        retValue = super.clone();
        return retValue;
    }
    
    public void close() throws NamingException {
        realDirContext.close();
    }
    
    public String composeName(String str, String str1) throws NamingException {
        return str+"/"+str1;
    }
    
    public Name composeName(Name name, Name name1) throws NamingException {
        Name ret = new CompositeName();
        ret.addAll(name);
        ret.addAll(name1);
        return ret;
    }
    
    public Context createSubcontext(String str) throws NamingException {
        return createSubcontext(nameParser.parse(str));
    }
    
    public Context createSubcontext(Name name) throws NamingException {
        return wrap(realDirContext.createSubcontext(name));
    }
    
    public DirContext createSubcontext(String str, Attributes attributes) throws NamingException {
        return createSubcontext(nameParser.parse(str), attributes);
    }
    
    public DirContext createSubcontext(Name name, Attributes attributes) throws NamingException {
        if (attributes == null)
            attributes = new BasicAttributes();
        attributes.put(objClasses);
        return wrap(realDirContext.createSubcontext(name, attributes));
    }
    
    public void destroySubcontext(String str) throws NamingException {
        destroySubcontext(nameParser.parse(str));
    }
    
    public void destroySubcontext(Name name) throws NamingException {
        realDirContext.destroySubcontext(name);
    }
    
    public boolean equals(Object obj) {
        boolean retValue;
        
        retValue = super.equals(obj);
        return retValue;
    }
    
    protected void finalize() throws Throwable {
        super.finalize();
    }
    
    public Attributes getAttributes(String str) throws NamingException {
        return getAttributes(nameParser.parse(str));
    }
    
    public Attributes getAttributes(Name name) throws NamingException {
        return realDirContext.getAttributes(name);
    }
    
    public Attributes getAttributes(String str, String[] str1) throws NamingException {
        return getAttributes(nameParser.parse(str), str1);
    }
    
    public Attributes getAttributes(Name name, String[] str) throws NamingException {
        return realDirContext.getAttributes(name, str);
    }
    
    public java.util.Hashtable getEnvironment() throws NamingException {
        return realDirContext.getEnvironment();
    }
    
    public String getNameInNamespace() throws NamingException {
        return realDirContext.getNameInNamespace();
    }
    
    public NameParser getNameParser(String str) throws NamingException {
        return new LdapNameParser();
    }
    
    public NameParser getNameParser(Name name) throws NamingException {
        return new LdapNameParser();
    }
    
    public DirContext getSchema(String str) throws NamingException {
        return getSchema(nameParser.parse(str));
    }
    
    public DirContext getSchema(Name name) throws NamingException {
        return realDirContext.getSchema(name);
    }
    
    public DirContext getSchemaClassDefinition(String str) throws NamingException {
        return getSchemaClassDefinition(str);
    }
    
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return realDirContext.getSchemaClassDefinition(name);
    }
    
    public int hashCode() {
        int retValue;
        
        retValue = realDirContext.hashCode();
        return retValue;
    }
    
    public NamingEnumeration list(String str) throws NamingException {
        return list(nameParser.parse(str));
    }
    
    public NamingEnumeration list(Name name) throws NamingException {
        return realDirContext.list(name);
    }
    
    public NamingEnumeration listBindings(String str) throws NamingException {
        return listBindings(nameParser.parse(str));
    }
    
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return realDirContext.listBindings(name);
    }
    
    public Object lookup(String str) throws NamingException {
        return lookup(nameParser.parse(str));
    }
    
    public Object lookup(Name name) throws NamingException {
       Object ret = realDirContext.lookup(name);
       try {
         ret = getBoundObject(name, ret, null);
       } catch (Exception ex) {
           System.err.println("Error unwrapping in lookup()");
           ex.printStackTrace();
       }
       if (ret instanceof DirContext) {
          ret = wrap((DirContext)ret);
       }

        return ret;
    }
    
    public Object lookupLink(String str) throws NamingException {
        return lookupLink(nameParser.parse(str));
    }
    
    public Object lookupLink(Name name) throws NamingException {
       Object ret = realDirContext.lookupLink(name);
       try {
         ret = getBoundObject(name, ret, null);
       } catch (Exception ex) {
           System.err.println("Error unwrapping in lookupLink()");
           ex.printStackTrace();
       }
       
       if (ret instanceof DirContext) {
          ret = wrap((DirContext)ret);
       }

       return ret;
    }
    
    public void modifyAttributes(String str, ModificationItem[] modificationItem) throws NamingException {
        modifyAttributes(nameParser.parse(str), modificationItem);
    }
    
    public void modifyAttributes(Name name, ModificationItem[] modificationItem) throws NamingException {
        realDirContext.modifyAttributes(name, modificationItem);
    }
    
    public void modifyAttributes(String str, int param, Attributes attributes) throws NamingException {
        modifyAttributes(nameParser.parse(str), param, attributes);
    }
    
    public void modifyAttributes(Name name, int param, Attributes attributes) throws NamingException {
        realDirContext.modifyAttributes(name, param, attributes);
    }
    
    public void rebind(String str, Object obj) throws NamingException {
        rebind(nameParser.parse(str), obj);
    }
    
    public void rebind(Name name, Object obj) throws NamingException {
        DirStateFactory.Result res = getObjectToBind(name, obj, null);
        realDirContext.rebind(name, res.getObject());
    }
    
    public void rebind(String str, Object obj, Attributes attributes) throws NamingException {
        rebind(nameParser.parse(str), obj, attributes);
    }
    
    public void rebind(Name name, Object obj, Attributes attributes) throws NamingException {
        DirStateFactory.Result res = getObjectToBind(name, obj, attributes);
        realDirContext.rebind(name, res.getObject(), res.getAttributes());
    }
    
    public Object removeFromEnvironment(String str) throws NamingException {
        return realDirContext.removeFromEnvironment(str);
    }
    
    public void rename(String str, String str1) throws NamingException {
        rename(nameParser.parse(str), nameParser.parse(str1));
    }
    
    public void rename(Name name, Name name1) throws NamingException {
        realDirContext.rename(name, name1);
    }
    
    public NamingEnumeration search(String str, Attributes attributes) throws NamingException {
        return search(nameParser.parse(str), attributes);
    }
    
    public NamingEnumeration search(String str, String filter, SearchControls searchControls) throws NamingException {
        return search(nameParser.parse(str), filter, searchControls);
    }
    
    public NamingEnumeration search(String str, Attributes attributes, String[] str2) throws NamingException {
        return search(nameParser.parse(str), attributes, str2);
    }
    
    public NamingEnumeration search(String str, String str1, Object[] obj, SearchControls searchControls) throws NamingException {
        return search(nameParser.parse(str), str1, obj, searchControls);
    }
    
    public NamingEnumeration search(Name name, Attributes attributes) throws NamingException {
        return updateSearchResults(realDirContext.search(name, attributes));
    }
    
    public NamingEnumeration search(Name name, String str, SearchControls searchControls) throws NamingException {
        return updateSearchResults(realDirContext.search(name, str, searchControls));
    }
    
    public NamingEnumeration search(Name name, Attributes attributes, String[] str) throws NamingException {
        return updateSearchResults(realDirContext.search(name, attributes, str));
    }
    
    public NamingEnumeration search(Name name, String str, Object[] obj, SearchControls searchControls) throws NamingException {
        return updateSearchResults(realDirContext.search(name, str, obj, searchControls));
    }
    
    public String toString() {
        String retValue;
        
        retValue = realDirContext.toString();
        return retValue;
    }
    
    public void unbind(String str) throws NamingException {
        unbind(nameParser.parse(str));
    }
    
    public void unbind(Name name) throws NamingException {
        realDirContext.unbind(name);
    }
    
    public static DirContext wrap(Object obj) {
        if (obj instanceof DirContext) {
            obj = new LdapDirContext((DirContext)obj);
        }
        return (DirContext)obj;
    }
    
    private DirStateFactory.Result getObjectToBind(Name name, Object obj, Attributes attrs) throws NamingException {
        DirStateFactory.Result res = null;
        //System.out.println("1) CALLED getObjectToBind on a "+obj.getClass().toString());
        try {
            //
            // If this is a remote (RMI) object, I wrap it as a marshalled object.
            // This has the effect of extracting the stub rather than the impl object
            //
            if (obj instanceof Remote) {
                try {
                  obj = new MarshalledObject(obj);
                } catch (java.io.IOException ioe) {
                    System.err.println("Error marshalling Remote object");
                    ioe.printStackTrace();
                }
            }
            
            res = DirectoryManager.getStateToBind(obj, name, 
                    realDirContext, 
                    realDirContext.getEnvironment(), 
                    attrs);
        } catch (NamingException ex) {
            ex.printStackTrace();
            throw ex;
        }
        //System.out.println("Returning a  "+res.getObject().getClass().toString());

        return res;
    }
    
    private Object getBoundObject(Name name, Object obj, Attributes attrs) throws Exception {
        Object ret;
        //System.out.println("2) CALLED getBoundObject on a "+obj.getClass().toString());
        
        ret = DirectoryManager.getObjectInstance(obj,
                                                  name,
                                                  realDirContext, 
                                                  realDirContext.getEnvironment(), 
                                                  attrs);
        
        //
        // If this is a marshalled object, I unwrap it here.
        // Should be an RMI stub.
        //
        if (ret instanceof MarshalledObject) {
            ret = ((MarshalledObject)ret).get();
        }
       //System.out.println("Returning a  "+ret.getClass().toString());
       return ret;
    }
    
    
    /**
     * This class and updateSearchResults() provide hooks so that we can call
     * getBoundObject() on objects that are returned from a search.  lookup()
     * just calls it directly.
     */
    private class LdapNamingEnumeration implements NamingEnumeration {
        
        private NamingEnumeration wrapped;
        public LdapNamingEnumeration (NamingEnumeration wrapped) {
            this.wrapped = wrapped;
        }
        
        public void close() throws javax.naming.NamingException {
            wrapped.close();
        }
        
        public java.lang.Object next() throws javax.naming.NamingException {
            return nextElement();
        }
        
        public boolean hasMore() throws javax.naming.NamingException {
            return wrapped.hasMore();
        }
        
        public java.lang.Object nextElement() {
            SearchResult sr = (SearchResult)wrapped.nextElement();
            try {
            sr.setObject(getBoundObject(nameParser.parse(sr.getName()), sr.getObject(), sr.getAttributes()));
            } catch (Exception ex) {
                System.err.println("Error unwrapping search results");
                ex.printStackTrace();
            }
            return sr;
        }
        
        public boolean hasMoreElements() {
            return wrapped.hasMoreElements();
        }
        
    }
    
    private NamingEnumeration updateSearchResults(NamingEnumeration enum) {
        return new LdapNamingEnumeration(enum);
    }
    
}
