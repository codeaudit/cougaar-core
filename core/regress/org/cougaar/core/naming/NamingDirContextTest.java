/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.*;
import junit.framework.*;

/**
 * A JUnit TestCase for testing the NamingDirContext and server
 **/
public class NamingDirContextTest extends TestCase {
  DirContext root;
  NSImpl ns;
  private static SearchControls oneLevelSearchControls;
  private static SearchControls subtreeSearchControls;
  private static SearchControls boundValueSearchControls;
  static {
    oneLevelSearchControls = new SearchControls();
    oneLevelSearchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    subtreeSearchControls = new SearchControls();
    subtreeSearchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    boundValueSearchControls = new SearchControls();
    boundValueSearchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    boundValueSearchControls.setReturningObjFlag(true);
  }

  public interface TestBindData {
    String getName();
    Object getObject();
    Attributes getAttributes() throws NamingException;
  }

  public static class StringTestBindData implements TestBindData {
    private String name;
    private String value;
    private String[][] nvs;
    public StringTestBindData(String name, String value, String[][] nvs) {
      this.name = name;
      this.value = value;
      this.nvs = nvs;
    }
    public String getName() {
      return name;
    }
    public Object getObject() {
      return value;
    }
    public Attributes getAttributes() throws NamingException {
      Attributes attributes = new BasicAttributes();
      for (int j = 0; j < nvs.length; j++) {
        attributes.put(new BasicAttribute(nvs[j][0], nvs[j][1]));
      }
      return attributes;
    }
  }

  public static class ContextTestBindData implements TestBindData {
    private Context ctx;
    private TestBindData tbd;

    public ContextTestBindData(DirContext ctx, TestBindData tbd) {
      this.ctx = ctx;
      this.tbd = tbd;
    }
    public String getName() {
      return tbd.getName();
    }
    public Object getObject() {
      return tbd.getObject();
    }
    public Attributes getAttributes() throws NamingException {
      Attributes attributes = tbd.getAttributes();
      attributes.put(new BasicAttribute("nameInNamespace",
                                        ctx.getNameInNamespace() + "/" + getName()));
      return attributes;
    }
  }

  public static final TestBindData[] testBindData = {
    new StringTestBindData("Aye", "The value of Aye",
                           new String[][] {{"fact", "the word Aye"},
                                           {"chocolate", "Merckens"}}),
    new StringTestBindData("Bee", "The value of Bee",
                           new String[][] {{"fact", "the word Bee"},
                                           {"chocolate", "Sharffen Berger"}}),
    new StringTestBindData("Lindt", "The value of Lindt",
                           new String[][] {{"chocolate", "Lindt"}})
  };
                     

  public NamingDirContextTest(String name) {
    super(name);
  }

  public void setUp() throws RemoteException {
    ns = new NSImpl();
    root = new NamingDirContext(ns, NSImpl.ROOT, new Hashtable());
  }

  public void tearDown() throws NoSuchObjectException {
    UnicastRemoteObject.unexportObject(ns, true);
    ns = null;
    root = null;
  }

  /**
   * Test that the root context can bind objects and lookup them. Uses
   * the common testBindLookup method.
   **/
  public void testBindLookup() throws NamingException, RemoteException {
    checkEmpty();
    testBindLookup(root);
    checkEmpty();
  }

  /**
   * Inner test method for insuring that a particular name is not (any
   * longer) in the given context. The caller is asserting that the
   * name should not exist in the context.
   * @param ctx the DirContext to examine
   * @param name the name to lookup
   **/
  private void testUnboundName(DirContext ctx, String name) throws NamingException, RemoteException {
    try {
      Object obj = ctx.lookup(name);
      if (obj == null)
        fail("lookup return null instead of throwing NamingException");
      else
        fail("lookup found " + obj + " instead of throwing NamingException");
    } catch (NamingException ne) {
    }
  }

  /**
   * Test that a context is empty.
   **/
  private void checkEmpty() throws NamingException, RemoteException {
    assertTrue("Context not empty", !root.list("").hasMore());
  }

  /**
   * Test that a DirContext can successfully bind, lookup, and unbind.
   * Strings are stored in the context under particular names with
   * particular attributes. they are then retrieved and the values and
   * attributes checked. Finally, the names are unbound and the absence
   * of the names is checked. The names, values, etc. are in the testBindData array
   **/
  private void testBindLookup(DirContext ctx) throws NamingException, RemoteException {
    testBind(ctx);
    testLookup(ctx);
    testUnbind(ctx);
  }

  private void testBind(DirContext ctx) throws NamingException, RemoteException {
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData tbd = new ContextTestBindData(ctx, testBindData[i]);
      String name = tbd.getName();
      Object value = tbd.getObject();
      Attributes attributes = tbd.getAttributes();
      testUnboundName(ctx, name);
      ctx.bind(name, value, attributes);
    }
  }

  private void testLookup(DirContext ctx) throws NamingException, RemoteException {
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData tbd = new ContextTestBindData(ctx, testBindData[i]);
      String name = tbd.getName();
      Object value = tbd.getObject();
      Attributes originalAttributes = tbd.getAttributes();
      assertEquals("Value mismatch", value, ctx.lookup(name));
      Attributes fetchedAttributes = ctx.getAttributes(name);
      assertEquals("Fetched attributes differ from original",
                   originalAttributes, fetchedAttributes);
    }
  }

  private void testUnbind(DirContext ctx) throws NamingException, RemoteException {
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData tbd = testBindData[i];
      String name = tbd.getName();
      ctx.unbind(name);
      testUnboundName(ctx, name);
    }
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData tbd = testBindData[i];
      String name = tbd.getName();
      testUnboundName(ctx, name);
    }
  }

  /**
   * Test the creation of subcontexts. Create a variety of
   * subcontexts. Insure that attempts to create subcontext that
   * already exist fail and that sub-subcontexts can be found.
   **/
  public void testCreateSubcontext() throws NamingException, RemoteException {
    checkEmpty();
    Attributes attributes = new BasicAttributes();
    // First create a subcontext of the root context.
    testUnboundName(root, "a");
    DirContext a = root.createSubcontext("a", attributes);
    assertNotNull("Null context", a);
    assertEquals("full name wrong", "a", a.getNameInNamespace());
    testBindLookup(a);          // Make sure it works, too
    // Try to create a subcontext of a non-existent subcontext; it should fail
    try {
      root.createSubcontext("b/c", attributes);
      fail("createSubcontext in non-existant context failed to throw NamingException");
    } catch (NamingException ne) {
    }
    // Create a subcontext of the "a" subcontext above, but from the root
    DirContext b_in_a = root.createSubcontext("a/b", attributes);
    assertNotNull("Null context", b_in_a);
    assertEquals("full name wrong", "a/b", b_in_a.getNameInNamespace());
    testBindLookup(b_in_a);     // Make sure it works, too
    // Now try to directly create the "b" subcontext. It should fail because it already exists
    try {
      a.createSubcontext("b", attributes); // Should fail because already exists
      fail("create already existing subcontext failed to throw NamingException");
    } catch (NamingException ne) {
    }
    // Now be sure the a/b subcontext can be lookup'ed in the a subcontext
    assertNotNull("b in a is null", a.lookup("b"));
    // Attempt to unbind the subcontext. Should fail because destroySubcontext must be used
    try {
      a.unbind("b");            // This should refuse
      fail("unbind of context failed to throw NamingException");
    } catch (NamingException ne) {
    }
    
    // Check that we won't destroy a non empty context
    a.bind("b/c", "c");
    try {
      a.destroySubcontext("b");
      fail("destroySubcontext of non empty Context failed to throw NamingException");
    } catch (NamingException ne) {
    }

    // remove b/c so we can destroy b
    a.unbind("b/c");

    // Now destroy "b" the proper way
    a.destroySubcontext("b");
    // Be sure it is gone.
    testUnboundName(a, "b");
    // Also destroy "a"
    root.destroySubcontext("a");
    // And be sure it's gone, too
    testUnboundName(root, "a");
    checkEmpty();
  }

  /**
   * Test attribute searching in this context. First create a
   * hierarchy of contexts. Every context is populated with the values
   * in testBindData above. Then we search for various attributes with
   * various search controls and check the outcome.
   **/
  public void testSearch() throws NamingException, RemoteException {
    checkEmpty();
    DirContext a = root.createSubcontext("a", (Attributes) null);
    DirContext b = root.createSubcontext("b", (Attributes) null);
    DirContext x_in_a = a.createSubcontext("x", (Attributes) null);
    DirContext y_in_a = a.createSubcontext("y", (Attributes) null);
    DirContext x_in_b = b.createSubcontext("x", (Attributes) null);
    DirContext y_in_b = b.createSubcontext("y", (Attributes) null);
    DirContext m_in_y_in_b = y_in_b.createSubcontext("m", (Attributes) null);
    testBind(a);
    testBind(b);
    testBind(x_in_a);
    testBind(y_in_a);
    testBind(x_in_b);
    testBind(y_in_b);
    testBind(m_in_y_in_b);
    testLookup(a);
    testLookup(b);
    testLookup(x_in_a);
    testLookup(y_in_a);
    testLookup(x_in_b);
    testLookup(y_in_b);
    testLookup(m_in_y_in_b);

    testSearchOneLevel(a);           
    testSearchOneLevel(b);           
    testSearchOneLevel(x_in_a);      
    testSearchOneLevel(y_in_a);      
    testSearchOneLevel(x_in_b);      
    testSearchOneLevel(y_in_b);      
    testSearchOneLevel(m_in_y_in_b);

    testSearchSubtree(new DirContext[] {a, x_in_a, y_in_a});
    testSearchSubtree(new DirContext[] {b, x_in_b, y_in_b, m_in_y_in_b});
    testSearchSubtree(new DirContext[] {x_in_a});
    testSearchSubtree(new DirContext[] {y_in_a});
    testSearchSubtree(new DirContext[] {x_in_b});
    testSearchSubtree(new DirContext[] {y_in_b, m_in_y_in_b});
    testSearchSubtree(new DirContext[] {m_in_y_in_b});

    testUnbind(a);
    testUnbind(b);
    testUnbind(x_in_a);
    testUnbind(y_in_a);
    testUnbind(x_in_b);
    testUnbind(y_in_b);
    testUnbind(m_in_y_in_b);

    y_in_b.destroySubcontext("m");
    b.destroySubcontext("y");
    b.destroySubcontext("x");
    a.destroySubcontext("y");
    a.destroySubcontext("x");
    root.destroySubcontext("b");
    root.destroySubcontext("a");
    
    checkEmpty();
  }

  private NamingEnumeration getAttributes() {
    return new NamingEnumeration() {
      int ix = 0;
      NamingEnumeration attrs = null;
      public boolean hasMore() throws NamingException {
        while (true) {
          while (attrs == null) {
            if (ix >= testBindData.length) return false;
            attrs = testBindData[ix++].getAttributes().getAll();
          }
          if (attrs.hasMoreElements()) return true;
          attrs.close();
          attrs = null;
        }
      }

      public Object next() throws NamingException {
        if (attrs != null) return attrs.nextElement();
        throw new NoSuchElementException();
      }
      public boolean hasMoreElements() {
        try {
          return hasMore();
        } catch (NamingException ne) {
          ne.printStackTrace();
        }
        return false;
      }
      public Object nextElement() {
        if (attrs != null) {
          return attrs.nextElement();
        }
        throw new NoSuchElementException();
      }

      public void close() {
        try {
          if (attrs != null) {
            attrs.close();
            attrs = null;
          }
        } catch (NamingException ne) {
          ne.printStackTrace();
        }
      }
    };
  }

  /**
   * Search one level for objects matched against one Attribute
   **/
  private void testSearchOneLevel(DirContext ctx) throws NamingException {
    for (NamingEnumeration enum = getAttributes(); enum.hasMore(); ) {
      Attribute matchingAttribute = (Attribute) enum.next();
      assertNotNull(matchingAttribute);
      testSearchOneLevelMatch(ctx, matchingAttribute);
      testSearchOneLevelEquality(ctx, matchingAttribute);
      testSearchOneLevelSubstring(ctx, matchingAttribute);
      testSearchOneLevelLessEqual(ctx, matchingAttribute);
      testSearchOneLevelGreaterEqual(ctx, matchingAttribute);
    }
  }

  /**
   * Test match type search operations for a single Attribute
   **/
  private void testSearchOneLevelMatch(DirContext ctx, Attribute matchingAttribute)
    throws NamingException
  {
    String tName = "OneLevelMatch for " + matchingAttribute;
    Attributes matchingAttributes = new BasicAttributes();
    matchingAttributes.put(matchingAttribute);
    Map matchingNames = new HashMap();
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData t = new ContextTestBindData(ctx, testBindData[i]);
      if (matchingAttribute.equals(t.getAttributes().get(matchingAttribute.getID()))) {
        assertNull(matchingNames.put(t.getName(), t));
      }
    }
    checkSearchResults(tName, ctx, ctx.search("", matchingAttributes), matchingNames, false);
    checkSearchResults(tName, ctx, ctx.search("", matchingAttributes, (String[]) null), matchingNames, false);
  }

  /**
   * Test filter string searches that search one level for equality for a single attribute
   **/
  private void testSearchOneLevelEquality(DirContext ctx, Attribute matchingAttribute)
    throws NamingException
  {
    String tName = "OneLevelEquality for " + matchingAttribute;
    String matchString = "(" + matchingAttribute.getID() + "=" + matchingAttribute.get() + ")";
    Map matchingNames = new HashMap();
    for (int i = 0; i < testBindData.length; i++) {
      TestBindData t = new ContextTestBindData(ctx, testBindData[i]);
      if (matchingAttribute.equals(t.getAttributes().get(matchingAttribute.getID()))) {
        assertNull(matchingNames.put(t.getName(), t));
      }
    }
    checkSearchResults(tName, ctx, ctx.search("", matchString, oneLevelSearchControls), matchingNames, false);
  }

  private void testSearchOneLevelSubstring(DirContext ctx, Attribute matchingAttribute) {}
  private void testSearchOneLevelPresence(DirContext ctx, Attribute matchingAttribute) {}
  private void testSearchOneLevelGreaterEqual(DirContext ctx, Attribute matchingAttribute) {}
  private void testSearchOneLevelLessEqual(DirContext ctx, Attribute matchingAttribute) {}

  /**
   * Search one level for objects matched against one Attribute
   **/
  private void testSearchSubtree(DirContext[] contexts) throws NamingException {
    for (NamingEnumeration enum = getAttributes(); enum.hasMore(); ) {
      Attribute matchingAttribute = (Attribute) enum.next();
      assertNotNull(matchingAttribute);
      testSearchSubtreeEquality(contexts, matchingAttribute);
      testSearchSubtreeSubstring(contexts, matchingAttribute);
      testSearchSubtreeLessEqual(contexts, matchingAttribute);
      testSearchSubtreeGreaterEqual(contexts, matchingAttribute);
      testSearchReturningObjEquality(contexts, matchingAttribute);
    }
  }

  /**
   * Test filter string searches that search a subtree for equality for a single attribute
   * @param contexts all the contexts that could contain matches. contexts[0] is the starting point
   **/
  private void testSearchSubtreeEquality(DirContext[] contexts, Attribute matchingAttribute)
    throws NamingException
  {
    String tName = "SubtreeEquality for " + matchingAttribute;
    DirContext ctx0 = contexts[0];
    String matchString = "(" + matchingAttribute.getID() + "=" + matchingAttribute.get() + ")";
    Map matchingNames = new HashMap();
    String ctx0Name = ctx0.getNameInNamespace();
    for (int j = 0; j < contexts.length; j++) {
      DirContext ctx = contexts[j];
      String ctxName = ctx.getNameInNamespace(); // Should have same root equal to ctx0Name
      assertTrue("Test bug -- subcontext name does not start correctly",
                 ctxName.startsWith(ctx0Name));
      String prefix;
      if (ctxName.equals(ctx0Name))
        prefix = "";
      else
        prefix = ctxName.substring(ctx0Name.length() + 1) + "/";
      for (int i = 0; i < testBindData.length; i++) {
        TestBindData t = new ContextTestBindData(ctx, testBindData[i]);
        if (matchingAttribute.equals(t.getAttributes().get(matchingAttribute.getID()))) {
          String name = prefix + t.getName();
          assertNull(matchingNames.put(name, t));
        }
      }
    }
    checkSearchResults(tName, ctx0, ctx0.search("", matchString, subtreeSearchControls), matchingNames, false);
  }

  /**
   * Test filter string searches that search a subtree for equality
   * for a single attribute and checks that the bound value is
   * correct.
   * @param contexts all the contexts that could contain matches.
   * contexts[0] is the starting point
   **/
  private void testSearchReturningObjEquality(DirContext[] contexts, Attribute matchingAttribute)
    throws NamingException
  {
    String tName = "ReturningObjEquality for " + matchingAttribute;
    DirContext ctx0 = contexts[0];
    String matchString = "(" + matchingAttribute.getID() + "=" + matchingAttribute.get() + ")";
    Map matchingNames = new HashMap();
    String ctx0Name = ctx0.getNameInNamespace();
    for (int j = 0; j < contexts.length; j++) {
      DirContext ctx = contexts[j];
      String ctxName = ctx.getNameInNamespace(); // Should have same root equal to ctx0Name
      assertTrue("Test bug -- subcontext name does not start correctly",
                 ctxName.startsWith(ctx0Name));
      String prefix;
      if (ctxName.equals(ctx0Name))
        prefix = "";
      else
        prefix = ctxName.substring(ctx0Name.length() + 1) + "/";
      for (int i = 0; i < testBindData.length; i++) {
        TestBindData t = new ContextTestBindData(ctx, testBindData[i]);
        if (matchingAttribute.equals(t.getAttributes().get(matchingAttribute.getID()))) {
          String name = prefix + t.getName();
          assertNull(matchingNames.put(name, t));
        }
      }
    }
    checkSearchResults(tName, ctx0, ctx0.search("", matchString, boundValueSearchControls), matchingNames, true);
  }

  private void testSearchSubtreeSubstring(DirContext[] contexts, Attribute matchingAttribute) {}
  private void testSearchSubtreePresence(DirContext[] contexts, Attribute matchingAttribute) {}
  private void testSearchSubtreeGreaterEqual(DirContext[] contexts, Attribute matchingAttribute) {}
  private void testSearchSubtreeLessEqual(DirContext[] contexts, Attribute matchingAttribute) {}

  /**
   * Check the result of a search operation.
   * @param enum the SearchResults NamingEnumeration
   * @param matchingNames a Map mapping the names of the expected hits
   * to the corresponding TestBindData
   **/
  private void checkSearchResults(String tName, DirContext ctx, NamingEnumeration enum, Map matchingNames, boolean checkBoundValue)
    throws NamingException
  {
    String ctxName = ctx.getNameInNamespace();
    while(enum.hasMore()) {
      SearchResult sr = (SearchResult) enum.next();
      String name = sr.getName();
      TestBindData t = (TestBindData) matchingNames.get(name);
      assertNotNull(tName + " unexpected match '" + name + "' not in " + matchingNames, t);
      checkAttributes(tName, sr.getAttributes(), t.getAttributes());
      if (checkBoundValue) {
        assertEquals(tName + " incorrect bound value", t.getObject(), sr.getObject());
      } else {
        assertNull(tName + " superfluous bound value", sr.getObject());
      }
    }
  }

  private void checkAttributes(String tName, Attributes superset, Attributes subset)
    throws NamingException
  {
    for (NamingEnumeration enum = subset.getAll(); enum.hasMore(); ) {
      Attribute attr = (Attribute) enum.next();
      String id = attr.getID();
      Attribute found = superset.get(id);
      assertNotNull(tName + " attribute " + id + " missing", found);
      assertEquals(tName + " attribute " + id, attr, found);
    }
  }
}
