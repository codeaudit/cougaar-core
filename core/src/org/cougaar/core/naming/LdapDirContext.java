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

import javax.naming.directory.*;
import javax.naming.*;




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
        realDirContext.bind(name, obj);
    }
    
    public void bind(String str, Object obj, Attributes attributes) throws NamingException {
        bind (nameParser.parse(str), obj, attributes);
    }
    
    public void bind(Name name, Object obj, Attributes attributes) throws NamingException {
        realDirContext.bind(name, obj, attributes);
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
        realDirContext.rebind(name, obj);
    }
    
    public void rebind(String str, Object obj, Attributes attributes) throws NamingException {
        rebind(nameParser.parse(str), obj, attributes);
    }
    
    public void rebind(Name name, Object obj, Attributes attributes) throws NamingException {
        realDirContext.rebind(name, obj, attributes);
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
    
    public NamingEnumeration search(Name name, Attributes attributes) throws NamingException {
        return realDirContext.search(name, attributes);
    }
    
    public NamingEnumeration search(String str, String filter, SearchControls searchControls) throws NamingException {
        return search(nameParser.parse(str), filter, searchControls);
    }
    
    public NamingEnumeration search(String str, Attributes attributes, String[] str2) throws NamingException {
        return search(nameParser.parse(str), attributes, str2);
    }
    
    public NamingEnumeration search(Name name, String str, SearchControls searchControls) throws NamingException {
        return realDirContext.search(name, str, searchControls);
    }
    
    public NamingEnumeration search(Name name, Attributes attributes, String[] str) throws NamingException {
        return realDirContext.search(name, attributes, str);
    }
    
    public NamingEnumeration search(String str, String str1, Object[] obj, SearchControls searchControls) throws NamingException {
        return search(nameParser.parse(str), str1, obj, searchControls);
    }
    
    public NamingEnumeration search(Name name, String str, Object[] obj, SearchControls searchControls) throws NamingException {
        return realDirContext.search(name, str, obj, searchControls);
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
    
}
