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
The standard agent template, which defines which infrastructure
components to load into every agent.

This file tells the agent to load a blackboard, message listener, etc.
It also tells the agent to load any components specified in the XML
file, and to load them in the appropriate order and insertion point.
The sequencing is very precise to ensure that services are advertised
and enabled in the correct dependency order.

A developer can create an alternate template and specify it in the society
XML file with an optional "template" parameter:
  <agent name="X" template="MyAgent.xsl">
     ..
  </agent>
Or, a developer can modify this template to add custom components and/or
define parameters to enable/disable components.

The XSL template design is documented in the Cougaar Developers' Guide's
section titled "XSL Agent Templates".
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <xsl:import href="util.xsl"/>

  <!--
  The 'template' parameter is a high-level parameter which enables/disables
  several parameters:
    -Dorg.cougaar.society.xsl.param.template=$value

  The supported values are:

    embedded     = applets and other embedded environments.

    single_node  = adds standard plugins and servlets (e.g. "/tasks" servlet)

    single_debug = distributed mts, loopback wp, added metrics aspects,
                   adds standard plugins and servlets (e.g. "/tasks" servlet)

    legacy       = backwards-compatible with prior Cougaar releases.
                   distributed mts/wp, adds planning and communities.

    lan          = distributed mts/wp, added metrics aspects,
                   adds standard plugins and servlets (e.g. "/wp" servlet)

    wan          = LAN plus communities and JMS

  -->
  <xsl:param name="template">legacy</xsl:param>

  <!--
  Our template definitions.

  Note that each line must be formatted as
    $name=$value,
  with no whitespace between the = and , characters.

  Boolean parameters that default to 'false' have been omitted, since all of
  our xsl:if tests check for
    $value = 'true'
  -->
  <xsl:param name="matrix">
    <xsl:choose>
      <xsl:when test="$template = 'embedded'">
        threadService=trivial,
        mts=single_node,
        wpserver=single_node,
        metrics=trivial,
      </xsl:when>
      <xsl:when test="$template = 'single_node'">
        threadService=full,
        pluginThreadPool=30,
        mts=single_node,
        wpserver=single_node,
        metrics=trivial,
        servlets=true,
        standard_agent_servlets=true,
        standard_node_servlets=true,
        servlet_engine.tomcat=true,
        servlet_redirector.http_redirect=true,
      </xsl:when>
      <!-- Note: 
	   Do *not* change threadService=full to trivial unless you also change to metrics=trivial
      -->
      <xsl:when test="$template = 'single_debug'">
        threadService=full,
        pluginThreadPool=30,
        socketFactory=true,
        mts=full,
        link_protocol.loopback=true,
        wpserver=single_node,
        metrics=full,
        sensors=true,
        servlets=true,
        standard_agent_servlets=true,
        standard_node_servlets=true,
        standard_aspects=true,
        servlet_engine.tomcat=true,
        servlet_redirector.http_redirect=true,
      </xsl:when>
      <xsl:when test="$template = 'legacy'">
        threadService=full,
        pluginThreadPool=30,
        socketFactory=true,
        mts=full,
        wpserver=true,
        metrics=full,
        servlets=true,
        planning=true,
        domain_ini=true,
        communities=legacy,
        servlet_engine.tomcat=true,
        servlet_redirector.http_redirect=true,
      </xsl:when>
      <xsl:when test="$template = 'lan'">
        threadService=full,
        pluginThreadPool=30,
        socketFactory=true,
        mts=full,
        wpserver=full,
        metrics=full,
        sensors=true,
        servlets=true,
        mobility=true,
        standard_agent_servlets=true,
        standard_node_servlets=true,
        standard_aspects=true,
        servlet_engine.tomcat=true,
        servlet_engine.mts=true,
        servlet_redirector.http_redirect=true,
        servlet_redirector.mts_tunnel=true,
      </xsl:when>
      <xsl:when test="$template = 'wan'">
        threadService=full,
        pluginThreadPool=30,
        socketFactory=true,
        mts=full,
        link_protocol.loopback=true,
        link_protocol.rmi=true,
        link_protocol.jms=true,
        wpserver=full,
        metrics=full,
        sensors=true,
        servlets=true,
        mobility=true,
        communities=true,
        standard_agent_servlets=true,
        standard_node_servlets=true,
        standard_aspects=true,
        servlet_engine.tomcat=true,
        servlet_engine.mts=true,
        servlet_redirector.http_redirect=true,
        servlet_redirector.mts_tunnel=true,
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="logBadParameter">
          <xsl:with-param name="name" select="'template'"/>
          <xsl:with-param name="value" select="$template"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>

  <!--
  The following parameters are based on the above "matrix" table.

  They can be overwritten by setting:
    -Dorg.cougaar.society.xsl.param.$name=$value

  For backwards compatibility, the following parameter:
    -Dorg.cougaar.core.load.wp.server=$value
  is translated into:
    -Dorg.cougaar.society.xsl.param.wpserver=$value

  XSLT 1.0 lacks xsl:function support, so the select= expressions are identical
  except for the parameter name.
  -->
  <xsl:param name="threadService" select="substring-before(substring-after($matrix, concat('threadService', '=')), ',')"/>
  <xsl:param name="wpserver" select="substring-before(substring-after($matrix, concat('wpserver', '=')), ',')"/>
  <xsl:param name="metrics" select="substring-before(substring-after($matrix, concat('metrics', '=')), ',')"/>
  <xsl:param name="sensors" select="substring-before(substring-after($matrix, concat('sensors', '=')), ',')"/>
  <xsl:param name="servlets" select="substring-before(substring-after($matrix, concat('servlets', '=')), ',')"/>
  <xsl:param name="planning" select="substring-before(substring-after($matrix, concat('planning', '=')), ',')"/>
  <xsl:param name="domain_ini" select="substring-before(substring-after($matrix, concat('domain_ini', '=')), ',')"/>
  <xsl:param name="communities" select="substring-before(substring-after($matrix, concat('communities', '=')), ',')"/>
  <xsl:param name="mobility" select="substring-before(substring-after($matrix, concat('mobility', '=')), ',')"/>
  <xsl:param name="standard_agent_servlets" select="substring-before(substring-after($matrix, concat('standard_agent_servlets', '=')), ',')"/>
  <xsl:param name="fast_startup" select="substring-before(substring-after($matrix, concat('fast_startup', '=')), ',')"/>

  <!--
  if an agent "template" attribute is not specified, the "defaultAgent"
  value is assumed, which defaults to this template file.
  -->
  <xsl:param name="defaultAgent">SimpleAgent.xsl</xsl:param>

  <xsl:strip-space elements="*"/>

  <xsl:output method="xml" indent="yes"/>

  <!-- default template to preserve all tags -->
  <xsl:template match="@*|node()|processing-instruction()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|processing-instruction()|comment()"/>
    </xsl:copy>
  </xsl:template>

  <!-- we control component tags -->
  <xsl:template match="component"/>

  <!-- match 'agent' elements of type SimpleAgent -->
  <xsl:template match="agent[(@template = 'SimpleAgent.xsl') or ($defaultAgent = 'SimpleAgent.xsl' and not(@template) and not(@type))]">
    <agent name="{@name}" template="">
      <!-- keep facets & comments -->
      <xsl:apply-templates/>
      <!-- get components -->
      <xsl:call-template name="SimpleAgent_run"/>
    </agent>
  </xsl:template>

  <!--
  Mix the config's components with our template's components.

  The components are split into templates to support easy
  "subclassing" of this templates, e.g. "NodeAgent.xsl".
  -->
  <xsl:template name="SimpleAgent_run">
    <xsl:call-template name="HIGH_agent_pre"/>

    <xsl:call-template name="HIGH_agent_0"/>
    <xsl:call-template name="HIGH_config"/>
    <xsl:call-template name="HIGH_agent_1"/>
    <xsl:call-template name="HIGH_agent_2"/>

    <xsl:call-template name="INTERNAL_config"/>

    <xsl:call-template name="BINDER_config"/>
    <xsl:call-template name="BINDER_agent_0"/>

    <xsl:call-template name="COMPONENT_agent_wp_server"/>

    <xsl:call-template name="COMPONENT_config"/>

    <xsl:call-template name="LOW_agent_plugins_0"/>
    <xsl:call-template name="LOW_agent_plugins_1"/>
    <xsl:call-template name="LOW_config"/>
    <xsl:call-template name="LOW_agent_1"/>
  </xsl:template>

  <!-- named templates, which simply group components -->

  <xsl:template name="HIGH_agent_pre">
    <!-- agent-id service -->
    <component
      name="org.cougaar.core.agent.service.id.AgentIdentificationServiceComponent()"
      class="org.cougaar.core.agent.service.id.AgentIdentificationServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component">
      <!-- only the first agent-level component supports this "$AGENT_NAME" arg. -->
      <argument>$AGENT_NAME</argument>
    </component>

    <!-- block the NodeControlService if (agent-id != node-id) -->
    <component
      name="org.cougaar.core.agent.NodeControlBlocker()"
      class="org.cougaar.core.agent.NodeControlBlocker"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- logging service, which wraps Log4j -->
    <component
      name="org.cougaar.core.node.LoggingServiceComponent()"
      class="org.cougaar.core.node.LoggingServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- node-level blackboard quiescence monitor, disabled in agents -->
    <component
      name="org.cougaar.core.node.QuiescenceReportComponent()"
      class="org.cougaar.core.node.QuiescenceReportComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- loading/starting/etc logger -->
    <component
      name="org.cougaar.core.agent.BeginLogger()"
      class="org.cougaar.core.agent.BeginLogger"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- persistence -->
    <component
      name="org.cougaar.core.persist.PersistenceServiceComponent()"
      class="org.cougaar.core.persist.PersistenceServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- register agent in the static deserializer table (mts, etc) -->
    <component
      name="org.cougaar.core.agent.RegisterContext()"
      class="org.cougaar.core.agent.RegisterContext"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- rehydrate mobile/persisted state -->
    <component
      name="org.cougaar.core.agent.RehydrateEarly()"
      class="org.cougaar.core.agent.RehydrateEarly"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!--  read the mobility snapshot and override our component list -->
    <component
      name="org.cougaar.core.agent.FindComponentsEarly()"
      class="org.cougaar.core.agent.FindComponentsEarly"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="HIGH_agent_0">
    <!-- events -->
    <component
      name="org.cougaar.core.agent.service.event.EventServiceComponent()"
      class="org.cougaar.core.agent.service.event.EventServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- uids -->
    <component
      name="org.cougaar.core.agent.service.uid.UIDServiceComponent()"
      class="org.cougaar.core.agent.service.uid.UIDServiceComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="HIGH_config">
    <!-- all agent-level components with "priority='HIGH'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority" select="'HIGH'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="HIGH_agent_1">
    <!-- obtain crypto identity, if enabled -->
    <component
      name="org.cougaar.core.agent.AcquireIdentity()"
      class="org.cougaar.core.agent.AcquireIdentity"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- rehydrate our persistence snapshot, if found -->
    <component
      name="org.cougaar.core.agent.RehydrateLate()"
      class="org.cougaar.core.agent.RehydrateLate"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!--  read the persistence snapshot and override our component list -->
    <component
      name="org.cougaar.core.agent.FindComponentsLate()"
      class="org.cougaar.core.agent.FindComponentsLate"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- register with the node -->
    <component
      name="org.cougaar.core.agent.RegisterAgent()"
      class="org.cougaar.core.agent.RegisterAgent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="HIGH_agent_2">
    <!-- advertise our "topology" wp entry -->
    <component
      name="org.cougaar.core.agent.Topology()"
      class="org.cougaar.core.agent.Topology"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- blackboard reconciliation and version tracking -->
    <component
      name="org.cougaar.core.agent.Reconcile()"
      class="org.cougaar.core.agent.Reconcile"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- agent-level mts proxy -->
    <component
      name="org.cougaar.core.agent.MessageSwitch()"
      class="org.cougaar.core.agent.MessageSwitch"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- agent blackboard message queue -->
    <component
      name="org.cougaar.core.agent.QueueHandler()"
      class="org.cougaar.core.agent.QueueHandler"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- handle queue when suspending and resuming -->
    <component
      name="org.cougaar.core.agent.MessageSwitchShutdown()"
      class="org.cougaar.core.agent.MessageSwitchShutdown"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- start periodic agent restart checker -->
    <component
      name="org.cougaar.core.agent.ReconcileEnabler()"
      class="org.cougaar.core.agent.ReconcileEnabler"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- alarms -->
    <component
      name="org.cougaar.core.agent.AlarmComponent()"
      class="org.cougaar.core.agent.AlarmComponent"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- time control -->
    <component
      name="org.cougaar.core.agent.DemoControl()"
      class="org.cougaar.core.agent.DemoControl"
      priority="HIGH"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="INTERNAL_config">
    <!-- all agent-level components with "priority='INTERNAL'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority" select="'INTERNAL'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="BINDER_config">
    <!-- all agent-level components with "priority='BINDER'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority" select="'BINDER'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="BINDER_agent_0">
    <xsl:if test="$threadService = 'full'">
      <!-- agent-level thread service -->
      <component
        name="org.cougaar.core.thread.ThreadServiceProvider()"
        class="org.cougaar.core.thread.ThreadServiceProvider"
        priority="BINDER"
        insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <!-- backwards compatible thread service wrapper -->
    <component
      name="org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent()"
      class="org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- asset prototypes -->
    <xsl:if test="$planning = 'true'">
      <component
         name="org.cougaar.planning.ldm.PrototypeRegistryServiceComponent()"
         class="org.cougaar.planning.ldm.PrototypeRegistryServiceComponent"
         priority="BINDER"
         insertionpoint="Node.AgentManager.Agent.Component"/>

      <!-- planning data types -->
      <component
         name="org.cougaar.planning.ldm.LDMServiceComponent()"
         class="org.cougaar.planning.ldm.LDMServiceComponent"
         priority="BINDER"
         insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <xsl:if test="$servlets = 'true'">
      <!-- agent-level servlet manager -->
      <component
         name="org.cougaar.lib.web.service.LeafServletServiceComponent()"
         class="org.cougaar.lib.web.service.LeafServletServiceComponent"
         priority="BINDER"
         insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <!-- domain container -->
    <component
      name="org.cougaar.core.domain.DomainManager()"
      class="org.cougaar.core.domain.DomainManager"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.DomainManager">
      <argument>load_planning=<xsl:value-of select="$planning"/></argument>
      <argument>read_config_file=<xsl:value-of select="$domain_ini"/></argument>
      <argument>filename=LDMDomains.ini</argument>
    </component>
    <!-- domains -->
    <xsl:call-template name="find_HIGH_through_BINDER">
      <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.DomainManager.'"/>
    </xsl:call-template>
    <xsl:if test="$mobility = 'true'">
      <!-- agent mobility support -->
      <component
        name='org.cougaar.core.mobility.ldm.MobilityDomain(mobility)'
        class='org.cougaar.core.mobility.ldm.MobilityDomain'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.DomainManager.Domain'>
        <argument>mobility</argument>
      </component>
    </xsl:if>
    <xsl:call-template name="find_COMPONENT_through_LOW">
      <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.DomainManager.'"/>
    </xsl:call-template>

    <!-- legacy community service impl -->
    <xsl:if test="$communities = 'legacy'">
      <component
         name="org.cougaar.community.CommunityServiceComponent()"
         class="org.cougaar.community.CommunityServiceComponent"
         priority="BINDER"
         insertionpoint="Node.AgentManager.Agent.Component"/>
    </xsl:if>

    <!-- blackboard -->
    <component
      name="org.cougaar.core.blackboard.StandardBlackboard()"
      class="org.cougaar.core.blackboard.StandardBlackboard"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="COMPONENT_agent_wp_server">
    <!--
    wp server
   
    Look for a dummy "Server" component.

    For backwards compatibility we also support a "wpserver" XSL
    parameter to disable the default server.
    -->
    <xsl:if test="($wpserver = 'full' or $wpserver = 'true' or $wpserver = 'false') and (component[@class='org.cougaar.core.wp.server.Server'] or ($wpserver = 'true' and ../node))">
      <component
        name="org.cougaar.core.wp.server.ServerContainer()"
        class="org.cougaar.core.wp.server.ServerContainer"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer"/>
      <!-- server -->
      <component
        name="org.cougaar.core.wp.bootstrap.ConfigManager()"
        class="org.cougaar.core.wp.bootstrap.ConfigManager"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      <component
        name="org.cougaar.core.wp.bootstrap.PeersManager()"
        class="org.cougaar.core.wp.bootstrap.PeersManager"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      <component
        name="org.cougaar.core.wp.server.ServerTransport()"
        class="org.cougaar.core.wp.server.ServerTransport"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      <component
        name="org.cougaar.core.wp.server.RootAuthority()"
        class="org.cougaar.core.wp.server.RootAuthority"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component">
        <xsl:if test="$fast_startup = 'true'">
          <argument>successTTD=90000</argument>
          <argument>failTTD=1000</argument>
        </xsl:if>
      </component>
      <!-- bootstrap advertise -->
      <component
        name="org.cougaar.core.wp.bootstrap.AdvertiseManager()"
        class="org.cougaar.core.wp.bootstrap.AdvertiseManager"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      <component
        name="org.cougaar.core.wp.bootstrap.multicast.MulticastAdvertise()"
        class="org.cougaar.core.wp.bootstrap.multicast.MulticastAdvertise"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      <xsl:if test="$servlets = 'true'">
        <component
          name="org.cougaar.core.wp.bootstrap.http.HttpAdvertise()"
          class="org.cougaar.core.wp.bootstrap.http.HttpAdvertise"
          priority="COMPONENT"
          insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
      </xsl:if>
      <component
        name="org.cougaar.core.wp.bootstrap.rmi.RMIAdvertise()"
        class="org.cougaar.core.wp.bootstrap.rmi.RMIAdvertise"
        priority="COMPONENT"
        insertionpoint="Node.AgentManager.Agent.WPServer.Component"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="COMPONENT_config">
    <!-- all agent-level components with "priority='COMPONENT'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority" select="'COMPONENT'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_agent_plugins_0">
    <!-- plugin container -->
    <component
      name="org.cougaar.core.plugin.PluginManager()"
      class="org.cougaar.core.plugin.PluginManager"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.PluginManager"/>

    <!--load high-priority plugins, e.g. security -->
    <xsl:call-template name="find_HIGH_through_BINDER">
      <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.PluginManager.'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_agent_plugins_1">
    <xsl:if test="$sensors = 'true' and $metrics = 'full'">
      <component
        name='org.cougaar.core.qos.metrics.PersistenceAdapterPlugin()'
        class='org.cougaar.core.qos.metrics.PersistenceAdapterPlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
      </component>
    </xsl:if>

    <xsl:if test="$mobility = 'true'">
      <!-- agent mobility support -->
      <component
        name='org.cougaar.core.mobility.service.RedirectMovePlugin()'
        class='org.cougaar.core.mobility.service.RedirectMovePlugin'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'/>
      <xsl:if test="$servlets = 'true'">
        <component
          name='org.cougaar.core.mobility.servlet.MoveAgentServlet()'
          class='org.cougaar.core.mobility.servlet.MoveAgentServlet'
          priority='COMPONENT'
          insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'/>
      </xsl:if>
    </xsl:if>

    <xsl:if test="$standard_agent_servlets = 'true' and $servlets = 'true' and $planning = 'true'">
      <!-- blackboard view -->
      <component
        name='org.cougaar.core.servlet.SimpleServletComponent(org.cougaar.planning.servlet.PlanViewServlet,/tasks)'
        class='org.cougaar.core.servlet.SimpleServletComponent'
        priority='COMPONENT'
        insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
        <argument>org.cougaar.planning.servlet.PlanViewServlet</argument>
        <argument>/tasks</argument>
      </component>
    </xsl:if>

    <!--load any remaining plugins -->
    <xsl:call-template name="find_COMPONENT_through_LOW">
      <xsl:with-param name="insertionpoint" select="'Node.AgentManager.Agent.PluginManager.'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_config">
    <!-- all agent-level components with "priority='LOW'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority" select="'LOW'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_agent_1">
    <!-- release queued messages -->
    <component
      name="org.cougaar.core.agent.MessageSwitchUnpend()"
      class="org.cougaar.core.agent.MessageSwitchUnpend"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- agent lifecycle event logging -->
    <component
      name="org.cougaar.core.agent.Events()"
      class="org.cougaar.core.agent.Events"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- loaded/started/etc logger -->
    <component
      name="org.cougaar.core.agent.EndLogger()"
      class="org.cougaar.core.agent.EndLogger"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

</xsl:stylesheet>
