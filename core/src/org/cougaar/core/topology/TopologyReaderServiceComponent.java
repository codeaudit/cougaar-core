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

package org.cougaar.core.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;

import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component creates and maintains the node-level
 * TopologyReaderService.
 *
 * @see TopologyReaderService for use by all components
 */
public final class TopologyReaderServiceComponent
extends GenericStateModelAdapter
implements Component 
{

  // save here for easy reference below
  private static final int ANY_AGENT_TYPE = 
    TopologyReaderService.ANY_TYPE;

  private ServiceBroker sb;

  private NamingService namingService;
  private LoggingService log;
  private EventDirContext rootContext;
  private EventDirContext topologyContext;
  private Map cache = new HashMap();
  private NamingListener namingListener = new MyNamingListener();
  private TopologyReaderServiceProviderImpl topologyRSP;
  private int componentId = System.identityHashCode(this);

  private class MyNamingListener implements NamespaceChangeListener, ObjectChangeListener {
    public void objectAdded(NamingEvent evt) {
      try {
        String name = evt.getNewBinding().getName();
        topologyContext.addNamingListener(name, EventContext.OBJECT_SCOPE, namingListener);
        if (log.isDebugEnabled()) log.debug(componentId + " installing NamingListener to added " + name);
      } catch (NamingException ne) {
        log.error("Error installing naming listener", ne);
      }
      clearCache();
    }
    public void objectRemoved(NamingEvent evt) {
      clearCache();
    }
    public void objectRenamed(NamingEvent evt) {
      clearCache();
    }
    public void objectChanged(NamingEvent evt) {
      clearCache();
    }
    public void namingExceptionThrown(NamingExceptionEvent evt) {
    }
  }

  public void setBindingSite(BindingSite bs) {
    // only care about the service broker
    //this.sb = bs.getServiceBroker();
  }

  public void setNodeControlService(NodeControlService ncs) {
    this.sb = ncs.getRootServiceBroker();
  }

  public void load() {
    super.load();

    this.log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    this.namingService = (NamingService)
      sb.getService(this, NamingService.class, null);
    if (namingService == null) {
      throw new RuntimeException(
          "Unable to obtain naming service");
    }

    // create and advertise our services
    this.topologyRSP = new TopologyReaderServiceProviderImpl();
    sb.addService(TopologyReaderService.class, topologyRSP);
  }

  public void unload() {
    // clean up ns?
    // revoke our services
    if (topologyRSP != null) {
      sb.revokeService(TopologyReaderService.class, topologyRSP);
      topologyRSP = null;
    }
    // release all services
    if (namingService != null) {
      sb.releaseService(this, NamingService.class, namingService);
      namingService = null;
    }
    if ((log != null) && (log != LoggingService.NULL)) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  private void clearCache() {
    synchronized (cache) {
      if (log.isDebugEnabled()) log.debug(componentId + " clearCache " + cache.size() + " entries");
      cache.clear();
    }
  }

  private EventDirContext getTopologyContext() throws NamingException {
    synchronized (cache) {
      if (topologyContext == null) {
        try {
          topologyContext = (EventDirContext)
            getRootContext().lookup(TopologyNamingConstants.TOPOLOGY_DIR);
          topologyContext.addNamingListener("", EventContext.SUBTREE_SCOPE, namingListener);
        } catch (NamingException ne) {
          throw ne;
        } catch (Exception e) {
          NamingException x = 
            new NamingException("Unable to access name-server");
          x.setRootCause(e);
          throw x;
        }
      }
      return topologyContext;
    }
  }

  private EventDirContext getRootContext() throws NamingException {
    synchronized (cache) {
      if (rootContext == null) {
        try {
          DirContext initialContext = (DirContext) namingService.getRootContext();
          rootContext = (EventDirContext) initialContext.lookup("");
        } catch (NamingException ne) {
          throw ne;
        } catch (Exception e) {
          NamingException x = 
            new NamingException("Unable to access name-server");
          x.setRootCause(e);
          throw x;
        }
      }
      return rootContext;
    }
  }

  private class CacheKey {
    public static final int SEARCHALLVALUES = 1;
    public static final int SEARCHAGENT_S = 2;
    public static final int SEARCHAGENT_SS = 3;
    public static final int SEARCHAGENT_ISS = 4;
    public static final int SEARCHALLAGENTS = 5;
    public static final int SEARCHALLATTRIBUTES = 6;
    int keyType;
    Object[] keyElements;
    int hash;

    public CacheKey(int keyType, Object[] keyElements) {
      this.keyType = keyType;
      this.keyElements = keyElements;
      hash = keyType;
      for (int i = 0; i < keyElements.length; i++) {
        hash = hash * 31 + keyElements[i].hashCode();
      }
    }

    public int hashCode() {
      return hash;
    }

    public boolean equals(Object o) {
      if (o instanceof CacheKey) {
        CacheKey that = (CacheKey) o;
        if (this.keyType == that.keyType) {
          if (this.keyElements.length == that.keyElements.length) {
            for (int i = 0; i < this.keyElements.length; i++) {
              Object o1 = this.keyElements[i];
              Object o2 = that.keyElements[i];
              if (o1 == o2) {
                return true;
              }
              if (o1 == null) {
                return false;
              }
              if (o2 == null) {
                return false;
              }
              if (!o1.equals(o2)) {
                return false;
              }
            }
            return true;
          }
        }
      }
      return false;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("CacheKey(").append(keyType);
      for (int i = 0; i < keyElements.length; i++) {
        buf.append(", ").append(keyElements[i]);
      }
      buf.append(")");
      return buf.toString();
    }
  }

  private class TopologyReaderServiceProviderImpl
    implements ServiceProvider {

      private final TopologyReaderServiceImpl topologyRS;

      public TopologyReaderServiceProviderImpl() {
        // keep only one instance
        topologyRS = new TopologyReaderServiceImpl();
      }

      public Object getService(
          ServiceBroker sb,
          Object requestor,
          Class serviceClass) {
        if (serviceClass == TopologyReaderService.class) {
          return topologyRS;
        } else {
          return null;
        }
      }

      public void releaseService(
          ServiceBroker sb,
          Object requestor,
          Class serviceClass,
          Object service) {
      }
    }

  private class TopologyReaderServiceImpl
    implements TopologyReaderService {

      public TopologyReaderServiceImpl() {
      }

      //
      // currently no caching!
      //
      // May want to tie into MTS metrics, to avoid lookup 
      // if recent inter-agent communication was observed.
      //

      public String getParentForChild(
          int parentType,
          int childType,
          String childName) {
        return lookupParentForChild(parentType, childType, childName);
      }

      public Set getChildrenOnParent(
          int childType,
          int parentType,
          String parentName) {
        return lookupChildrenOnParent(childType, parentType, parentName);
      }

      public Set getAll(int type) {
        return lookupAll(type);
      }

      public TopologyEntry getEntryForAgent(String agent) {
        return lookupEntryForAgent(agent);
      }

      public Set getAllEntries(
          String agent,
          String node,
          String host,
          String site,
          String enclave) {
        return lookupAllEntries(
            agent, node, host, site, enclave);
      }

      public long getIncarnationForAgent(String agent) {
        return lookupIncarnationForAgent(agent);
      }

      //
      // "lookup*" variants:
      //

      public String lookupParentForChild(
          int parentType,
          int childType,
          String childName) {
        validate(childType, parentType);
        validateName(childType, childName);
        String parentAttr = getAttributeName(parentType);
        if ((childType & ANY_AGENT_TYPE) != 0) {
          return (String) 
            searchAgent(childType, childName, parentAttr);
        }
        String childAttr = getAttributeName(childType);
        return (String)
          searchFirstValue(
              childAttr,
              childName,
              parentAttr);
      }

      public Set lookupChildrenOnParent(
          int childType,
          int parentType,
          String parentName) {
        validate(childType, parentType);
        validateName(parentType, parentName);
        String parentAttr = getAttributeName(parentType);
        Attributes match = new BasicAttributes();
        match.put(parentAttr, parentName);
        return searchAllValues(match, childType);
      }

      public Set lookupAll(int type) {
        validateType(type);
        if (type == ANY_AGENT_TYPE) {
          return searchAllAgents();
        }
        Attributes match = new BasicAttributes();
        return searchAllValues(match, type);
      }

      public long lookupIncarnationForAgent(String agent) {
        Long l = (Long)
          searchAgent(
              agent,
              TopologyNamingConstants.INCARNATION_ATTR);
        return ((l != null) ? l.longValue() : -1L);
      }

      public TopologyEntry lookupEntryForAgent(String agent) {
        Attributes ats = searchAgent(agent);
        TopologyEntry te = 
          ((ats != null) ? 
           createTopologyEntry(ats) :
           null);
        return te;
      }

      public Set lookupAllEntries(
          String agent,
          String node,
          String host,
          String site,
          String enclave) {
        if (agent != null) {
          TopologyEntry te = lookupEntryForAgent(agent);
          if ((te == null) ||
              ((node != null) && 
               (!(node.equals(te.getNode())))) ||
              ((host != null) && 
               (!(host.equals(te.getHost())))) ||
              ((site != null) && 
               (!(site.equals(te.getSite())))) ||
              ((enclave != null) && 
               (!(enclave.equals(te.getEnclave()))))) {
            return Collections.EMPTY_SET;
          }
          return Collections.singleton(te);
        }

        Attributes match = new BasicAttributes();
        if (node != null) {
          match.put(
              TopologyNamingConstants.NODE_ATTR,
              node);
        }
        if (host != null) {
          match.put(
              TopologyNamingConstants.HOST_ATTR,
              host);
        }
        if (site != null) {
          match.put(
              TopologyNamingConstants.SITE_ATTR,
              site);
        }
        if (enclave != null) {
          match.put(
              TopologyNamingConstants.ENCLAVE_ATTR,
              enclave);
        }

        List allAts = searchAllAttributes(match);
        Set tes;
        if (allAts == null) {
          tes = null;
        } else if (allAts.isEmpty()) {
          tes = Collections.EMPTY_SET;
        } else {
          int n = allAts.size();
          tes = new HashSet(n);
          for (int i = 0; i < n; i++) {
            Attributes ats = (Attributes) allAts.get(i);
            if (ats != null) {
              TopologyEntry te = createTopologyEntry(ats);
              if (te != null) {
                tes.add(te);
              }
            }
          }
        }

        return tes;
      }

      private TopologyEntry createTopologyEntry(Attributes ats) {

        String agent   = (String) getAttribute(ats,
            TopologyNamingConstants.AGENT_ATTR);
        String node    = (String) getAttribute(ats,
            TopologyNamingConstants.NODE_ATTR);
        String host    = (String) getAttribute(ats,
            TopologyNamingConstants.HOST_ATTR);
        String site    = (String) getAttribute(ats,
            TopologyNamingConstants.SITE_ATTR);
        String enclave = (String) getAttribute(ats,
            TopologyNamingConstants.ENCLAVE_ATTR);
        String linc    = (String) getAttribute(ats,
            TopologyNamingConstants.INCARNATION_ATTR);
        String lmoveId = (String) getAttribute(ats,
            TopologyNamingConstants.MOVE_ID_ATTR);
        String itype   = (String) getAttribute(ats,
            TopologyNamingConstants.TYPE_ATTR);
        String istatus = (String) getAttribute(ats,
            TopologyNamingConstants.STATUS_ATTR);

        return new TopologyEntry(
            agent, 
            node, 
            host, 
            site,
            enclave,
            Long.parseLong(linc), 
            Long.parseLong(lmoveId), 
            Integer.parseInt(itype),
            Integer.parseInt(istatus));
      }

      /** get named attribute, throw exception if not present */
      private Object getAttribute(Attributes ats, String id) {
        return getAttributeValue(ats, id, true);
      }

      // type to attribute name mapping:

      private String getAttributeName(int type) {
        // type already validated
        int t = type;
        if ((t & ANY_AGENT_TYPE) != 0) {
          t = AGENT;
        }
        switch (t) {
          case AGENT: return TopologyNamingConstants.AGENT_ATTR;
          case NODE: return TopologyNamingConstants.NODE_ATTR;
          case HOST: return TopologyNamingConstants.HOST_ATTR;
          case SITE: return TopologyNamingConstants.SITE_ATTR;
          case ENCLAVE: return TopologyNamingConstants.ENCLAVE_ATTR;
          default: throw new RuntimeException("Invalid type ("+type+")");
        }
      }

      // validate utilities:

      private void validate(
          int childType, int parentType) {
        validateType(childType);
        validateType(parentType);
        validateRelation(childType, parentType);
      }

      private void validateName(int type, String name) {
        if (name == null) {
          throw new IllegalArgumentException(
              "Invalid name \""+
              getTypeAsString(type)+
              "\"");
        }
      }

      private void validateType(int type) {
        int t = type;
        if ((t & ANY_AGENT_TYPE) != 0) {
          t &= ~ANY_AGENT_TYPE;
          t |= AGENT;
        }
        switch (t) {
          case AGENT:
          case NODE:
          case HOST:
          case SITE:
          case ENCLAVE:
            return;
          default: 
            break;
        }
        throw new IllegalArgumentException(
            "Invalid type \""+
            getTypeAsString(type)+
            "\"");
      }

      private void validateRelation(
          int childType, int parentType) {
        // types already validated
        int ct = childType;
        int pt = parentType;
        if ((ct & ANY_AGENT_TYPE) != 0) {
          ct = AGENT;
        }
        if ((pt & ANY_AGENT_TYPE) != 0) {
          pt = AGENT;
        }
        if (ct >= pt) {
          throw new IllegalArgumentException(
              "Child type \""+
              getTypeAsString(childType)+
              "\" is not contained by parent type \""+
              getTypeAsString(parentType)+
              "\"");
        }
      }

      private String getTypeAsString(int i) {
        switch (i) {
          case AGENT: return "AGENT";
          case NODE: return "NODE";
          case HOST: return "HOST";
          case SITE: return "SITE";
          case ENCLAVE: return "ENCLAVE";
          default: break;
        }
        if (((i & ANY_AGENT_TYPE) != 0) &&
            ((i & ~ANY_AGENT_TYPE) == 0)) {
          String s = "AGENT(";
          if ((i & AGENT_TYPE) != 0) s += "L";
          if ((i & NODE_AGENT_TYPE) != 0) s += "N";
          if ((i & SYSTEM_TYPE) != 0) s += "S";
          s += ")";
          return s;
        }
        return "INVALID ("+i+")";
      }

      // search utils:

      private Object getAttributeValue(Attributes ats, String id) {
        return getAttributeValue(ats, id, false);
      }

      private Object getAttributeValue(
          Attributes ats, String id, boolean confirm) {
        if (ats == null) {
          if (confirm) {
            throw new RuntimeException(
                "Null attributes set");
          } else {
            return null;
          }
        }
        Attribute at = ats.get(id);
        if (at == null) {
          if (confirm) {
            throw new RuntimeException(
                "Unknown attribute \""+id+"\"");
          } else {
            return null;
          }
        }
        Object val;
        try {
          val = at.get();
        } catch (NamingException ne) {
          throw new RuntimeException(
              "Unable to get value for attribute \""+id+"\"",
              ne);
        }
        if (val == null) {
          if (confirm) {
            throw new RuntimeException(
                "Null value for attribute \""+id+"\"");
          } else {
            return null;
          }

        }
        return val;
      }

      private Attributes searchAgent(String agent) {
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHAGENT_S, new Object[] {agent});
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return (Attributes) cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          Attributes ret;
          try {
            EventDirContext ctx = getTopologyContext();
            ret = ctx.getAttributes(agent);
          } catch (NamingException ne) {
            if (ne instanceof NameNotFoundException) {
              ret = null;
            }
            throw new RuntimeException(
                                       "Unable to access name server", ne);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }

      private Object searchAgent(
          String agent, String single_attr) {
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHAGENT_SS, new Object[] {agent, single_attr});
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return (Attributes) cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          Object ret;
          try {
            EventDirContext ctx = getTopologyContext();
            String[] ats_filter = { single_attr };
            Attributes ats = ctx.getAttributes(agent);
            ret = getAttributeValue(ats, single_attr);
          } catch (NamingException ne) {
            if (ne instanceof javax.naming.NameNotFoundException) {
              ret = null;
            }
            throw new RuntimeException(
                                       "Unable to access name server", ne);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }

      private Object searchAgent(
          int type, String agent, String single_attr) {
        if (type == ANY_AGENT_TYPE) {
          return searchAgent(agent, single_attr);
        }
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHAGENT_ISS,
                                           new Object[] {new Integer(type), agent, single_attr});
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          Object ret;
          try {
            EventDirContext ctx = getTopologyContext();
            String[] ats_filter = { 
              single_attr, 
              TopologyNamingConstants.TYPE_ATTR,
            };
            Attributes ats = ctx.getAttributes(agent);
            ret = getAttributeValue(ats, single_attr);
            if (ret == null) {
              ret = null;
            } else {
              Object otype = getAttributeValue(ats, 
                                               TopologyNamingConstants.TYPE_ATTR);
              if (otype == null) {
                ret = null;
              } else {
                int itype = Integer.parseInt((String) otype);
                if ((type & itype) == 0) {
                  ret = null;
                }
              }
            }
          } catch (NamingException ne) {
            if (ne instanceof javax.naming.NameNotFoundException) {
              ret = null;
            }
            throw new RuntimeException(
                                       "Unable to access name server", ne);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }

      private Set searchAllAgents() {
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHALLAGENTS, new Object[0]);
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return (Set) cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          Set ret;
          try {
            EventDirContext ctx = getTopologyContext();
            NamingEnumeration e = ctx.list("");
            if (!(e.hasMore())) {
              ret = Collections.EMPTY_SET;
            } else {
              ret = new HashSet(13);
              do {
                NameClassPair ncp = (NameClassPair) e.next();
                String agent = ncp.getName();
                if (agent != null) {
                  ret.add(agent);
                }
              } while (e.hasMore());
            }
          } catch (NamingException ne) {
            throw new RuntimeException(
                                       "Unable to access name server", ne);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }

      private Attributes searchFirstAttributes(Attributes match) {
        // could optimize:
        List ats = searchAllAttributes(match);
        if ((ats == null) || 
            (ats.size() < 1)) {
          return null;
        }
        return (Attributes) ats.get(0);
      }

      private Object searchFirstValue(
          String filter_name, String filter_value, String single_attr) {
        Attributes match = new BasicAttributes();
        match.put(filter_name, filter_value);
        Attributes ats = searchFirstAttributes(match);
        return getAttributeValue(ats, single_attr);
      }

      private List searchAllAttributes(Attributes match) {
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHALLATTRIBUTES, new Object[] {match});
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return (List) cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          List ret;
          try {
            NamingEnumeration e =
              getTopologyContext().search(
                                          "",
                                          match,
                                          null);
            if (!(e.hasMore())) {
              ret = Collections.EMPTY_LIST;
            } else {
              ret = new ArrayList(13);
              do {
                SearchResult result = (SearchResult) e.next();
                if (result != null) {
                  Attributes ats = result.getAttributes();
                  if (ats != null) {
                    ret.add(ats);
                  }
                }
              } while (e.hasMore());
            }
          } catch (NamingException e) {
            if (log.isDebugEnabled()) log.debug(e.toString(), e);
            throw new RuntimeException(
                                       "Unable to access name server", e);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }

      private Set searchAllValues(
          Attributes match, int type) {
        synchronized (cache) {
          CacheKey cacheKey = new CacheKey(CacheKey.SEARCHALLVALUES, new Object[] {match, new Integer(type)});
          if (cache.containsKey(cacheKey)) {
            if (log.isDebugEnabled()) log.debug(componentId + " cache hit " + cacheKey);
            return (Set) cache.get(cacheKey);
          }
          if (log.isDebugEnabled()) log.debug(componentId + " cache miss " + cache.size() + " " + cacheKey);
          Set ret;
          String single_attr = getAttributeName(type);
          boolean isAgentFilter = 
            (((type & ANY_AGENT_TYPE) != 0) &&
             ((type != ANY_AGENT_TYPE)));
          try {
            NamingEnumeration e;
            if (isAgentFilter) {
              String[] ats_filter = { 
                single_attr, 
                TopologyNamingConstants.TYPE_ATTR,
              };
              e = getTopologyContext().search("", match,
                                              ats_filter);
            } else {
              String[] ats_filter = { single_attr };
              e = getTopologyContext().search("", match, ats_filter);
            }
            if (!(e.hasMore())) {
              ret = Collections.EMPTY_SET;
            } else {
              ret = new HashSet(13);
              do {
                SearchResult result = (SearchResult) e.next();
                // get value
                if (result == null) continue;
                Attributes ats = result.getAttributes();
                Object val = getAttributeValue(ats, single_attr);
                if (val == null) continue;
                // check agent-type filter
                if (isAgentFilter) {
                  Object otype = getAttributeValue(ats, 
                                                   TopologyNamingConstants.TYPE_ATTR);
                  if (otype == null) continue;
                  int itype = Integer.parseInt((String) otype);
                  if ((type & itype) == 0) continue;
                }
                // add value
                ret.add(val);
              } while (e.hasMore()); 
            }
          } catch (NamingException e) {
            throw new RuntimeException(
                                       "Unable to access name server", e);
          }
          cache.put(cacheKey, ret);
          return ret;
        }
      }
  }
}

