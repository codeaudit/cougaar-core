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
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.StateTuple;
import org.cougaar.util.PropertyParser;

/**
 * Hard-coded components, for use by agent boostrappers until the
 * file reader is completed (bug 2522).
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
      l.add(NODE_THREAD_SERVICE);
      l.add(MTS_SOCKET_FACTORY);
      if (PropertyParser.getBoolean("org.cougaar.core.load.wp", true)) {
        l.add(WHITE_PAGES_SERVICE);
      }
      if (PropertyParser.getBoolean("org.cougaar.core.load.wp.server", true)) {
        l.add(WHITE_PAGES_SERVER);
      }
      if (Boolean.getBoolean("org.cougaar.metrics.trivial")) {
        l.add(TRIVIAL_QOS_METRICS_SERVICE);
      } else {
        l.add(QOS_METRICS_SERVICE);
      }
      l.add(NODE_METRICS);
      if (Boolean.getBoolean("org.cougaar.core.mts.singlenode")) {
        l.add(LOCAL_MESSAGE_TRANSPORT_SERVICE);
      } else {
        l.add(MESSAGE_TRANSPORT_SERVICE);
      }
      l.add(TIME_SERVICES);
      if (PropertyParser.getBoolean("org.cougaar.core.load.servlet", true)) {
        l.add(SERVLET_SERVICE);
      }
    }

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
      if (PropertyParser.getBoolean("org.cougaar.core.load.community", true)) {
        l.add(COMMUNITY_INITIALIZER_SERVICE);
      }
      if (PropertyParser.getBoolean("org.cougaar.core.load.planning", true)) {
        l.add(ASSET_INITIALIZER_SERVICE);
      }
    }

    l.add(THREAD_SERVICE);
    l.add(SCHEDULER_SERVICE);
    if (PropertyParser.getBoolean("org.cougaar.core.load.planning", true)) {
      l.add(PROTOTYPE_REGISTRY_SERVICE);
      l.add(LDM_SERVICE);
    }
    if (PropertyParser.getBoolean("org.cougaar.core.load.servlet", true)) {
      l.add(LEAF_SERVLET_SERVICE);
    }
    l.add(DOMAIN_MANAGER);
    if (PropertyParser.getBoolean("org.cougaar.core.load.community", true)) {
      l.add(COMMUNITY_SERVICE);
    }
    l.add(BLACKBOARD_SERVICE);

    // COMPONENT
    l.addAll(findComponents(cds, COMPONENT));

    l.add(PLUGIN_MANAGER);

    // LOW
    l.addAll(findComponents(cds, LOW));

    l.add(MESSAGE_SWITCH_UNPEND);
    l.add(EVENTS);
    l.add(END_LOGGER);

    if (isNode) {
      l.add(AGENT_LOADER);
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
            "BestEffortAbsCapacity=300",
            "WillBlockAbsCapacity=30",
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

  private static final ComponentDescription WHITE_PAGES_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.wp.resolver.Resolver",
        "Node.AgentManager.Agent.WPClient",
        "org.cougaar.core.wp.resolver.Resolver",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription WHITE_PAGES_SERVER =
    new ComponentDescription(
        "org.cougaar.core.wp.server.Server",
        "Node.AgentManager.Agent.WPServer",
        "org.cougaar.core.wp.server.Server",
        null, null, null, null, null,
        HIGH);

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

  private static final ComponentDescription TIME_SERVICES =
    new ComponentDescription(
        "org.cougaar.core.node.TimeComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.TimeComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription SERVLET_SERVICE =
    new ComponentDescription(
        "org.cougaar.lib.web.service.RootServletServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.lib.web.service.RootServletServiceComponent",
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
