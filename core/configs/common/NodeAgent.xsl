<?xml version="1.0"?>

<!--
<copyright>
 Copyright 2001-2004 BBNT Solutions, LLC
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
XSL Template for NodeAgent, which reuses most of SimpleAgent.
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <!-- extend SimpleAgent -->
  <xsl:import href="SimpleAgent.xsl"/>

  <!-- optional node-name filter -->
  <xsl:param name="node"/>

  <!-- optional wp server flag -->
  <xsl:param name="wpserver">true</xsl:param>

  <xsl:output method="xsl" indent="yes"/>

  <!-- match 'node' elements -->  
  <xsl:template match="node[(@template = 'NodeAgent.xsl') or (not(@template) and not(@type))]">
    <xsl:if test="not($node) or ($node = @name)">
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

    <xsl:call-template name="HIGH_agent_0"/>
    <xsl:call-template name="HIGH_config"/>
    <xsl:call-template name="HIGH_agent_1"/>

    <xsl:call-template name="HIGH_node_1b"/>

    <xsl:call-template name="HIGH_agent_2"/>

    <xsl:call-template name="INTERNAL_config"/>

    <xsl:call-template name="BINDER_config"/>
    
    <xsl:call-template name="BINDER_node_pre0"/>

    <xsl:call-template name="BINDER_agent_0"/>

    <xsl:call-template name="COMPONENT_config"/>

    <xsl:call-template name="LOW_agent_0"/>
    <xsl:call-template name="LOW_config"/>
    <xsl:call-template name="LOW_agent_1"/>

    <xsl:call-template name="LOW_node_1b"/>
  </xsl:template>

  <xsl:template name="init_node">
    <!--
    Support components that are siblings of agents, e.g.:
    <component .. insertionpoint="Node.AgentManager.Binder"/>

    This is the extra special agent binder (bug 1340), possibly the cause of
    bugs 3038, 3279, and others...
    -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority">HIGH</xsl:with-param>
      <xsl:with-param name="insertionpoint">Node.AgentManager.</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="HIGH_node_1b">
    <!-- busy indicator -->
    <component
      name="org.cougaar.core.node.NodeBusyComponent()"
      class="org.cougaar.core.node.NodeBusyComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- thread service -->
    <component
      name="org.cougaar.core.thread.ThreadServiceProvider()"
      class="org.cougaar.core.thread.ThreadServiceProvider"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component">
      <argument>isRoot=true</argument>
      <argument>BestEffortAbsCapacity=300</argument>
      <argument>WillBlockAbsCapacity=30</argument>
      <argument>CpuIntenseAbsCapacity=2</argument>
      <argument>WellBehavedAbsCapacity=2</argument>
    </component>

    <!-- wp and mts socket factory -->
    <component
      name="org.cougaar.mts.base.SocketFactorySPC()"
      class="org.cougaar.mts.base.SocketFactorySPC"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- wp cache -->
    <component
      name="org.cougaar.core.wp.resolver.Resolver()"
      class="org.cougaar.core.wp.resolver.Resolver"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.WPClient"/>

    <!--
    wp server
   
    For backwards compatibility we use a "wpserver" spreadsheet
    parameter to disable this component through a system property
    that's passed by the XML parser.
    -->
    <xsl:if test="$wpserver = 'true'">
      <component
        name="org.cougaar.core.wp.server.Server()"
        class="org.cougaar.core.wp.server.Server"
        priority="HIGH"
        insertionpoint="Node.AgentManager.Agent.WPServer"/>
    </xsl:if>

    <!-- metrics -->
    <!--
    <component
      name="org.cougaar.core.qos.metrics.MetricsServiceProvider()"
      class="org.cougaar.core.qos.metrics.MetricsServiceProvider"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.MetricsServices"/>
    -->
    <component
      name="org.cougaar.core.qos.rss.RSSMetricsServiceProvider()"
      class="org.cougaar.core.qos.rss.RSSMetricsServiceProvider"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.MetricsServices"/>
    <xsl:call-template name="findAll">
      <xsl:with-param name="insertionpoint">Node.AgentManager.Agent.MetricsServices.</xsl:with-param>
    </xsl:call-template>
    <component
      name="org.cougaar.core.node.NodeMetrics()"
      class="org.cougaar.core.node.NodeMetrics"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- mts -->
    <!--
    <component
      name="org.cougaar.core.mts.singlenode.SingleNodeMTSProvider()"
      class="org.cougaar.core.mts.singlenode.SingleNodeMTSProvider"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.MessageTransport"/>
    -->
    <component
      name="org.cougaar.mts.base.MessageTransportServiceProvider()"
      class="org.cougaar.mts.base.MessageTransportServiceProvider"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.MessageTransport"/>
    <xsl:call-template name="findAll">
      <xsl:with-param name="insertionpoint">Node.AgentManager.Agent.MessageTransport.</xsl:with-param>
    </xsl:call-template>

    <!-- alarms -->
    <component
      name="org.cougaar.core.node.TimeComponent()"
      class="org.cougaar.core.node.TimeComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- servlets -->
    <component
      name="org.cougaar.lib.web.service.RootServletServiceComponent()"
      class="org.cougaar.lib.web.service.RootServletServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="BINDER_node_pre0">
    <!-- community config reader -->
    <component
      name="org.cougaar.community.init.CommunityInitializerServiceComponent()"
      class="org.cougaar.community.init.CommunityInitializerServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- asset config reader -->
    <component
      name="org.cougaar.planning.ldm.AssetInitializerServiceComponent()"
      class="org.cougaar.planning.ldm.AssetInitializerServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="LOW_node_1b">
    <!-- child agents -->
    <component
      name="org.cougaar.core.node.AgentLoader()"
      class="org.cougaar.core.node.AgentLoader"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component">
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
