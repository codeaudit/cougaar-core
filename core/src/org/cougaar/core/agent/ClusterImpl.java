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

package org.cougaar.core.agent;

import org.cougaar.core.service.*;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.service.alarm.*;

import org.cougaar.core.agent.service.containment.*;

import org.cougaar.core.agent.service.democontrol.*;

import org.cougaar.core.agent.service.registry.*;

import org.cougaar.core.agent.service.scheduler.*;

import org.cougaar.core.agent.service.uid.*;

import org.cougaar.core.blackboard.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.naming.directory.DirContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameClassPair;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.StateModelException;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentBindingSite;

import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;

import org.cougaar.core.service.NamingService;

import org.cougaar.core.node.ComponentMessage;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageStatistics;

// cluster context registration
import org.cougaar.core.agent.ClusterContext;

// blackboard support
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.blackboard.BlackboardServiceProvider;

// message-transport support
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.service.MessageWatcherService;

// LDM service
import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.plugin.LDMServiceProvider;

// prototype and property providers
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;

// domain and factory support
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.domain.Domain;
import org.cougaar.core.domain.DomainManager;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.RootFactory;

// types for factory support
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;

// prototype registry service
import org.cougaar.core.agent.service.registry.PrototypeRegistry;
import org.cougaar.core.service.PrototypeRegistryService;
import org.cougaar.core.agent.service.registry.PrototypeRegistryServiceProvider;

// Object factories
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.ClusterObjectFactoryImpl;

// Scenario time support
import org.cougaar.core.agent.AdvanceClockMessage;
import org.cougaar.core.agent.service.alarm.Alarm;

// Persistence
//  import org.cougaar.core.persist.DatabasePersistence;
import org.cougaar.core.persist.Persistence;

import org.cougaar.util.PropertyParser;

/**
 * Implementation of Agent which creates a PluginManager and Blackboard and 
 * provides basic services to Agent Components.
 * <p>
 * <pre>
 * @property org.cougaar.core.agent.heartbeat
 *   If enabled, a low-priority thread runs and prints
 *   a '.' every few seconds when nothing else much is going on.
 *   This is a one-per-vm function.  Default <em>true</em>.
 * @property org.cougaar.core.agent.idleInterval 
 * How long between idle detection and heartbeat cycles (prints '.');
 * @property org.cougaar.core.agent.idle.verbose
 *   If <em>true</em>, will print elapsed time (seconds) since
 *   cluster start every idle.interval millis.
 * @property org.cougaar.core.agent.idle.verbose.interval=60000
 *   The number of milliseconds between verbose idle reports.
 * @property org.cougaar.core.agent.showTraffic
 *   If <em>true</em>, shows '+' and '-' on message sends and receives.  if
 *   <em>false</em>, also turns off reports of heartbeat ('.') and other status chars.
 *
 * @property org.cougaar.core.servlet.enable
 *   Used to enable ServletService; defaults to "true".
 * </pre>
 */
public class ClusterImpl 
  extends SimpleAgent
  //  implements Cluster, LDMServesPlugin, ClusterContext, MessageTransportClient, MessageStatistics, StateObject
{
  /** Standard, no argument constructor. */
  public ClusterImpl() {
  }
  public ClusterImpl(ComponentDescription comdesc) {
    super(comdesc);
    // services added in load()
  }
}
