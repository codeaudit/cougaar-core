/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.PrintStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import javax.naming.spi.NamingManager;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Implementation of javax.naming.directory.DirContext for 
 * Cougaar Naming Service
 */

public class NamingEventContext extends NamingDirContext implements EventDirContext {
  private static SearchControls ONELEVEL_CTLS =
    new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, new String[0], false, false);
  private static SearchControls OBJECT_CTLS =
    new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, new String[0], false, false);
  private static SearchControls SUBTREE_CTLS =
    new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, new String[0], false, false);
  private static Logger logger = Logging.getLogger(NamingEventContext.class);
  private static Filter TRUE_FILTER =
    new Filter() {
      public boolean match(Attributes a) {
        return true;
      }
      public void toString(StringBuffer b) {
        b.append("TRUE");
      }
    };

  /**
   * Maintain a map from listener to callback id so that a given
   * listener always uses the same callback id. This greatly
   * simplifies unregistering the callbacks.
   **/
  private Map callbackIds = new HashMap(13);
  private Map listeners = new HashMap(13);

  /**
   * Our callback
   **/
  private NSCallbackImpl callback;

  private int nextId = 0;

  private String myFullName;

  protected NamingEventContext(NS ns, NSKey nsKey, Hashtable inEnv)
  {
    super(ns, nsKey, inEnv);
    myClassName = getClass().getName();
  }

  protected Context cloneContext() {
    return new NamingEventContext(myNS, myNSKey, myEnv);
  }

  protected Context createContext(NSKey nsKey) {
    return new NamingEventContext(myNS, nsKey, myEnv);
  }

  public void close() throws NamingException {
    NamingListener[] ll = (NamingListener[])
      listeners.values().toArray(new NamingListener[listeners.size()]);
    for (int i = 0; i < ll.length; i++) {
      removeNamingListener(ll[i]);
    }
    super.close();
  }

  // Implementation of the EventContext interface
  public void addNamingListener(Name target, int scope, NamingListener l)
    throws NamingException
  {
    SearchControls ctls;
    switch (scope) {
    case ONELEVEL_SCOPE: ctls = ONELEVEL_CTLS; break;
    case OBJECT_SCOPE: ctls = OBJECT_CTLS; break;
    case SUBTREE_SCOPE: ctls = SUBTREE_CTLS; break;
    default: throw new NamingException("Illegal scope=" + scope);
    }
    addNamingListener(l, search(target, TRUE_FILTER, ctls));
  }

  public void addNamingListener(String target, int scope, NamingListener l)
    throws NamingException, InvalidNameException
  {
    addNamingListener(new CompositeName(target), scope, l);
  }

  public void removeNamingListener(NamingListener l)
    throws NamingException
  {
    NSCallback.Id cbid;
    synchronized (callbackIds) {
      cbid = (NSCallback.Id) callbackIds.remove(l);
      listeners.remove(cbid);
    }
    if (cbid != null) {
      try {
        getNS().unregisterInterest(getNSKey(), cbid);
      } catch (RemoteException re) {
        throw newNamingException(re);
      }
    }
  }

  /**
   * We don't support listening to targets that don't yet exist.
   **/
  public boolean targetMustExist() {
    return true;
  }

  // Implementation of EventDirContext
  public void addNamingListener(Name target,
                                String filter,
                                Object[] filterArgs,
                                SearchControls ctls,
                                NamingListener l)
    throws NamingException
  {
    addNamingListener(l, search(target, filter, filterArgs, trimSearchControls(ctls)));
  }

  public void addNamingListener(Name target,
                                String filter,
                                SearchControls ctls,
                                NamingListener l)
    throws NamingException
  {
    addNamingListener(l, search(target, filter, trimSearchControls(ctls)));
  }

  public void addNamingListener(String target,
                                String filter,
                                Object[] filterArgs,
                                SearchControls ctls,
                                NamingListener l)
    throws NamingException
  {
    ctls = trimSearchControls(ctls);
    addNamingListener(l, search(target, filter, filterArgs, ctls));
  }

  public void addNamingListener(String target,
                                String filter,
                                SearchControls ctls,
                                NamingListener l)
    throws NamingException
  {
    addNamingListener(l, search(target, filter, trimSearchControls(ctls)));
  }
  // End of interface implementations

  private NamingException newNamingException(RemoteException re) {
    NamingException ne = new NamingException("Remote exception");
    ne.setRootCause(re);
    return ne;
  }

  /**
   * Reduce the SearchControls to just enough to server our needs. In
   * particular, we do not the objects or attributes to be retrieved,
   * only the names. If the given SearchControls is satisfactory, use
   * it. Otherwise, create a new one retaining only that part of the
   * SearchControls that determines the extent of the search.
   **/
  private SearchControls trimSearchControls(SearchControls ctls) {
    if (ctls.getReturningAttributes() == null ||
        ctls.getReturningAttributes().length > 0 ||
        ctls.getReturningObjFlag()) {
      ctls = new SearchControls(ctls.getSearchScope(),
                                ctls.getCountLimit(),
                                ctls.getTimeLimit(),
                                new String[0],
                                false,
                                false);
    }
    return ctls;
  }

  /**
   * Track down the context of the name and return its key and the
   * name of the object in that context (in an NSObjectKey). If the
   * name is that of a Context, its key is return with a name of "".
   * Otherwise, the name will not be "". If any element of the path
   * (except the last) fails to resolve to a context, throw an
   * NotContextException.
   **/
  private NSObjectKey resolveName(Name name)
    throws NamingException, RemoteException
  {
    try {
      NSObjectKey result = new NSObjectKey(getNSKey(), ""); // Start with this context
      for (int i = 0, n = name.size(); i < n; i++) {
        String atom = name.get(i);
        Object next = getNS().getKey(result.nsKey, atom);
        if (next instanceof NSKey) { // Always ok if next is a Context
          result.nsKey = (NSKey) next; // Name
        } else if (i + 1 < n) {
          throw new NotContextException(getNS().fullName(result.nsKey, atom) + " is not a name context");
        } else {
          result.name = atom;
        }
      }
      return result;
    } catch (RemoteException re) {
      throw newNamingException(re);
    }
  }

  /**
   * Common code for all search-based targets.
   **/
  private void addNamingListener(NamingListener l, NamingEnumeration e)
    throws NamingException
  {
    try {
      NSCallback.Id cbid = getCallbackId(l);
      Map namesByContext = new HashMap(13);
      while (e.hasMore()) {
        SearchResult sr = (SearchResult) e.next();
        String name = sr.getName();
        NSObjectKey key = resolveName(new CompositeName(name));
        List names = (List) namesByContext.get(key.nsKey);
        if (names == null) {
          names = new ArrayList();
          namesByContext.put(key.nsKey, names);
        }
        names.add(key.name);
      }
      for (Iterator i = namesByContext.keySet().iterator(); i.hasNext(); ) {
        NSKey dirKey = (NSKey) i.next();
        List names = (List) namesByContext.get(dirKey);
        String[] nameAry = (String[]) names.toArray(new String[names.size()]);
        registerInterest(cbid, dirKey, nameAry);
      }
    } catch (RemoteException re) {
      throw newNamingException(re);
    }
  }

  /**
   * Get the callback id for a given listener. The same callback id is
   * used for a given listener until that listener is unregistered.
   **/
  private NSCallback.Id getCallbackId(NamingListener l)
    throws RemoteException
  {
    NSCallback.Id cbid;
    synchronized (callbackIds) {
      cbid = (NSCallback.Id) callbackIds.get(l);
      if (cbid == null) {
        if (callback == null) callback = new NSCallbackImpl(this);
        int interestType = 0;
        if (l instanceof NamespaceChangeListener) {
          interestType |= NSCallback.NAMESPACE_INTEREST;
        }
        if (l instanceof ObjectChangeListener) {
          interestType |= NSCallback.OBJECT_INTEREST;
        }
        cbid = new NSCallback.Id(callback, ++nextId, interestType);
        callbackIds.put(l, cbid);
        listeners.put(cbid, l);
      }
      return cbid;
    }
  }

  private NamingListener getListener(NSCallback.Id cbid) {
    synchronized (callbackIds) {
      return (NamingListener) listeners.get(cbid);
    }
  }

  void dispatch(List events) {
    for (Iterator i = events.iterator(); i.hasNext(); ) {
      NSNamingEvent evt = (NSNamingEvent) i.next();
      NSCallback.Id cbid = evt.getCallbackId();
      NamingListener l = getListener(cbid);
      if (l == null) {
        if (logger.isWarnEnabled()) logger.warn("No listener found for " + cbid);
        continue;
      }
      Binding oldBinding = evt.oldBinding;
      if (oldBinding != null && !oldBinding.isRelative()) {
        String name = oldBinding.getName();
        Object nsObj = getNSObject(oldBinding.getObject());
        try {
          nsObj = NamingManager.getObjectInstance(nsObj, null, null, myEnv);
        } catch (Exception e) {
          logger.error("Exception getting name server object", e);
        }
	// WARNING: Next line not currently jikes-compilable. Use javac with -source=1.4
        assert name.startsWith(myFullName);
        oldBinding = new Binding(name.substring(myFullName.length()), nsObj);
      }
      Binding newBinding = evt.newBinding;
      if (newBinding != null && !newBinding.isRelative()) {
        String name = newBinding.getName();
        Object nsObj = getNSObject(newBinding.getObject());
        try {
          nsObj = NamingManager.getObjectInstance(nsObj, null, null, myEnv);
        } catch (Exception e) {
          logger.error("Exception getting name server object", e);
        }
	// WARNING: Next line not currently jikes-compilable. Use javac with -source=1.4
        assert name.startsWith(myFullName);
        newBinding = new Binding(name.substring(myFullName.length()), nsObj);
      }
      new NamingEvent(this, evt.type, newBinding, oldBinding, null).dispatch(l);
    }
  }

  /**
   * Register interest in a set of objects in a given context
   **/
  private void registerInterest(NamingListener l, NSKey dirKey, String[] names)
    throws NamingException, RemoteException
  {
    registerInterest(getCallbackId(l), dirKey, names);
  }

  private void registerInterest(NSCallback.Id cbid, NSKey dirKey, String[] names)
    throws NamingException, RemoteException
  {
    if (myFullName == null) {
      myFullName = getNS().fullName(getNSKey(), "") + NS.DirSeparator;
    }
    getNS().registerInterest(dirKey, names, cbid);
  }

  private static class NSObjectKey implements Serializable {
    private NSKey nsKey;
    private String name;

    public NSObjectKey(NSKey nsKey, String name) {
      this.nsKey = nsKey;
      this.name = name;
    }
  }

  private static void print(NamingEvent evt, String label) {
    System.out.println(label
                       + ": context=" + evt.getEventContext()
                       + ", type=" + evt.getType()
                       + ", new=" + evt.getNewBinding()
                       + ", old=" + evt.getOldBinding());

  }
    
  private static void print(NamingExceptionEvent evt, String label) {
    System.out.println(label + ": exception=" + evt.getException());
  }

  private static class Listener implements NamespaceChangeListener, ObjectChangeListener {
    String id;
    Listener(String id) {
      this.id = id;
    }
    public void objectAdded(NamingEvent evt) {
      print(evt, id + " objectAdded");
    }
    public void objectChanged(NamingEvent evt) {
      print(evt, id + " objectChanged");
    }
    public void objectRemoved(NamingEvent evt) {
      print(evt, id + " objectRemoved");
    }
    public void objectRenamed(NamingEvent evt) {
      print(evt, id + " objectRenamed");
    }
    public void namingExceptionThrown(NamingExceptionEvent evt) {
      print(evt, id + " namingExceptionThrown");
    }
  }

  private static class ObjectListener implements ObjectChangeListener {
    String id;
    ObjectListener(String id) {
      this.id = id;
    }
    public void objectChanged(NamingEvent evt) {
      print(evt, id + " objectChanged");
    }
    public void namingExceptionThrown(NamingExceptionEvent evt) {
      print(evt, id + " namingExceptionThrown");
    }
  }

  private static class NamespaceListener implements NamespaceChangeListener {
    String id;
    NamespaceListener(String id) {
      this.id = id;
    }
    public void objectAdded(NamingEvent evt) {
      print(evt, id + " objectAdded");
    }
    public void objectRemoved(NamingEvent evt) {
      print(evt, id + " objectRemoved");
    }
    public void objectRenamed(NamingEvent evt) {
      print(evt, id + " objectRenamed");
    }
    public void namingExceptionThrown(NamingExceptionEvent evt) {
      print(evt, id + " namingExceptionThrown");
    }
  }

  public static void main(String[] args) {
    System.out.println(System.getProperty("user.home"));
    try {
      String prefix = null;
      if (args.length > 0) prefix = args[0];
      NamingServiceFactory nsf = new NamingServiceFactory();
      EventDirContext root = (EventDirContext) nsf.getInitialContext(new Hashtable());
      if (prefix == null) return;
      NamingListener rootSubtreeObjectListener =
        new ObjectListener("Root Subtree Scope Object Listener(attribute1=attr1Value)");
      NamingListener rootNamespaceListener = new NamespaceListener("Root Object Scope Namespace Listener");
      NamingListener subOneLevelObjectListener = new ObjectListener("Sub One Level Object Listener");
      NamingListener subOneLevelNamespaceListener = new NamespaceListener("Sub One Level Namespace Listener");
      NamingListener bothListener = new Listener("Both Listener");
      root.addNamingListener("", OBJECT_SCOPE, rootNamespaceListener);
      root.bind(prefix + "name1", "value1");
      BasicAttributes attrs = new BasicAttributes();
      attrs.put("attribute1", "attr1Value");
      root.bind(prefix + "name2", "value2", attrs);
      try {
        root.bind("rebindname", prefix + "rebindvalue");
      } catch (NameAlreadyBoundException abe) {
        root.rebind("rebindname", prefix + "reboundvalue");
      }
      NamingEventContext subcontext;
      try {
        subcontext = (NamingEventContext) root.createSubcontext("subcontext");
      } catch (NameAlreadyBoundException abe) {
        subcontext = (NamingEventContext) root.lookup("subcontext");
      }
      subcontext.addNamingListener("", ONELEVEL_SCOPE, subOneLevelNamespaceListener);
      NamingEventContext subsub = (NamingEventContext) subcontext.createSubcontext(prefix + "subsub");
      root.rename(prefix + "name1", prefix + "name3");
      root.rebind(prefix + "name3", "newvalue3");
      root.unbind(prefix + "name2");
      subcontext.bind(prefix + "sub1", "subv1", attrs);
      subsub.bind(prefix + "subsub1", "subsubv1");
      subsub.unbind(prefix + "subsub1");
      subcontext.destroySubcontext(prefix + "subsub");
//       root.removeNamingListener(listener1);
      root.addNamingListener("", "(attribute1=attr1Value)", SUBTREE_CTLS, rootSubtreeObjectListener);
      root.addNamingListener("", SUBTREE_SCOPE, bothListener);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
