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
import java.util.List;
import java.util.Set;
import javax.naming.Binding;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext; // inlined
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
import org.cougaar.core.component.ServiceRevokedListener;
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
  private TopologyReaderServiceProviderImpl topologyRSP;

  private Cache cache;
  private UncachedTopology uncachedTopology;

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

    uncachedTopology = new UncachedTopology();
    cache =
      new Cache(
          new KeyedFetcher(uncachedTopology),
          "Topology");

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
    // kill cache
    if (cache != null) {
      // cache.stop();
      uncachedTopology = null;
      cache = null;
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

  //
  //  void exampleUsage() {
  //    // EXAMLE CODE:
  //    String agentName = "test";
  //    TopologyEntry agentInfo;
  //    didTimeout = false;
  //    haveStale = false;
  //    try {
  //      agentInfo = topologyService.getEntryForAgent(agentName);
  //    } catch (TopologyReaderService.TimeoutException te) {
  //      didTimeout = true;
  //      if (te.hasStale()) {
  //        agentInfo = tte.withStale().getEntryForAgent(agentName);
  //      } else {
  //        agentInfo = null;
  //        haveStale = true;
  //      }
  //    }
  //    if (agentInfo == null) {
  //      if (didTimeout) {
  //        // don't know, maybe not listed
  //      } else {
  //        // not listed in topology
  //      } 
  //    } else {
  //      if (didTimeout) {
  //        if (haveStale) {
  //          // can use the stale
  //        } else {
  //          // no current or stale value
  //        }
  //      } else {
  //        // good
  //      }
  //    }
  //  }
  //

  //
  // Timeout exceptions, to be moved into the service interface:
  //

  private static abstract class TopologyTimeoutException
    extends TopologyReaderService.TimeoutException {
      protected final boolean hasStale;
      protected final Object value;
      TopologyTimeoutException(
          boolean hasStale, Object value) {
        this.hasStale = hasStale;
        this.value = value;
      }
      public boolean hasStale() {
        return hasStale;
      }
      public TopologyReaderService withStale() {
        if (!hasStale) {
          throw new UnsupportedOperationException(
              "No stale value");
        }
        return getTopology();
      }

      protected abstract TopologyReaderService getTopology();
    };

  private static class TopologyTimeoutAgentException 
    extends TopologyTimeoutException {
      TopologyTimeoutAgentException(
          boolean hasStale, Object value) {
        super(hasStale, value);
      }
      protected TopologyReaderService getTopology() {
        return new UnsupportedTopology() {
          protected Attributes fetchAgent(String agent) {
            return (Attributes) value;
          }
        };
      }
    };

  private static class TopologyTimeoutNamesException 
    extends TopologyTimeoutException {
      TopologyTimeoutNamesException(
          boolean hasStale, Object value) {
        super(hasStale, value);
      }
      protected TopologyReaderService getTopology() {
        return new UnsupportedTopology() {
          protected Set fetchNames(int type, Attributes match) {
            return (Set) value;
          }
        };
      }
    };

  private static class TopologyTimeoutAgentsException 
    extends TopologyTimeoutException {
      TopologyTimeoutAgentsException(
          boolean hasStale, Object value) {
        super(hasStale, value);
      }
      protected TopologyReaderService getTopology() {
        return new UnsupportedTopology() {
          protected List fetchAgents(Attributes match) {
            return (List) value;
          }
        };
      }
    };

  //
  // simple service provider:
  //

  private class TopologyReaderServiceProviderImpl
    implements ServiceProvider {

      public Object getService(
          ServiceBroker sb,
          Object requestor,
          Class serviceClass) {
        if (serviceClass == TopologyReaderService.class) {
          return new CachedTopology();
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

  //
  // Backer for all topology lookups:
  //

  private interface TopologyFetcher {

    /**
     * Fetch the attributes for the specified agent.
     * <p>
     * For example, to fetch the attributes for agent X:<pre>
     *   fetchAgent("X")
     * </pre>
     */
    Attributes fetchAgent(String agent);

    /**
     * Fetch all names of the given entry type and where
     * the parent matches the specified parent name.
     * <p>
     * If the match is null then all elements match.
     */
    Set fetchNames(int type, Attributes match);

    /**
     * Fetch all agent attributes which match the specified
     * filter.
     * <p>
     * For example, if the "match" is (node=X, host=Y), then<pre>
     *   1) find all agents on node X
     *   2) filter these agents to just those on host Y
     *   3) fetch the attributes for each matching agent
     *   4) return the list of attributes (one per agent)
     * </pre>
     */
    List fetchAgents(Attributes match);
  }

  //
  // topology reader service implementations:
  //

  private static abstract class AbstractTopology
    implements TopologyReaderService {

      //
      // abstract "fetch" methods:
      //

      /**
       * Fetch the attributes for the specified agent.
       * <p>
       * For example, to fetch the attributes for agent X:<pre>
       *   fetchAgent("X")
       * </pre>
       */
      protected abstract Attributes fetchAgent(String agent);

      /**
       * Fetch all names of the given entry type and where
       * the parent matches the specified parent name.
       * <p>
       * If the match is null then all elements match.
       */
      protected abstract Set fetchNames(int type, Attributes match);

      /**
       * Fetch all agent attributes which match the specified
       * filter.
       * <p>
       * For example, if the "match" is (node=X, host=Y), then<pre>
       *   1) find all agents on node X
       *   2) filter these agents to just those on host Y
       *   3) fetch the attributes for each matching agent
       *   4) return the list of attributes (one per agent)
       * </pre>
       */
      protected abstract List fetchAgents(Attributes match);

      //
      // denied timeout methods:
      //

      public void setTimeout(long timeout) {
        throw new UnsupportedOperationException();
      }

      public long getTimeout() {
        throw new UnsupportedOperationException();
      }

      public void setUseStale(boolean useStale) {
        throw new UnsupportedOperationException();
      }

      public boolean getUseStale() {
        throw new UnsupportedOperationException();
      }

      //
      // old "get*" variants, before we had caching
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
        String parentName;
        if ((childType & ANY_AGENT_TYPE) != 0) {
          // special case for agent lookup
          Attributes ats = fetchAgent(childName);
          parentName = (String) getAttributeValue(ats, parentAttr);
          if (parentName != null) {
            // confirm child type
            Object otype = 
              getAttributeValue(
                  ats, TopologyNamingConstants.TYPE_ATTR);
            int itype = 
              (otype != null ? 
               Integer.parseInt((String) otype) :
               -1);
            if ((itype < 0) || 
                ((childType & itype) == 0)) {
              parentName = null;
            }
          }
        } else {
          // find the parent for a non-agent
          //
          // for now we something inefficient:
          //   we simply list all agents on that
          // child, then use any of the (all-same)
          // parents.  For example, to get the host
          // for node X, we list all agents on node X,
          // then grab the host from any agent.
          String childAttr = getAttributeName(childType);
          Attributes match = new BasicAttributes();
          match.put(childAttr, childName);
          List allAts = fetchAgents(match);
          Attributes firstAts =
            (((allAts != null) &&
              (allAts.size() > 0)) ?
             ((Attributes) allAts.get(0)) :
             null);
          parentName = (String)
            getAttributeValue(firstAts, parentAttr);
        }
        return parentName;
      }

      public Set lookupChildrenOnParent(
          int childType,
          int parentType,
          String parentName) {
        validate(childType, parentType);
        validateName(parentType, parentName);
        Attributes match = new BasicAttributes();
        String parentAttr = getAttributeName(parentType);
        match.put(parentAttr, parentName);
        return fetchNames(childType, match);
      }

      public Set lookupAll(int type) {
        validateType(type);
        Attributes match = new BasicAttributes();
        return fetchNames(type, match);
      }

      public long lookupIncarnationForAgent(String agent) {
        Attributes ats = fetchAgent(agent);
        String sinc = (String) getAttributeValue(ats,
            TopologyNamingConstants.INCARNATION_ATTR);
        return ((sinc != null) ? Long.parseLong(sinc) : -1L);
      }

      public TopologyEntry lookupEntryForAgent(String agent) {
        Attributes ats = fetchAgent(agent);
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

        List allAts = fetchAgents(match);
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

      protected String getAttributeName(int type) {
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

      // attribute utils:

      protected Object getAttributeValue(Attributes ats, String id) {
        return getAttributeValue(ats, id, false);
      }

      protected Object getAttributeValue(
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
    }

  private static abstract class UnsupportedTopology
    extends AbstractTopology {
      protected Attributes fetchAgent(String agent) {
        throw new UnsupportedOperationException();
      }
      protected Set fetchNames(int type, Attributes match) {
        throw new UnsupportedOperationException();
      }
      protected List fetchAgents(Attributes match) {
        throw new UnsupportedOperationException();
      }
    };

  private class CachedTopology
    extends AbstractTopology {

      //
      // timeouts are okay:
      //

      private long timeout = -1;
      private boolean useStale = true;

      public void setTimeout(long timeout) {
        this.timeout = timeout;
      }

      public long getTimeout() {
        return timeout;
      }

      public void setUseStale(boolean useStale) {
        this.useStale = useStale;
      }

      public boolean getUseStale() {
        return useStale;
      }

      //
      // fetches must access pass through the cache:
      //

      protected Attributes fetchAgent(String agent) {
        CacheKey key = new CacheKey(
            CacheKey.FETCH_AGENT,
            new Object[] {agent});
        return (Attributes) fetch(key);
      }

      protected Set fetchNames(int type, Attributes match) {
        CacheKey key = new CacheKey(
            CacheKey.FETCH_NAMES,
            new Object[] {new Integer(type), match});
        return (Set) fetch(key);
      }

      protected List fetchAgents(Attributes match) {
        CacheKey key = new CacheKey(
            CacheKey.FETCH_AGENTS,
            new Object[] {match});
        return (List) fetch(key);
      }

      private Object fetch(CacheKey key) {
        long threadId =
          (log.isDebugEnabled() ? 
           System.identityHashCode(Thread.currentThread()) :
           0);
        if (threadId != 0) {
          log.debug(
              "Fetch from cache by thread="+threadId+
              " of key="+key+
              " with timeout="+timeout+
              " and useStale="+useStale);
        }
        Cache.CacheEntry entry = 
          cache.fetch(
              key,
              (key.keyType != CacheKey.FETCH_AGENT),
              timeout);
        boolean hasValue;
        Object value;
        synchronized (entry) {
          if (entry.hasValue()) {
            hasValue = true;
            value = entry.getValue();
            if (useStale || !entry.isStale()) {
              if (threadId != 0) {
                log.debug(
                    "Fetched by thread="+threadId+
                    " of "+(entry.isStale() ? "stale" : "current")+
                    " value="+value);
              }
              return value;
            }
          } else {
            hasValue = false;
            value = null;
          }
        }
        if (threadId != 0) {
          log.debug(
              "Timeout by thread="+threadId+
              (hasValue ? 
               (" with value="+value) :
               (" without value")));
        }
        switch (key.keyType) {
          case CacheKey.FETCH_AGENT:
            throw new TopologyTimeoutAgentException(
                hasValue, value);
          case CacheKey.FETCH_NAMES:
            throw new TopologyTimeoutNamesException(
                hasValue, value);
          case CacheKey.FETCH_AGENTS:
            throw new TopologyTimeoutAgentsException(
                hasValue, value);
          default:
            throw new IllegalArgumentException(
                "Unknown key type: "+key.keyType);
        }
      }
    }

  private static class KeyedFetcher 
    implements Cache.Fetcher {
      private final AbstractTopology backer;

      public KeyedFetcher(AbstractTopology backer) {
        this.backer = backer;
      }

      public Object fetch(Object okey) {
        CacheKey key = (CacheKey) okey;
        switch (key.keyType) {
          case CacheKey.FETCH_AGENT:
            return 
              backer.fetchAgent(
                  ((String) key.keyElements[0]));
          case CacheKey.FETCH_NAMES:
            return 
              backer.fetchNames(
                  (((Integer) key.keyElements[0]).intValue()),
                  ((Attributes) key.keyElements[1]));
          case CacheKey.FETCH_AGENTS:
            return 
              backer.fetchAgents(
                  ((Attributes) key.keyElements[0]));
          default:
            throw new IllegalArgumentException(
                "Unknown key type: "+key.keyType);
        }
      }
    }

  private class UncachedTopology
    extends AbstractTopology {

      private final Object topologyContextLock = new Object();
      private EventDirContext topologyContext;

      private final NamingListener namingListener = new MyNamingListener();

      protected Attributes fetchAgent(String agent) {
        Attributes ret;
        try {
          EventDirContext ctx = getTopologyContext();
          ret = ctx.getAttributes(agent);
          if (ret == null &&
              log.isDebugEnabled()) {
            log.debug("Attributes missing for agent "+agent);
          }
        } catch (NamingException ne) {
          if (ne instanceof NameNotFoundException) {
            if (log.isDebugEnabled()) {
              log.debug("Unknown agent "+agent);
            }
            ret = null;
          } else {
            if (log.isDebugEnabled()) {
              log.debug("Unable to fetchAgent("+agent+")", ne);
            }
            throw new RuntimeException(
                "Unable to access name server to fetch agent "+agent,
                ne);
          }
        }
        return ret;
      }

      protected Set fetchNames(int type, Attributes match) {
        Set ret;
        String single_attr = getAttributeName(type);
        boolean isAgentFilter = 
          (((type & ANY_AGENT_TYPE) != 0) &&
           ((type != ANY_AGENT_TYPE)));
        try {
          NamingEnumeration e;
          // could optimize for "all agents" case:
          //  (type == ANY_AGENT_TYPE && match.size() == 0)
          // since NS keyset is the list of all agents.
          //
          if (isAgentFilter) {
            // need the name and type
            String[] ats_filter = { 
              single_attr, 
              TopologyNamingConstants.TYPE_ATTR,
            };
            e = getTopologyContext().search(
                "", match, ats_filter);
          } else {
            // only need the name
            String[] ats_filter = { single_attr };
            e = getTopologyContext().search(
                "", match, ats_filter);
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
              if (isAgentFilter) {
                // check agent-type filter
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
          if (log.isDebugEnabled()) {
            log.debug(
                "Unable to fetchNames("+type+", "+match+")",
                e);
          }
          throw new RuntimeException(
              "Unable to access name server", e);
        }
        return ret;
      }

      protected List fetchAgents(Attributes match) {
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
          if (log.isDebugEnabled()) {
            log.debug("Unable to fetchAgents("+match+")", e);
          }
          throw new RuntimeException(
              "Unable to access name server", e);
        }
        return ret;
      }

      private EventDirContext getTopologyContext() throws NamingException {
        synchronized (topologyContextLock) {
          if (topologyContext == null) {
            try {
              DirContext initialContext = (DirContext) 
                namingService.getRootContext();
              EventDirContext rootContext = (EventDirContext) 
                initialContext.lookup("");
              topologyContext = (EventDirContext)
                rootContext.lookup(TopologyNamingConstants.TOPOLOGY_DIR);
              topologyContext.addNamingListener(
                  "", EventContext.SUBTREE_SCOPE, namingListener);
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

      private void dirtyCache(String agentName) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Dirty cache for id="+System.identityHashCode(this)+
              " and agent="+agentName);
        }
        try {
          CacheKey key = new CacheKey(
              CacheKey.FETCH_AGENT,
              new Object[] {agentName});
          cache.dirty(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      private class MyNamingListener 
        implements NamespaceChangeListener, ObjectChangeListener {
          public void objectAdded(NamingEvent evt) {
            String name = evt.getNewBinding().getName();
            try {
              topologyContext.addNamingListener(
                  name, EventContext.OBJECT_SCOPE, namingListener);
              if (log.isDebugEnabled()) {
                log.debug(
                    "Installing NamingListener for added "+name+
                    " and id="+System.identityHashCode(
                      UncachedTopology.this));
              }
            } catch (NamingException ne) {
              log.error("Error installing naming listener", ne);
              return;
            }
            dirtyCache(name);
          }
          public void objectRemoved(NamingEvent evt) {
            String name = evt.getOldBinding().getName();
            dirtyCache(name);
          }
          public void objectRenamed(NamingEvent evt) {
            /*
               String name = evt.getNewBinding().getName();
               dirtyCache(name);
             */
          }
          public void objectChanged(NamingEvent evt) {
            String name = evt.getNewBinding().getName();
            dirtyCache(name);
          }
          public void namingExceptionThrown(NamingExceptionEvent evt) {
          }
        }
    }

  private static class CacheKey {
    public static final int FETCH_AGENT  = 1;
    public static final int FETCH_NAMES  = 2;
    public static final int FETCH_AGENTS = 3;
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

}
