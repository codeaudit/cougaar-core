/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.spi.*;
import java.util.*;
import org.cougaar.core.society.NameServer;
import junit.framework.*;

/**
 * A JUnit TestCase for testing the NamingDirContext and server
 **/
public class NamingDirContextTest extends TestCase {
  DirContext root;
  NSImpl ns;

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

  public void testBindLookup() throws NamingException, RemoteException {
    testBindLookup(root);
  }

  private void testUnboundName(DirContext ctx, String name) throws NamingException, RemoteException {
    try {
      ctx.lookup(name);
      fail("lookup failed to throw NamingException");
    } catch (NamingException ne) {
    }
  }

  private void testBindLookup(DirContext ctx) throws NamingException, RemoteException {
    String name = "a";
    String value = "The value of a";
    Attributes attributes = new BasicAttributes();
    testUnboundName(ctx, name);
    attributes.put(new BasicAttribute("fact", "the letter A"));
    attributes.put(new BasicAttribute("chocolate", "Merckens"));
    ctx.bind(name, value, attributes);
    assertEquals("Value mismatch", value, ctx.lookup(name));
    Attributes fetchedAttributes = ctx.getAttributes(name);
    assertEquals("Fetched attributes differ from original",
                 attributes, fetchedAttributes);
    ctx.unbind(name);
    testUnboundName(ctx, name);
  }

  public void testCreateSubcontext() throws NamingException, RemoteException {
    Attributes attributes = new BasicAttributes();
    testUnboundName(root, "a");
    DirContext a = root.createSubcontext("a", attributes);
    assertNotNull("Null context", a);
    assertEquals("full name wrong", "a", a.getNameInNamespace());
    testBindLookup(a);          // Make sure it works, too
    try {
      root.createSubcontext("b/c", attributes);
      fail("createSubcontext in non-existant context failed to throw NamingException");
    } catch (NamingException ne) {
    }
    DirContext b_in_a = root.createSubcontext("a/b", attributes);
    assertNotNull("Null context", b_in_a);
    assertEquals("full name wrong", "a/b", b_in_a.getNameInNamespace());
    testBindLookup(b_in_a);     // Make sure it works, too
    try {
      a.createSubcontext("b", attributes); // Should fail because already exists
      fail("create already existing subcontext failed to throw NamingException");
    } catch (NamingException ne) {
    }
    assertNotNull("b in a is null", a.lookup("b"));
    try {
      a.unbind("b");            // This should refuse
      fail("unbind of context failed to throw NamingException");
    } catch (NamingException ne) {
    }
    a.destroySubcontext("b");
    testUnboundName(a, "b");
    root.destroySubcontext("a");
    testUnboundName(root, "a");
  }
}
