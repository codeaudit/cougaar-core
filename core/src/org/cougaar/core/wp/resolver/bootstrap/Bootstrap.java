/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver.bootstrap;

import java.net.URL;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.resolver.Handler;
import org.cougaar.core.wp.resolver.HandlerRegistryService;
import org.cougaar.core.wp.resolver.Resolver; // inlined

/**
 * This is the resolver bootstrap cache, which includes
 * subcomponents to:<ul>
 *   <li>maintain a table of bootstrap entries</li>
 *   <li>read bootstrap config files</li>
 *   <li>resolve symbolic boostrap entries (rmi)</li>
 * </ul>
 * <p>
 * This component advertises the TableService for its child
 * components to use.
 */
public class Bootstrap
extends ContainerSupport
{
  public static final String INSERTION_POINT = 
    Resolver.INSERTION_POINT + ".Bootstrap";

  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  private ServiceProvider tableSP;

  private HandlerRegistryService hrs;

  private final BindWatchers bindWatchers = new BindWatchers();

  private final EntryTable table = new EntryTable();

  private final Handler myHandler = 
    new Handler() {
      public Response submit(Response res) {
        return Bootstrap.this.mySubmit(res);
      }
      public void execute(Request req, Object result, long ttl) {
        Bootstrap.this.myExecute(req, result, ttl);
      }
    };

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(
      AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults
    l.add(new ComponentDescription(
            "Config",
            Bootstrap.INSERTION_POINT+".conf",
            "org.cougaar.core.wp.resolver.bootstrap.ConfigReader",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "RMI",
            Bootstrap.INSERTION_POINT+".rmi",
            "org.cougaar.core.wp.resolver.bootstrap.RMIBootstrapLookup",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

    // read config
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            agentId.toString(),
            specifyContainmentPoint());
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+agentId+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    return new ComponentDescriptions(l);
  }

  public void load() {
    if (logger.isDebugEnabled()) {
      logger.debug("Loading bootstrap");
    }

    hrs = (HandlerRegistryService)
      getServiceBroker().getService(
          this, HandlerRegistryService.class, null);
    if (hrs == null) {
      throw new RuntimeException(
          "Unable to obtain HandlerRegistryService");
    }
    hrs.register(myHandler);

    tableSP = new TableSP();
    getChildServiceBroker().addService(TableService.class, tableSP);

    super.load();
  }

  public void unload() {
    super.unload();

    // release services
    ServiceBroker sb = getServiceBroker();
    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    if (hrs != null) {
      hrs.unregister(myHandler);
      sb.releaseService(
          this, HandlerRegistryService.class, hrs);
    }

    if (tableSP != null) {
      getChildServiceBroker().revokeService(
          TableService.class, tableSP);
      table.clear();
      bindWatchers.clear();
      tableSP = null;
    }
  }

  public Response mySubmit(Response res) {
    Request req = res.getRequest();
    if (logger.isDebugEnabled()) {
      logger.debug("Bootstrap intercept wp request: "+req);
    }
    if (req instanceof Request.Get) {
      Request.Get greq = (Request.Get) req;
      String name = greq.getName();
      String type = greq.getType();
      AddressEntry bootAE = table.get(name, type);
      if (bootAE != null) {
        res.setResult(bootAE, 30000);
      }
    } else if (req instanceof Request.Bind) {
      Request.Bind bin = (Request.Bind) req;
      AddressEntry ae = bin.getAddressEntry();
      boolean overWrite = bin.isOverWrite();
      if (overWrite) {
        bindWatchers.rebind(ae);
      } else {
        bindWatchers.bind(ae);
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind unb = (Request.Unbind) req;
      AddressEntry ae = unb.getAddressEntry();
      bindWatchers.unbind(ae);
    } else {
      // ignore
    }
    return res;
  }

  public void myExecute(Request req, Object result, long ttl) {
    // no-op?
  }

  /**
   * Intercepts binds and tells the bind watchers.
   */
  private static class BindWatchers {

    // Set<TableService.BindWatcher>
    private final Set bindWatchers = new IdentityHashSet();

    public void register(TableService.BindWatcher bw) {
      synchronized (bindWatchers) {
        bindWatchers.add(bw);
      }
    }

    public void unregister(TableService.BindWatcher bw) {
      synchronized (bindWatchers) {
        bindWatchers.remove(bw);
      }
    }

    private void clear() {
      synchronized (bindWatchers) {
        bindWatchers.clear();
      }
    }

    public void bind(AddressEntry entry) {
      synchronized (bindWatchers) {
        for (Iterator iter = bindWatchers.iterator();
            iter.hasNext();
            ) {
          TableService.BindWatcher bw = 
            (TableService.BindWatcher) iter.next();
          bw.bind(entry);
        }
      }
    }

    public void rebind(AddressEntry entry) {
      synchronized (bindWatchers) {
        for (Iterator iter = bindWatchers.iterator();
            iter.hasNext();
            ) {
          TableService.BindWatcher bw = 
            (TableService.BindWatcher) iter.next();
          bw.rebind(entry);
        }
      }
    }

    public void unbind(AddressEntry entry) {
      synchronized (bindWatchers) {
        for (Iterator iter = bindWatchers.iterator();
            iter.hasNext();
            ) {
          TableService.BindWatcher bw = 
            (TableService.BindWatcher) iter.next();
          bw.unbind(entry);
        }
      }
    }
  }

  /**
   * Holds the bootstrap entries and tells the table watchers
   * when the entries change.
   */
  private static class EntryTable {
    // Map<String><Map<String,AddressEntry>>>
    private final Map entries = new HashMap();

    // Set<TableService.TableWatcher>
    private final Set tableWatchers = new IdentityHashSet();

    public AddressEntry get(String name, String type) {
      synchronized (entries) {
        Map m = (Map) entries.get(name);
        if (m != null) {
          return (AddressEntry) m.get(type);
        }
        return null;
      }
    }

    public Iterator iterator() {
      return entries().iterator();
    }

    // List<AddressEntry>
    private List entries() {
      synchronized (entries) {
        List ret = new ArrayList(entries.size());
        for (Iterator iter = entries.values().iterator();
            iter.hasNext();
            ) {
          Map m = (Map) iter.next();
          ret.addAll(m.values());
        }
        return ret;
      }
    }

    public void add(AddressEntry entry) {
      synchronized (entries) {
        String name = entry.getName();
        Map m = (Map) entries.get(name);
        if (m == null) {
          m = new HashMap(3);
          entries.put(name, m);
        }
        String type = entry.getType();
        m.put(type, entry);

        // tell table watchers
        for (Iterator iter = tableWatchers.iterator();
            iter.hasNext();
            ) {
          TableService.TableWatcher tw = 
            (TableService.TableWatcher) iter.next();
          tw.added(entry);
        }
      }
    }
    public void replace(AddressEntry oldE, AddressEntry newE) {
      // assert oldE.getName().equals(newE.getName());
      synchronized (entries) {
        if (oldE != null) {
          remove(oldE);
        }
        if (newE != null) {
          add(newE);
        }
      }
    }
    public void remove(AddressEntry entry) {
      synchronized (entries) {
        String name = entry.getName();
        Map m = (Map) entries.get(name);
        if (m != null) {
          String type = entry.getType();
          AddressEntry ae = (AddressEntry) m.get(type);
          if (entry.equals(ae)) {
            m.remove(type);
            if (m.isEmpty()) {
              entries.remove(name);
            }

            // tell table watchers
            for (Iterator iter = tableWatchers.iterator();
                iter.hasNext();
                ) {
              TableService.TableWatcher tw =
                (TableService.TableWatcher) iter.next();
              tw.removed(entry);
            }
          }
        }
      }
    }

    private void clear() {
      synchronized (entries) {
        entries.clear();

        // should we tell the watchers?  for now we assume
        // this is due to a component unload.
        tableWatchers.clear();
      }
    }

    public void register(TableService.TableWatcher tw) {
      synchronized (entries) {
        if (tableWatchers.add(tw)) {
          tw.init(entries());
        }
      }
    }

    public void unregister(TableService.TableWatcher tw) {
      synchronized (entries) {
        tableWatchers.remove(tw);
      }
    }
  }

  private class TableSP 
    implements ServiceProvider {
      private final TableService ts =
        new TableService() {
          public AddressEntry get(String name, String type) {
            return table.get(name, type);
          }
          public Iterator iterator() {
            return table.iterator();
          }
          public void add(AddressEntry entry) {
            table.add(entry);
          }
          public void replace(AddressEntry oldE, AddressEntry newE) {
            table.replace(oldE, newE);
          }
          public void remove(AddressEntry entry) {
            table.remove(entry);
          }
          public void register(Watcher w) {
            if (w instanceof TableService.BindWatcher) {
              bindWatchers.register((TableService.BindWatcher) w);
            }
            if (w instanceof TableService.TableWatcher) {
              table.register((TableService.TableWatcher) w);
            }
          }
          public void unregister(Watcher w) {
            if (w instanceof TableService.BindWatcher) {
              bindWatchers.unregister((TableService.BindWatcher) w);
            }
            if (w instanceof TableService.TableWatcher) {
              table.unregister((TableService.TableWatcher) w);
            }
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (TableService.class.isAssignableFrom(
              serviceClass)) {
          return ts;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  /**
   * Hash set based upon identity.
   * Should be in the JDK's utils...
   */
  public static class IdentityHashSet 
    extends AbstractSet
    implements Set
    {
      private final Map map = new IdentityHashMap();
      private static final Object PRESENT = new Object();
      public Iterator iterator() {
        return map.keySet().iterator();
      }
      public int size() {
        return map.size();
      }
      public boolean isEmpty() {
        return map.isEmpty();
      }
      public boolean contains(Object o) {
        return map.containsKey(o);
      }
      public boolean add(Object o) {
        return map.put(o, PRESENT)==null;
      }
      public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
      }
      public void clear() {
        map.clear();
      }
    }
}
