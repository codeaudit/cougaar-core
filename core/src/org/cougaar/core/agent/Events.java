/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.net.InetAddress;
import java.net.URI;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The Events component generates {@link EventService} events
 * to announce the agent's load/start/stop/<i>etc</i> and move.
 */
public final class Events 
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private EventService eventService;
  private WhitePagesService wps;

  private MobilityNotificationClient mnc;
  private MobilityNotificationService mns;

  private MessageAddress moveTargetNode;

  private MessageAddress localAgent;
  private MessageAddress localNode;
  private String localHost;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(
          this, LoggingService.class, null);

    eventService = (EventService)
      sb.getService(
          this, EventService.class, null);
    if (eventService == null) {
      throw new RuntimeException(
          "Unable to obtain EventService");
    }

    wps = (WhitePagesService)
      sb.getService(this, WhitePagesService.class, null);

    // local agent
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // local node
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis != null) {
      localNode = nis.getMessageAddress();
      sb.releaseService(
          this, NodeIdentificationService.class, nis);
    }

    // local host
    try {
      InetAddress localAddr = InetAddress.getLocalHost();
      localHost = localAddr.getHostName();
    } catch (Exception e) {
      localHost = "?";
    }

    // mobility watcher
    mnc =
      new MobilityNotificationClient() {
        public void movingTo(MessageAddress destinationNode) {
          moveTargetNode = destinationNode;
        }
      };
    mns = (MobilityNotificationService)
      sb.getService(mnc, MobilityNotificationService.class, null);
    if (mns == null && log.isInfoEnabled()) {
      log.info(
         "Unable to obtain MobilityNotificationService"+
         ", mobility is disabled");
    }
  }

  public void start() {
    super.start();

    if (eventService != null &&
        eventService.isEventEnabled()) {
      eventService.event(
          "AgentLifecycle("+"Started"+
          ") Agent("+localAgent+
          ") Node("+localNode+
          ") Host("+localHost+
          ")");
    }
  }

  public void suspend() {
    super.suspend();

    if (moveTargetNode == null) {
      return;
    }
    if (eventService == null ||
        !eventService.isEventEnabled()) {
      return;
    }
    // record event
    String moveTargetHost = "?";
    try {
      AddressEntry nodeEntry = 
        wps.get(
            moveTargetNode.getAddress(),
            "topology",
            10000); // wait at most 10 seconds
      if (nodeEntry != null) {
        moveTargetHost = nodeEntry.getURI().getHost();
      }
    } catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info(
            "Unable to get host for destination node "+
            moveTargetNode,
            e);
      }
    }
    eventService.event(
        "AgentLifecycle("+"Moving"+
        ") Agent("+localAgent+
        ") Node("+localNode+
        ") Host("+localHost+
        ") ToNode("+moveTargetNode+
        ") ToHost("+moveTargetHost+
        ")");
  }

  public void resume() {
    super.resume();

    if (moveTargetNode == null) {
      return;
    }

    if (eventService != null &&
        eventService.isEventEnabled()) {
      eventService.event(
          "AgentLifecycle("+"NotMoved"+
          ") Agent("+localAgent+
          ") Node("+localNode+
          ") Host("+localHost+
          ") ToNode("+moveTargetNode+
          ")");
    }
    moveTargetNode = null;
  }

  public void stop() {
    super.stop();

    if (eventService != null &&
        eventService.isEventEnabled()) {
      if (moveTargetNode == null) {
        eventService.event(
            "AgentLifecycle("+"Stopped"+
            ") Agent("+localAgent+
            ") Node("+localNode+
            ") Host("+localHost+
            ")");
      } else {
        eventService.event(
            "AgentLifecycle("+"Moved"+
            ") Agent("+localAgent+
            ") Node("+localNode+
            ") Host("+localHost+
            ") ToNode("+moveTargetNode+
            ")");
      }
    }
  }

  public void unload() {
    super.unload();

    if (mns != null) {
      sb.releaseService(mnc, MobilityNotificationService.class, mns);
      mns = null;
    }
    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
    if (eventService != null) {
      sb.releaseService(this, EventService.class, eventService);
      eventService = null;
    }
  }
}
