/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.StateTuple;

/**
 * Hard-coded components for INI and DB configurations, which
 * have been moved into the XML reader's XSL templates.
 * <p>
 * This is all deprecated but preserved (for now) for backwards
 * compatibility. 
 */
abstract class DefaultComponents {

  private static final int HIGH      = ComponentDescription.PRIORITY_HIGH;
  private static final int INTERNAL  = ComponentDescription.PRIORITY_INTERNAL;
  private static final int BINDER    = ComponentDescription.PRIORITY_BINDER;
  private static final int COMPONENT = ComponentDescription.PRIORITY_COMPONENT;
  private static final int LOW       = ComponentDescription.PRIORITY_LOW;

  private DefaultComponents() {}

  public static List flattenComponents(
      ComponentDescriptions cds) {
    List l = new ArrayList();
    l.addAll(findComponents(cds, HIGH));
    l.addAll(findComponents(cds, INTERNAL));
    l.addAll(findComponents(cds, BINDER));
    l.addAll(findComponents(cds, COMPONENT));
    l.addAll(findComponents(cds, LOW));
    return l;
  }

  public static List getHardCodedComponents(
      ComponentDescriptions cds,
      boolean isNode) {

    List l = new ArrayList();

    // see "Bootstrap" class for initial component list

    l.add(EVENT_SERVICE);
    l.add(UID_SERVICE);

    // HIGH
    l.addAll(findComponents(cds, HIGH));

    l.add(ACQUIRE_IDENTITY);
    l.add(REHYDRATE_LATE);
    l.add(FIND_COMPONENTS_LATE);

    l.add(REGISTER_AGENT);

    if (isNode) {
      l.add(NODE_BUSY);
      l.add(CONFIG_SERVICE); // new since 11.2.0
      l.add(NODE_THREAD_SERVICE);
      l.add(MTS_SOCKET_FACTORY);

      // Split since 11.2.0
      if (SystemProperties.getBoolean("org.cougaar.core.load.wp", true)) {
	// Load WP Cache / Client pieces
	l.add(WP_RESOLVER_CONTAINER);
	l.add(WP_CONFIG_MANAGER);
	l.add(WP_SELECT_MANAGER);
	l.add(WP_CLIENT_TRANSPORT);
	l.add(WP_LEASE_MANAGER);
	l.add(WP_CACHE_MANAGER);
	l.add(WP_RESOLVER);
	l.add(WPBOOT_DISCOVERY_MANAGER);
	l.add(WPBOOT_CONFIGDISCOVERY);
	l.add(WPBOOT_MULTIDISCOVERY);
	l.add(WPBOOT_HTTPDISCOVERY);
	l.add(WPBOOT_RMIDISCOVERY);
	l.add(WPBOOT_ENSUREFOUND);	
      }

      if (SystemProperties.getBoolean("org.cougaar.metrics.trivial")) {
        l.add(TRIVIAL_QOS_METRICS_SERVICE);
      } else {
        l.add(QOS_METRICS_SERVICE);
      }
      l.add(NODE_METRICS);

      l.add(INCARNATION); // new since 11.2.0

      if (SystemProperties.getBoolean("org.cougaar.core.mts.singlenode")) {
        l.add(LOCAL_MESSAGE_TRANSPORT_SERVICE);
      } else {
        l.add(MESSAGE_TRANSPORT_SERVICE);
      }

      l.add(REAL_TIME_COMP); // Split into 2 is for 11.4
      l.add(NATURAL_TIME_COMP);

      if (SystemProperties.getBoolean("org.cougaar.core.load.servlet", true)) {
        l.add(SERVLET_SERVICE);
      }

      l.add(SUICIDE_SERVICE); // new since 11.2.0
    } // end of IfNode

    l.add(TOPOLOGY);
    l.add(RECONCILE);
    l.add(MESSAGE_SWITCH);
    l.add(QUEUE_HANDLER);
    l.add(MESSAGE_SWITCH_SHUTDOWN);
    l.add(RECONCILE_ENABLER);
    l.add(ALARM_COMPONENT);
    l.add(DEMO_CONTROL);

    // INTERNAL
    l.addAll(findComponents(cds, INTERNAL));

    // BINDER
    l.addAll(findComponents(cds, BINDER));

    if (isNode) {
      if (SystemProperties.getBoolean("org.cougaar.core.load.community", true)) {
        l.add(COMMUNITY_INITIALIZER_SERVICE);
      }
      if (SystemProperties.getBoolean("org.cougaar.core.load.planning", true)) {
        l.add(ASSET_INITIALIZER_SERVICE);
      }
    }

    l.add(THREAD_SERVICE);
    l.add(SCHEDULER_SERVICE);
    if (SystemProperties.getBoolean("org.cougaar.core.load.planning", true)) {
      l.add(PROTOTYPE_REGISTRY_SERVICE);
      l.add(LDM_SERVICE);
    }
    if (SystemProperties.getBoolean("org.cougaar.core.load.servlet", true)) {
      l.add(LEAF_SERVLET_SERVICE);
    }

    l.add(DOMAIN_MANAGER);
    if (SystemProperties.getBoolean("org.cougaar.core.load.community", true)) {
      l.add(COMMUNITY_SERVICE);
    }
    l.add(BLACKBOARD_SERVICE);

    // COMPONENT

    // WP Server must be loaded after leaf_servlet_service for
    // HTTP service advertising
    // This re-order & split is new since 11.2.0
    if (isNode) {
      if (SystemProperties.getBoolean("org.cougaar.core.load.wp.server", true)) {
	// Add WP Server Components
	l.add(WP_SERVER_CONTAINER);
	l.add(WP_SERVER_CONFIG_MANAGER);
	l.add(WP_SERVER_PEERS_MANAGER);
	l.add(WP_SERVER_TRANSPORT);
	l.add(WP_SERVER_ROOT_AUTH);
	l.add(WP_SERVER_AD_MANAGER);
	l.add(WP_SERVER_MULTI_ADVERTISE);
	l.add(WP_SERVER_HTTP_ADVERTISE);
	l.add(WP_SERVER_RMI_ADVERTISE);
      }
    }

    l.addAll(findComponents(cds, COMPONENT));

    l.add(PLUGIN_MANAGER);

    // LOW
    l.addAll(findComponents(cds, LOW));

    l.add(MESSAGE_SWITCH_UNPEND);
    l.add(EVENTS);
    l.add(END_LOGGER);

    if (isNode) {
      l.add(AGENT_LOADER); // FIXME: Needs arguments of agent names?
      l.add(HEARTBEAT);
    }

    return l;
  }

  /**
   * Find all agent-level components at a given priority level.
   */
  private static List findComponents(
      ComponentDescriptions descs,
      int priority) {
    return findComponents(
        descs,
        "Node.AgentManager.Agent.",
        priority);
  }

  private static List findComponents(
      ComponentDescriptions descs,
      String containmentPoint,
      int priority) {
    List ret = new ArrayList();
    if (descs == null) {
      return ret;
    }
    List pcd = descs.selectComponentDescriptions(priority);
    if (pcd == null) {
      return ret;
    }
    for (Iterator it = pcd.iterator(); it.hasNext(); ) {
      Object o = it.next();
      ComponentDescription cd =
        ((o instanceof StateTuple) ?
         (((StateTuple) o).getComponentDescription()) :
         ((ComponentDescription) o));
      String ip = cd.getInsertionPoint();
      if (ip.startsWith(containmentPoint) &&
          ip.indexOf(".", containmentPoint.length()+1) < 0) {
        ret.add(o);
      }
    }
    return ret;
  }

  // pre-HIGH

  private static final ComponentDescription EVENT_SERVICE =
    new ComponentDescription(
          "org.cougaar.core.agent.service.event.EventServiceComponent",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.service.event.EventServiceComponent",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription UID_SERVICE =
    new ComponentDescription(
          "org.cougaar.core.agent.service.uid.UIDServiceComponent",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.service.uid.UIDServiceComponent",
          null, null, null, null, null,
          HIGH);

  // HIGH
  
  private static final ComponentDescription ACQUIRE_IDENTITY =
    new ComponentDescription(
        "org.cougaar.core.agent.AcquireIdentity",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.AcquireIdentity",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription REHYDRATE_LATE =
    new ComponentDescription(
        "org.cougaar.core.agent.RehydrateLate",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.RehydrateLate",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription FIND_COMPONENTS_LATE =
    new ComponentDescription(
          "org.cougaar.core.agent.FindComponentsLate",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.FindComponentsLate",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription REGISTER_AGENT =
    new ComponentDescription(
        "org.cougaar.core.agent.RegisterAgent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.RegisterAgent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription NODE_BUSY =
    new ComponentDescription(
        "org.cougaar.core.node.NodeBusyComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.NodeBusyComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription CONFIG_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.node.ConfigurationServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.ConfigurationServiceComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription NODE_THREAD_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.thread.ThreadServiceProvider",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.thread.ThreadServiceProvider",
        null,
        Arrays.asList(
          new String[] {
            //("name=Node "+nodeName),
            "isRoot=true",
            "BestEffortAbsCapacity=30",
            "WillBlockAbsCapacity=300",
            "CpuIntenseAbsCapacity=2",
            "WellBehavedAbsCapacity=2",
          }),
        null, null, null,
        HIGH);

  private static final ComponentDescription MTS_SOCKET_FACTORY =
    new ComponentDescription(
        "org.cougaar.mts.base.SocketFactorySPC",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.mts.base.SocketFactorySPC",
        null, null, null, null, null,
        HIGH);

  // WP Client / Cache Components
  private static final ComponentDescription WP_RESOLVER_CONTAINER =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.ResolverContainer",
        "Node.AgentManager.Agent.WPClient",
        "org.cougaar.core.wp.resolver.ResolverContainer",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_CONFIG_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.ConfigManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.ConfigManager",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_SELECT_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.SelectManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.resolver.SelectManager",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_CLIENT_TRANSPORT =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.ClientTransport",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.resolver.ClientTransport",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_LEASE_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.LeaseManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.resolver.LeaseManager",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_CACHE_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.CacheManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.resolver.CacheManager",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WP_RESOLVER =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.Resolver",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.resolver.Resolver",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_DISCOVERY_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.DiscoveryManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.DiscoveryManager",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_CONFIGDISCOVERY =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.config.ConfigDiscovery",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.config.ConfigDiscovery",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_MULTIDISCOVERY =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.multicast.MulticastDiscovery",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.multicast.MulticastDiscovery",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_HTTPDISCOVERY =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.http.HttpDiscovery",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.http.HttpDiscovery",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_RMIDISCOVERY =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.rmi.RMIDiscovery",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.rmi.RMIDiscovery",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WPBOOT_ENSUREFOUND =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.EnsureIsFoundManager",
        "Node.AgentManager.Agent.WPClient.Component",
        "org.cougaar.core.wp.bootstrap.EnsureIsFoundManager",
        null, null, null, null, null,
        HIGH);
  // End of WP Client / Cache pieces

  // WP Server pieces
  private static final ComponentDescription WP_SERVER_CONTAINER =
    new ComponentDescription(
        "org.cougaar.core.wp.server.ServerContainer",
        "Node.AgentManager.Agent.WPServer",
        "org.cougaar.core.wp.server.ServerContainer",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_CONFIG_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.ConfigManager",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.ConfigManager",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_PEERS_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.PeersManager",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.PeersManager",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_TRANSPORT =
    new ComponentDescription(
        "org.cougaar.core.wp.server.ServerTransport",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.server.ServerTransport",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_ROOT_AUTH =
    new ComponentDescription(
        "org.cougaar.core.wp.server.RootAuthority",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.server.RootAuthority",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_AD_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.AdvertiseManager",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.AdvertiseManager",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_MULTI_ADVERTISE =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.multicast.MulticastAdvertise",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.multicast.MulticastAdvertise",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_HTTP_ADVERTISE =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.http.HttpAdvertise",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.http.HttpAdvertise",
        null, null, null, null, null,
        COMPONENT);

  private static final ComponentDescription WP_SERVER_RMI_ADVERTISE =
    new ComponentDescription(
        "org.cougaar.core.wp.bootstrap.rmi.RMIAdvertise",
        "Node.AgentManager.Agent.WPServer.Component",
        "org.cougaar.core.wp.bootstrap.rmi.RMIAdvertise",
        null, null, null, null, null,
        COMPONENT);
  // End of WP Server pieces

  private static final ComponentDescription TRIVIAL_QOS_METRICS_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.qos.metrics.MetricsServiceProvider",
        "Node.AgentManager.Agent.MetricsServices",
        "org.cougaar.core.qos.metrics.MetricsServiceProvider",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription QOS_METRICS_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.qos.rss.RSSMetricsServiceProvider",
        "Node.AgentManager.Agent.MetricsServices",
        "org.cougaar.core.qos.rss.RSSMetricsServiceProvider",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription NODE_METRICS =
    new ComponentDescription(
        "org.cougaar.core.node.NodeMetrics",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.NodeMetrics",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription INCARNATION =
    new ComponentDescription(
        "org.cougaar.core.node.Incarnation",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.Incarnation",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription LOCAL_MESSAGE_TRANSPORT_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.mts.singlenode.SingleNodeMTSProvider",
        "Node.AgentManager.Agent.MessageTransport",
        "org.cougaar.core.mts.singlenode.SingleNodeMTSProvider",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription MESSAGE_TRANSPORT_SERVICE =
    new ComponentDescription(
        "org.cougaar.mts.base.MessageTransportServiceProvider",
        "Node.AgentManager.Agent.MessageTransport",
        "org.cougaar.mts.base.MessageTransportServiceProvider",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription REAL_TIME_COMP =
    new ComponentDescription(
        "org.cougaar.core.node.RealTimeComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.RealTimeComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription NATURAL_TIME_COMP =
    new ComponentDescription(
        "org.cougaar.core.node.NaturalTimeComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.NaturalTimeComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription SERVLET_SERVICE =
    new ComponentDescription(
        "org.cougaar.lib.web.service.RootServletServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.lib.web.service.RootServletServiceComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription SUICIDE_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.node.SuicideServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.SuicideServiceComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription TOPOLOGY =
    new ComponentDescription(
        "org.cougaar.core.agent.Topology",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.Topology",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription RECONCILE =
    new ComponentDescription(
        "org.cougaar.core.agent.Reconcile",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.Reconcile",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription MESSAGE_SWITCH =
    new ComponentDescription(
        "org.cougaar.core.agent.MessageSwitch",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.MessageSwitch",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription QUEUE_HANDLER =
    new ComponentDescription(
        "org.cougaar.core.agent.QueueHandler",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.QueueHandler",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription MESSAGE_SWITCH_SHUTDOWN =
    new ComponentDescription(
        "org.cougaar.core.agent.MessageSwitchShutdown",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.MessageSwitchShutdown",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription RECONCILE_ENABLER =
    new ComponentDescription(
        "org.cougaar.core.agent.ReconcileEnabler",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.ReconcileEnabler",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription ALARM_COMPONENT =
    new ComponentDescription(
        "org.cougaar.core.agent.AlarmComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.AlarmComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription DEMO_CONTROL =
    new ComponentDescription(
        "org.cougaar.core.agent.DemoControl",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.DemoControl",
        null, null, null, null, null,
        HIGH);

  // INTERNAL
 
  // BINDER

  private static final ComponentDescription COMMUNITY_INITIALIZER_SERVICE =
    new ComponentDescription(
        "org.cougaar.community.init.CommunityInitializerServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.community.init.CommunityInitializerServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription ASSET_INITIALIZER_SERVICE =
    new ComponentDescription(
        "org.cougaar.planning.ldm.AssetInitializerServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.planning.ldm.AssetInitializerServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription THREAD_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.thread.ThreadServiceProvider",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.thread.ThreadServiceProvider",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription SCHEDULER_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription PROTOTYPE_REGISTRY_SERVICE =
    new ComponentDescription(
        "org.cougaar.planning.ldm.PrototypeRegistryServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.planning.ldm.PrototypeRegistryServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription LDM_SERVICE =
    new ComponentDescription(
        "org.cougaar.planning.ldm.LDMServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.planning.ldm.LDMServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription LEAF_SERVLET_SERVICE =
    new ComponentDescription(
        "org.cougaar.lib.web.service.LeafServletServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.lib.web.service.LeafServletServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription DOMAIN_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.domain.DomainManager",
        "Node.AgentManager.Agent.DomainManager",
        "org.cougaar.core.domain.DomainManager",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription COMMUNITY_SERVICE =
    new ComponentDescription(
        "org.cougaar.community.CommunityServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.community.CommunityServiceComponent",
        null, null, null, null, null,
        BINDER);

  private static final ComponentDescription BLACKBOARD_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.blackboard.StandardBlackboard",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.blackboard.StandardBlackboard",
        null, null, null, null, null,
        BINDER);

  // COMPONENT

  private static final ComponentDescription PLUGIN_MANAGER =
    new ComponentDescription(
        "org.cougaar.core.plugin.PluginManager",
        "Node.AgentManager.Agent.PluginManager",
        "org.cougaar.core.plugin.PluginManager",
        null, null, null, null, null,
        LOW);

  // LOW

  private static final ComponentDescription MESSAGE_SWITCH_UNPEND =
    new ComponentDescription(
          "org.cougaar.core.agent.MessageSwitchUnpend",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.MessageSwitchUnpend",
          null, null, null, null, null,
          LOW);

  private static final ComponentDescription EVENTS =
    new ComponentDescription(
          "org.cougaar.core.agent.Events",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.Events",
          null, null, null, null, null,
          LOW);

  private static final ComponentDescription END_LOGGER =
    new ComponentDescription(
          "org.cougaar.core.agent.EndLogger",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.EndLogger",
          null, null, null, null, null,
          LOW);

  private static final ComponentDescription AGENT_LOADER =
    new ComponentDescription(
        "org.cougaar.core.node.AgentLoader",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.AgentLoader",
        null, null, null, null, null,
        LOW);

  private static final ComponentDescription HEARTBEAT =
    new ComponentDescription(
        "org.cougaar.core.node.HeartbeatComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.HeartbeatComponent",
        null, null, null, null, null,
        LOW);
}
