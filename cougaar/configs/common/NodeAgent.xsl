<?xml version="1.0"?>

<!--
<copyright>
 Copyright 2001-2006 BBNT Solutions, LLC
 under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 This program is free software; you can redistribute it and/or modify
 it under the terms of the Cougaar Open Source License as published by
 DARPA on the Cougaar Open Source Website (www.cougaar.org).

 THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 PERFORMANCE OF THE COUGAAR SOFTWARE.
</copyright>
-->

<!--
The standard node template, which defines which infrastructure
components to load into every node.

This file tells the node to load a message transport, naming service,
servlet engine, etc.

Note that every node has a node agent, so this template extends the
"SimpleAgent.xsl" template to populate the node-agent components.

For additional notes, see "SimpleAgent.xsl".
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <!-- extend SimpleAgent -->
  <xsl:import href="SimpleAgent.xsl"/>

  <!-- optional node-name filter -->
  <xsl:param name="node"/>

  <!--
  XSL parameters with defaults set by the "template" and "matrix" parameters
  defined in SimpleAgent.xsl.

  See SimpleAgent.xsl for details.
  -->
  <xsl:param name="mts" select="substring-before(substring-after($matrix, concat('mts', '=')), ',')"/>
  <xsl:param name="socketFactory" select="substring-before(substring-after($matrix, concat('socketFactory', '=')), ',')"/>
  <xsl:param name="standard_node_servlets" select="substring-before(substring-after($matrix, concat('standard_node_servlets', '=')), ',')"/>
  <xsl:param name="standard_aspects" select="substring-before(substring-after($matrix, concat('standard_aspects', '=')), ',')"/>
  <xsl:param name="link_protocol.loopback" select="substring-before(substring-after($matrix, concat('link_protocol.loopback', '=')), ',')"/>
  <xsl:param name="link_protocol.rmi" select="substring-before(substring-after($matrix, concat('link_protocol.rmi', '=')), ',')"/>
  <xsl:param name="link_protocol.jms" select="substring-before(substring-after($matrix, concat('link_protocol.jms', '=')), ',')"/>
  <xsl:param name="pluginThreadPool" select="substring-before(substring-after($matrix, concat('pluginThreadPool', '=')), ',')"/>
  <xsl:param name="servlet_engine.tomcat" select="substring-before(substring-after($matrix, concat('servlet_engine.tomcat', '=')), ',')"/>
  <xsl:param name="servlet_engine.micro" select="substring-before(substring-after($matrix, concat('servlet_engine.micro', '=')), ',')"/>
  <xsl:param name="servlet_engine.mts" select="substring-before(substring-after($matrix, concat('servlet_engine.mts', '=')), ',')"/>
  <xsl:param name="servlet_redirector.http_redirect" select="substring-before(substring-after($matrix, concat('servlet_redirector.http_redirect', '=')), ',')"/>
  <xsl:param name="servlet_redirector.http_tunnel" select="substring-before(substring-after($matrix, concat('servlet_redirector.http_tunnel', '=')), ',')"/>
  <xsl:param name="servlet_redirector.mts_tunnel" select="substring-before(substring-after($matrix, concat('servlet_redirector.mts_tunnel', '=')), ',')"/>

  <!--
  if a node "template" attribute is not specified, the "defaultNode"
  value is assumed, which defaults to this template file.
  -->
  <xsl:param name="defaultNode">NodeAgent.xsl</xsl:param>

  <xsl:output method="xml" indent="yes"/>

  <!-- match 'node' elements -->
  <xsl:template match="node[(@template = 'NodeAgent.xsl') or ($defaultNode = 'NodeAgent.xsl' and not(@template) and not(@type))]">
    <xsl:if test="not($node) or not(@name) or ($node = '*') or (@name = '*') or ($node = @name)">
      <node name="{@name}" template="">
        <!-- preserve facets, vm_parameters, etc -->
        <xsl:apply-templates select="node()[not(self::agent)]"/>
        <!-- get node-agent components -->
        <xsl:call-template name="NodeAgent_run"/>
        <!-- process child agents -->
        <xsl:apply-templates select="agent"/>
      </node>
    </xsl:if>
  </xsl:template>

  <!--
  Mix the config's componenents with both our node-agent specific
  components and the basic SimpleAgent components.
  -->
  <xsl:template name="NodeAgent_run">

    <xsl:call-template name="init_node"/>

    <xsl:call-template name="HIGH_agent_pre"/>
    <xsl:call-template name="HIGH_node_pre0"/>

    <xsl:call-template name="HIGH_agent_0"/>
    <xsl:call-template name="HIGH_config"/>
    <xsl:call-template name="HIGH_agent_1"/>

    <xsl:call-template name="HIGH_node_1b"/>

    <xsl:call-template name="HIGH_agent_2"/>

    <xsl:call-template name="HIGH_node_servlet_engine"/>

    <xsl:call-template name="INTERNAL_config"/>

    <xsl:call-template name="BINDER_config"/>
    
    <xsl:call-template name="BINDER_node_pre0"/>

    <xsl:call-template name="BINDER_agent_0"/>

    <xsl:call-template name="COMPONENT_agent_wp_server"/>

    <xsl:call-template name="COMPONENT_config"/>

    <xsl:call-template name="LOW_agent_plugins_0"/>
    <xsl:call-template name="LOW_node_plugins"/>
    <xsl:call-template name="LOW_agent_plugins_1"/>
    <xsl:call-template name="LOW_config"/>
    <xsl:call-template name="LOW_agent_1"/>

    <xsl:call-template name="LOW_node_1b"/>
  </xsl:template>

  <xsl:template name="init_node">
    <!--
    Support components that are siblings of agents, e.g.:
    <component .. insertionpoint="Node.AgentManager.Binder"/>

    This is the special agent binder (bug 1340), possibly the cause of
    bugs 3038, 3279, and others...
    -->
    <xsl:call-template name="findAll">
      <xsl:with-param name="insertionpoint" select="'Node.AgentManager.'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="HIGH_node_pre0">
    <!-- ConfigurationService -->
    <component
      name="org.cougaar.core.node.ConfigurationServiceComponent()"
      class="org.cougaar.core.node.ConfigurationServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- SuicideService -->
    <component 
      name="org.cougaar.core.node.SuicideServiceComponent()"
      class="org.cougaar.core.node.SuicideServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="HIGH_node_1b">
    <!-- thread service -->
    <xsl:choose>
      <xsl:when test="$threadService = 'trivial'">
        <!-- use trivial thread service, no limits -->
        <component
          name="org.cougaar.core.thread.TrivialThreadServiceProvider()"
          class="org.cougaar.core.thread.TrivialThreadServiceProvider"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:when>
      <xsl:when test="$threadService = 'full'">
        <!-- full thread service -->
        <component
          name="org.cougaar.core.thread.ThreadServiceProvider()"
          class="org.cougaar.core.thread.ThreadServiceProvider"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component">
          <argument>isRoot=true</argument>
          <argument>BestEffortAbsCapacity=<xsl:value-of select="$pluginThreadPool"/></argument>
          <argument>WillBlockAbsCapacity=300</argument>
          <argument>CpuIntenseAbsCapacity=2</argument>
          <argument>WellBehavedAbsCapacity=2</argument>
        </component>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'threadService'"/>
          <xsl:with-param name="value" select="$threadService"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="$socketFactory = 'true'">
      <!-- wp and mts socket factory -->
      <component
        name="org.cougaar.mts.base.SocketFactorySPC()"
        class="org.cougaar.mts.base.SocketFactorySPC"
        priority="HIGH"
        insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <xsl:choose>
      <xsl:when test="$wpserver = 'single_node' or $wpserver = 'singlenode'">
        <!-- loopback wp server-->
        <component
          name="org.cougaar.core.wp.LoopbackWhitePages()"
          class="org.cougaar.core.wp.LoopbackWhitePages"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:when>
      <xsl:when test="$wpserver = 'full' or $wpserver = 'false' or $wpserver = 'true'">
        <!-- distributed wp cache -->
        <component
          name="org.cougaar.core.wp.resolver.ResolverContainer()"
          class="org.cougaar.core.wp.resolver.ResolverContainer"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient"/>
        <component
          name="org.cougaar.core.wp.bootstrap.ConfigManager()"
          class="org.cougaar.core.wp.bootstrap.ConfigManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.resolver.SelectManager()"
          class="org.cougaar.core.wp.resolver.SelectManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.resolver.ClientTransport()"
          class="org.cougaar.core.wp.resolver.ClientTransport"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.resolver.LeaseManager()"
          class="org.cougaar.core.wp.resolver.LeaseManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.resolver.CacheManager()"
          class="org.cougaar.core.wp.resolver.CacheManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.resolver.Resolver()"
          class="org.cougaar.core.wp.resolver.Resolver"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.bootstrap.DiscoveryManager()"
          class="org.cougaar.core.wp.bootstrap.DiscoveryManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.bootstrap.config.ConfigDiscovery()"
          class="org.cougaar.core.wp.bootstrap.config.ConfigDiscovery"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <component
          name="org.cougaar.core.wp.bootstrap.multicast.MulticastDiscovery()"
          class="org.cougaar.core.wp.bootstrap.multicast.MulticastDiscovery"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component">
          <xsl:if test="$fast_startup = 'true'">
            <argument>minLookup=500</argument>
            <argument>maxLookup=2000</argument>
          </xsl:if>
        </component>
        <component
          name="org.cougaar.core.wp.bootstrap.http.HttpDiscovery()"
          class="org.cougaar.core.wp.bootstrap.http.HttpDiscovery"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component">
          <xsl:if test="$fast_startup = 'true'">
            <argument>minLookup=500</argument>
            <argument>maxLookup=2000</argument>
          </xsl:if>
        </component>
        <component
          name="org.cougaar.core.wp.bootstrap.rmi.RMIDiscovery()"
          class="org.cougaar.core.wp.bootstrap.rmi.RMIDiscovery"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component">
          <xsl:if test="$fast_startup = 'true'">
            <argument>minLookup=500</argument>
            <argument>maxLookup=2000</argument>
          </xsl:if>
        </component>
        <component
          name="org.cougaar.core.wp.bootstrap.EnsureIsFoundManager()"
          class="org.cougaar.core.wp.bootstrap.EnsureIsFoundManager"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.WPClient.Component"/>
        <xsl:call-template name="findAll">
          <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.WPClient.'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'wpserver'"/>
          <xsl:with-param name="value" select="$wpserver"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

    <!-- metrics -->
    <xsl:choose>
      <xsl:when test="$metrics = 'trivial'">
        <!-- use trivial metrics service -->
        <component
           name="org.cougaar.core.qos.metrics.MetricsServiceProvider()"
           class="org.cougaar.core.qos.metrics.MetricsServiceProvider"
           priority="HIGH"
           insertionpoint="Node.AgentManager.Agent.MetricsServices"/>
      </xsl:when>
      <xsl:when test="$metrics = 'full'">
        <!-- full metrics -->
        <component
           name="org.cougaar.core.qos.rss.RSSMetricsServiceProvider()"
           class="org.cougaar.core.qos.rss.RSSMetricsServiceProvider"
           priority="HIGH"
           insertionpoint="Node.AgentManager.Agent.MetricsServices"/>

        <!--load high-priority components, e.g. security -->
        <xsl:call-template name="find_HIGH_through_BINDER">
          <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.MetricsServices.'"/>
        </xsl:call-template>

        <xsl:if test="$standard_aspects = 'true'">
          <!-- standard metrics -->
          <component
            name='org.cougaar.core.qos.rss.AgentHostUpdaterComponent()'
            class='org.cougaar.core.qos.rss.AgentHostUpdaterComponent'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MetricsServices.Component'>
          </component>
          <component
            name='org.cougaar.core.qos.gossip.GossipFeedComponent()'
            class='org.cougaar.core.qos.gossip.GossipFeedComponent'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MetricsServices.Component'>
          </component>
        </xsl:if>

        <!--load any remaining metrics components-->
        <xsl:call-template name="find_COMPONENT_through_LOW">
          <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.MetricsServices.'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'metrics'"/>
          <xsl:with-param name="value" select="$metrics"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

    <component
      name="org.cougaar.core.node.NodeMetrics()"
      class="org.cougaar.core.node.NodeMetrics"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- incarnation tracker -->
    <component 
      name="org.cougaar.core.node.Incarnation()"
      class="org.cougaar.core.node.Incarnation"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- mts -->
    <xsl:choose>
      <xsl:when test="$mts = 'single_node' or $mts = 'singlenode'">
        <!-- use single-node mts -->
        <component
           name="org.cougaar.core.mts.singlenode.SingleNodeMTSProvider()"
           class="org.cougaar.core.mts.singlenode.SingleNodeMTSProvider"
           priority="HIGH"
           insertionpoint="Node.AgentManager.Agent.MessageTransport"/>
      </xsl:when>
      <xsl:when test="$mts = 'full'">
        <!-- full mts -->
        <component
           name="org.cougaar.mts.base.MessageTransportServiceProvider()"
           class="org.cougaar.mts.base.MessageTransportServiceProvider"
           priority="HIGH"
           insertionpoint="Node.AgentManager.Agent.MessageTransport"/>
        <!--
        TODO support $fast_startup by setting the equivalent of:
          -Dorg.cougaar.core.mts.destq.retry.initialTimeout=250
          -Dorg.cougaar.core.mts.destq.retry.maxTimeout=500
        This requires a modification to the mts, to make these component
        parameters instead of only system properties.
        -->

        <!--load high-priority components, e.g. security -->
        <xsl:call-template name="find_HIGH_through_BINDER">
          <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.MessageTransport.'"/>
        </xsl:call-template>

        <xsl:if test="$standard_aspects = 'true'">
          <!-- standard metrics and gossip aspects -->
           <component
            name='org.cougaar.mts.std.MessageTimeoutAspect'
            class='org.cougaar.mts.std.MessageTimeoutAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
           </component>
           <component
            name='org.cougaar.mts.std.WatcherAspect'
            class='org.cougaar.mts.std.WatcherAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.mts.std.AgentStatusAspect'
            class='org.cougaar.mts.std.AgentStatusAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.mts.std.MulticastAspect'
            class='org.cougaar.mts.std.MulticastAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.mts.std.StatisticsAspect()'
            class='org.cougaar.mts.std.StatisticsAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.mts.std.DeliveryVerificationAspect()'
            class='org.cougaar.mts.std.DeliveryVerificationAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
            <argument>info-time=9</argument>
            <argument>warn-time=99</argument>
          </component>
          <component
            name='org.cougaar.core.qos.gossip.GossipAspect()'
            class='org.cougaar.core.qos.gossip.GossipAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.core.qos.gossip.GossipStatisticsServiceAspect()'
            class='org.cougaar.core.qos.gossip.GossipStatisticsServiceAspect'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.core.qos.gossip.SimpleGossipQualifierComponent()'
            class='org.cougaar.core.qos.gossip.SimpleGossipQualifierComponent'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
          </component>
          <component
            name='org.cougaar.mts.std.DestinationThreadConstrictor()'
            class='org.cougaar.mts.std.DestinationThreadConstrictor'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
            <argument>MaxPerNode=2</argument>
            <argument>MaxThreads=15</argument>
          </component>
          <xsl:if test="$servlets = 'true'">
            <component
              name='org.cougaar.mts.std.DestinationQueueMonitorPlugin()'
              class='org.cougaar.mts.std.DestinationQueueMonitorPlugin'
              priority='COMPONENT'
              insertionpoint='Node.AgentManager.Agent.MessageTransport.Aspect'>
            </component>
          </xsl:if>
        </xsl:if>

        <!-- standard link protocols -->
        <xsl:if test="$link_protocol.loopback = 'true'">
          <component
            name='org.cougaar.mts.base.LoopbackLinkProtocol()'
            class='org.cougaar.mts.base.LoopbackLinkProtocol'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Component'/>	
        </xsl:if>
        <xsl:if test="$link_protocol.rmi = 'true'">
          <component
            name='org.cougaar.mts.rmi.RMILinkProtocol()'
            class='org.cougaar.mts.rmi.RMILinkProtocol'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Component'/>
        </xsl:if>
        <xsl:if test="$link_protocol.jms = 'true'">
          <component
            name='org.cougaar.mts.jms.JMSLinkProtocol()'
            class='org.cougaar.mts.jms.JMSLinkProtocol'
            priority='COMPONENT'
            insertionpoint='Node.AgentManager.Agent.MessageTransport.Component'/>	
        </xsl:if>

        <!--load any remaining mts components-->
        <xsl:call-template name="find_COMPONENT_through_LOW">
          <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.MessageTransport.'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'mts'"/>
          <xsl:with-param name="value" select="$mts"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

    <!-- alarms -->
    <component
      name="org.cougaar.core.node.RealTimeComponent()"
      class="org.cougaar.core.node.RealTimeComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
    <component
      name="org.cougaar.core.node.NaturalTimeComponent()"
      class="org.cougaar.core.node.NaturalTimeComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="HIGH_node_servlet_engine">
    <xsl:if test="$servlets = 'true'">

      <!-- servlet engine(s) -->
      <component
        name="org.cougaar.lib.web.engine.ServletEngineRegistry()"
        class="org.cougaar.lib.web.engine.ServletEngineRegistry"
        priority="HIGH"
        insertionpoint="Node.AgentManager.Agent.Component"/>
      <xsl:if test="$servlet_engine.tomcat = 'true'">
        <component
          name="org.cougaar.lib.web.tomcat.TomcatServletEngine()"
          class="org.cougaar.lib.web.tomcat.TomcatServletEngine"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>
      <xsl:if test="$servlet_engine.micro = 'true'">
        <component
          name="org.cougaar.lib.web.micro.http.HttpServletEngine()"
          class="org.cougaar.lib.web.micro.http.HttpServletEngine"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>
      <xsl:if test="$servlet_engine.mts = 'true'">
        <component
          name="org.cougaar.lib.web.micro.mts.MessagingServletEngine()"
          class="org.cougaar.lib.web.micro.mts.MessagingServletEngine"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>

      <!-- servlet redirectors/tunnels -->
      <component
        name="org.cougaar.lib.web.redirect.ServletRedirectorRegistry()"
        class="org.cougaar.lib.web.redirect.ServletRedirectorRegistry"
        priority="HIGH"
        insertionpoint="Node.AgentManager.Agent.Component"/>
      <xsl:if test="$servlet_redirector.http_redirect = 'true'">
        <component
          name="org.cougaar.lib.web.redirect.HttpServletRedirector()"
          class="org.cougaar.lib.web.redirect.HttpServletRedirector"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>
      <xsl:if test="$servlet_redirector.http_tunnel = 'true'">
        <component
          name="org.cougaar.lib.web.micro.http.HttpServletTunnel()"
          class="org.cougaar.lib.web.micro.http.HttpServletTunnel"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>
      <xsl:if test="$servlet_redirector.mts_tunnel = 'true'">
        <component
          name="org.cougaar.lib.web.micro.mts.MessagingServletTunnel()"
          class="org.cougaar.lib.web.micro.mts.MessagingServletTunnel"
          priority="HIGH"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:if>

      <!-- root-level servlet service -->
      <component
         name="org.cougaar.lib.web.service.RootServletServiceComponent()"
         class="org.cougaar.lib.web.service.RootServletServiceComponent"
         priority="HIGH"
         insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="BINDER_node_pre0">
    <!-- communities -->
    <xsl:choose>
      <xsl:when test="$communities = 'true'">
        <!-- shared node-level community service impl -->
        <component
          name="org.cougaar.community.CommunityServiceComponent()"
          class="org.cougaar.core.agent.service.community.CommunityServiceProvider"
          priority="BINDER"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:when>
      <xsl:when test="$communities = 'legacy'">
        <!-- community config reader -->
        <component
          name="org.cougaar.community.init.CommunityInitializerServiceComponent()"
          class="org.cougaar.community.init.CommunityInitializerServiceComponent"
          priority="BINDER"
          insertionpoint="Node.AgentManager.Agent.Component"/>
      </xsl:when>
      <xsl:when test="$communities = 'false' or $communities = ''">
        <!-- no community support -->
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'communities'"/>
          <xsl:with-param name="value" select="$communities"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>

    <!-- asset config reader -->
    <xsl:if test="$planning = 'true'">
      <component
         name="org.cougaar.planning.ldm.AssetInitializerServiceComponent()"
         class="org.cougaar.planning.ldm.AssetInitializerServiceComponent"
         priority="BINDER"
         insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <!-- busy indicator, to observe blackboard persistence -->
    <component
      name="org.cougaar.core.node.NodeBusyComponent()"
      class="org.cougaar.core.node.NodeBusyComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="LOW_node_plugins">
    <!-- metrics sensors -->
    <xsl:if test="$sensors = 'true' and $metrics = 'full'">
      <component
        name='org.cougaar.core.qos.metrics.AgentStatusRatePlugin()'
        class='org.cougaar.core.qos.metrics.AgentStatusRatePlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>
      <component
        name='org.cougaar.core.thread.AgentLoadSensorPlugin()'
        class='org.cougaar.core.thread.AgentLoadSensorPlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>
      <!-- must be loaded after the AgentLoadSensorPlugin -->
      <component
        name='org.cougaar.core.thread.AgentLoadRatePlugin()'
        class='org.cougaar.core.thread.AgentLoadRatePlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>
      <xsl:if test="$servlets = 'true'">
        <component
          name='org.cougaar.core.qos.gossip.GossipStatisticsPlugin()'
          class='org.cougaar.core.qos.gossip.GossipStatisticsPlugin'
          priority='COMPONENT'
          insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        </component>
        <component
          name='org.cougaar.mts.std.StatisticsPlugin()'
          class='org.cougaar.mts.std.StatisticsPlugin'
          priority='COMPONENT'
          insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        </component>
      </xsl:if>
    </xsl:if>

    <xsl:if test="$mobility = 'true'">
      <!-- agent mobility support -->
      <component
        name='org.cougaar.core.mobility.service.RootMobilityPlugin()'
        class='org.cougaar.core.mobility.service.RootMobilityPlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>
    </xsl:if>

    <xsl:if test="$servlets = 'true'">
      <!-- /favicon.ico -->
      <component
        name="org.cougaar.lib.web.service.FavIconServlet()"
        class="org.cougaar.lib.web.service.FavIconServlet"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <!-- optional servlets -->
    <xsl:if test="$standard_node_servlets = 'true' and $servlets = 'true'">
      <!-- thread activity view -->
      <component
        name='org.cougaar.core.thread.TopPlugin()'
        class='org.cougaar.core.thread.TopPlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>

      <!-- naming service view -->
      <component
        name='org.cougaar.core.wp.WhitePagesServlet(/wp)'
        class='org.cougaar.core.wp.WhitePagesServlet'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        <argument>/wp</argument>
      </component>

      <!-- logging level editor ("/log") -->
      <component
        name='org.cougaar.core.logging.LoggingConfigServlet()'
        class='org.cougaar.core.logging.LoggingConfigServlet'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>

      <!-- component model view -->
      <component
        name='org.cougaar.core.util.ComponentViewServlet(/components)'
        class='org.cougaar.core.util.ComponentViewServlet'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        <argument>/components</argument>
      </component>

      <xsl:if test="$metrics = 'full'">
        <!-- metrics service view -->
        <component
          name='org.cougaar.core.qos.metrics.MetricsServletPlugin()'
          class='org.cougaar.core.qos.metrics.MetricsServletPlugin'
          priority='COMPONENT'
          insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        </component>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="LOW_node_1b">
    <!-- child agents -->
    <component
      name="org.cougaar.core.node.AgentLoader()"
      class="org.cougaar.core.node.AgentLoader"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component">
      <xsl:for-each select="agent-decl">
        <argument>
          <xsl:value-of select="@name"/>
        </argument>
      </xsl:for-each>
      <xsl:for-each select="agent">
        <argument>
          <xsl:value-of select="@name"/>
        </argument>
      </xsl:for-each>
    </component>

    <!-- "." heartbeat printer -->
    <component
      name="org.cougaar.core.node.HeartbeatComponent()"
      class="org.cougaar.core.node.HeartbeatComponent"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

</xsl:stylesheet>
