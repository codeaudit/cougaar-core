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
XSL Template for SimpleAgent.

This template finds all agent elements that either specify a
"template='SimpleAgent.xsl'" attribute or lack both "template"
and "type" attributes.  The components in the matching agent
elements are merged with hard-coded component elements to create
the agent's full list of components.

Developers can modify the contents of this file to add/change/remove
the default components loaded into an agent.  For example, the
servlet server components can be commented out to disable servlets.

This file can also be reused to create new agent templates, e.g.
a custom "MyAgent.xsl":
  <xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
  
    <xsl:import href="SimpleAgent.xsl"/>
    <xsl:output method="xsl" indent="yes"/>
  
    <xsl:template match="agent[@template = 'MyAgent.xsl']">
      <agent name="{@name}" template="">
        <xsl:apply-templates/>
        <xsl:call-template name="MyAgent_run"/>
      </agent>
    </xsl:template>

    <xsl:template name="MyAgent_run">
      // maybe call some or all of the SimpleAgent templates here
      //
      // see NodeAgent.xsl for an example
    </xsl:template>
  </xsl:stylesheet>
In the "mySociety.xml" file the agent would specify the template:
  ...
  <agent name="Foo" template="MyAgent.xsl">
    ...
  </agent>
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <xsl:import href="util.xsl"/>

  <xsl:strip-space elements="*"/>

  <xsl:output method="xsl" indent="yes"/>

  <!-- default template to preserve all tags -->
  <xsl:template match="@*|node()|processing-instruction()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|processing-instruction()|comment()"/>
    </xsl:copy>
  </xsl:template>

  <!-- we control component tags -->
  <xsl:template match="component"/>

  <!-- match 'agent' elements of type SimpleAgent -->
  <xsl:template match="agent[(@template = 'SimpleAgent.xsl') or (not(@template) and not(@type))]">
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
    <xsl:call-template name="HIGH_agent_0"/>
    <xsl:call-template name="HIGH_config"/>
    <xsl:call-template name="HIGH_agent_1"/>
    <xsl:call-template name="HIGH_agent_2"/>

    <xsl:call-template name="INTERNAL_config"/>

    <xsl:call-template name="BINDER_config"/>
    <xsl:call-template name="BINDER_agent_0"/>

    <xsl:call-template name="COMPONENT_config"/>

    <xsl:call-template name="LOW_agent_0"/>
    <xsl:call-template name="LOW_config"/>
    <xsl:call-template name="LOW_agent_1"/>
  </xsl:template>

  <!-- named templates, which simply group components -->

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
      <xsl:with-param name="priority">HIGH</xsl:with-param>
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

    <!--
    if we're rehydrated, override our component list with the
    components found in the snapshot.  This will discard the
    remainder of this config.
    -->
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
      <xsl:with-param name="priority">INTERNAL</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="BINDER_config">
    <!-- all agent-level components with "priority='BINDER'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority">BINDER</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="BINDER_agent_0">
    <!-- agent-level thread service -->
    <component
      name="org.cougaar.core.thread.ThreadServiceProvider()"
      class="org.cougaar.core.thread.ThreadServiceProvider"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- backwards compatible thread service wrapper -->
    <component
      name="org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent()"
      class="org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- asset prototypes -->
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

    <!-- agent-level servlet manager -->
    <component
      name="org.cougaar.lib.web.service.LeafServletServiceComponent()"
      class="org.cougaar.lib.web.service.LeafServletServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- domain container -->
    <component
      name="org.cougaar.core.domain.DomainManager()"
      class="org.cougaar.core.domain.DomainManager"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.DomainManager"/>
    <!-- domains -->
    <xsl:call-template name="findAll">
      <xsl:with-param name="insertionpoint">Node.AgentManager.Agent.DomainManager.</xsl:with-param>
    </xsl:call-template>

    <!-- communities -->
    <component
      name="org.cougaar.community.CommunityServiceComponent()"
      class="org.cougaar.community.CommunityServiceComponent"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>

    <!-- blackboard -->
    <component
      name="org.cougaar.core.blackboard.StandardBlackboard()"
      class="org.cougaar.core.blackboard.StandardBlackboard"
      priority="BINDER"
      insertionpoint="Node.AgentManager.Agent.Component"/>
  </xsl:template>

  <xsl:template name="COMPONENT_config">
    <!-- all agent-level components with "priority='COMPONENT'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority">COMPONENT</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_agent_0">
    <!-- plugin container -->
    <component
      name="org.cougaar.core.plugin.PluginManager()"
      class="org.cougaar.core.plugin.PluginManager"
      priority="LOW"
      insertionpoint="Node.AgentManager.Agent.PluginManager"/>
    <!-- plugins -->
    <xsl:call-template name="findAll">
      <xsl:with-param name="insertionpoint">Node.AgentManager.Agent.PluginManager.</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="LOW_config">
    <!-- all agent-level components with "priority='LOW'" -->
    <xsl:call-template name="find">
      <xsl:with-param name="priority">LOW</xsl:with-param>
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
