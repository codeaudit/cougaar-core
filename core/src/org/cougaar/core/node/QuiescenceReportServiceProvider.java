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

package org.cougaar.core.node;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

class QuiescenceReportServiceProvider implements ServiceProvider {
  private Map quiescenceStates = new HashMap();
  private boolean isQuiescent = false;
  private List localAgents = new ArrayList();
  private Logger logger;
  private String nodeName;
  private AgentContainer agentContainer;
  private ServiceBroker sb;
  private EventService eventService;
  private Object eventServiceLock = new Object();
  private QuiescenceAnnouncer quiescenceAnnouncer;

  private static final long ANNOUNCEMENT_DELAY = 30000L;
  private static final String EOL = " "; //System.getProperty("line.separator");

  QuiescenceReportServiceProvider(String nodeName,
                                  AgentContainer agentContainer,
                                  ServiceBroker sb)
  {
    this.nodeName = nodeName;
    this.agentContainer = agentContainer;
    this.sb = sb;
    logger = new LoggingServiceWithPrefix(Logging.getLogger(getClass()), nodeName + ": ");
    quiescenceAnnouncer = new QuiescenceAnnouncer();
    quiescenceAnnouncer.start();
  }

  public Object getService(ServiceBroker xsb,
                           Object requestor,
                           Class serviceClass)
  {
    if (serviceClass != QuiescenceReportService.class) {
      throw new IllegalArgumentException("Can only provide QuiescenceReportService!");
    }
    return new QuiescenceReportServiceImpl(requestor.toString());
  }

  public void releaseService(ServiceBroker xsb, Object requestor,
                             Class serviceClass, Object service)
  {
    if (service instanceof QuiescenceReportService) {
      QuiescenceReportService quiescenceReportService = (QuiescenceReportService) service;
      quiescenceReportService.setQuiescentState();
    }
  }

  // Used by NodeAgent to create an instance for a specific agent address.
  QuiescenceReportService createQuiescenceReportService(MessageAddress agent) {
    return new QuiescenceReportServiceImpl(agent);
  }

  void agentRemoved() {
    checkQuiescence();
  }

  private void event(String msg) {
    synchronized (eventServiceLock) {
      if (eventService == null) {
        eventService = (EventService) sb.getService(this, EventService.class, null);
        if (eventService == null) {
          logger.error("No EventService available for " + EOL + msg);
          return;
        }
      }
    }
    eventService.event(msg);
  }

  private static void setMessageMap(MessageAddress me, Map messageNumbers, Map newMap) {
    Map existingMap = (Map) messageNumbers.get(me);
    if (existingMap == null) {
      existingMap = new HashMap(newMap);
      messageNumbers.put(me, existingMap);
    } else {
      existingMap.clear();
      existingMap.putAll(newMap);
    }
  }

  private QuiescenceState getQuiescenceState(MessageAddress me) {
    QuiescenceState quiescenceState = (QuiescenceState) quiescenceStates.get(me);
    if (quiescenceState == null) {
      quiescenceState = new QuiescenceState(me);
      quiescenceStates.put(me, quiescenceState);
    }
    return quiescenceState;
  }

  /**
   * An agent has become quiescent. If all agents are quiescent and no
   * messages are outstanding, we announce our quiescence. Otherwise,
   * we cancel quiescence.
   **/
  private synchronized void checkQuiescence() {
    // We could be quiescent
    localAgents.clear();
    localAgents.addAll(agentContainer.getAgentAddresses());
    // If an agent moves out of this node, we need to clean out any
    // remembered message numbers. Remember if any removes happened
    quiescenceStates.keySet().retainAll(localAgents);
    if (allAgentsAreQuiescent() && noMessagesAreOutstanding()) {
      announceQuiescence();
      if (!isQuiescent && logger.isInfoEnabled()) {
        logger.info("Is quiescent");
      }
      isQuiescent = true;
    } else if (isQuiescent) {
      cancelQuiescence();
    }
  }

  private synchronized void cancelQuiescence() {
    if (isQuiescent) {
      announceNonQuiescence();
      isQuiescent = false;
    }
  }

  private synchronized void setMessageNumbers(QuiescenceState quiescenceState, Map outgoing, Map incoming) {
    quiescenceState.setMessageNumbers(outgoing, incoming);
  }

  private synchronized void setQuiescent(QuiescenceState quiescenceState, boolean isQuiescent) {
    quiescenceState.setQuiescent(isQuiescent);
  }

  private boolean allAgentsAreQuiescent() {
    boolean result = true;
    for (Iterator theseAgents = localAgents.iterator(); theseAgents.hasNext(); ) {
      MessageAddress thisAgent = (MessageAddress) theseAgents.next();
      QuiescenceState thisAgentState = getQuiescenceState(thisAgent);
      if (!thisAgentState.isQuiescent()) {
        result = false;
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgent + " is not quiescent");
        } else {
          break;
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgent + " is quiescent");
        }
      }
    }
    return result;
  }

  private boolean noMessagesAreOutstanding() {
    checkQuiescence:
    for (Iterator theseAgents = localAgents.iterator(); theseAgents.hasNext(); ) {
      MessageAddress thisAgent = (MessageAddress) theseAgents.next();
      QuiescenceState thisAgentState = getQuiescenceState(thisAgent);
      for (Iterator thoseAgents = localAgents.iterator(); thoseAgents.hasNext(); ) {
        MessageAddress thatAgent = (MessageAddress) thoseAgents.next();
        QuiescenceState thatAgentState = getQuiescenceState(thatAgent);
        Integer sentNumber = thisAgentState.getOutgoingMessageNumber(thatAgent);
        Integer rcvdNumber = thatAgentState.getIncomingMessageNumber(thisAgent);
        boolean match;
        if (sentNumber == null) {
          match = rcvdNumber == null;
        } else {
          match = sentNumber.equals(rcvdNumber);
        }
        if (!match) {
          if (logger.isDebugEnabled()) {
            logger.debug("Quiescence prevented by "
                         + thisAgent + " sent " + sentNumber + ", but "
                         + thatAgent + " rcvd " + rcvdNumber);
          }
          return false;
        }
      }
    }
    return true;
  }

  private void appendMessageNumbers(StringBuffer ms, Map messages, String listTag, String itemTag) {
    ms.append("  <").append(listTag).append(">").append(EOL);
    for (Iterator entries = messages.entrySet().iterator(); entries.hasNext(); ) {
      Map.Entry entry = (Map.Entry) entries.next();
      MessageAddress otherAgent = (MessageAddress) entry.getKey();
      if (localAgents.contains(otherAgent)) continue;
      Integer msgnum = (Integer) entry.getValue(); // 
      ms.append("   <").append(itemTag).append(" agent=\"").append(otherAgent)
        .append("\" msgnum=\"").append(msgnum).append("\"/>").append(EOL);
    }
    ms.append("  </").append(listTag).append(">").append(EOL);
  }

  private void announceQuiescence() {
    // Spit out the message numbers we sent to agents of other
    // nodes and the message numbers we received from agents
    // of other nodes.
    StringBuffer ms = new StringBuffer();
    ms.append("<node name=\"").append(nodeName).append("\" quiescent=\"true\">").append(EOL);
    for (Iterator agents = localAgents.iterator(); agents.hasNext(); ) {
      MessageAddress agent = (MessageAddress) agents.next();
      ms.append(" <agent name=\"").append(agent.getAddress()).append("\">").append(EOL);
      QuiescenceState quiescenceState = getQuiescenceState(agent);
      appendMessageNumbers(ms, quiescenceState.getOutgoingMessageNumbers(), "receivers", "dest");
      appendMessageNumbers(ms, quiescenceState.getIncomingMessageNumbers(), "senders", "src");
      ms.append(" </agent>").append(EOL);
    }
    ms.append("</node>").append(EOL);
    quiescenceAnnouncer.announceQuiescence(ms.toString());
  } 

  private void announceNonQuiescence() {
    quiescenceAnnouncer.announceNonquiescence("<node name=\""
                                              + nodeName
                                              + "\" quiescent=\"false\"/>"
                                              + EOL);
  }

  private class QuiescenceAnnouncer extends Thread {
    private String pendingAnnouncement;
    private long announcementTime;
    private boolean running;

    public QuiescenceAnnouncer() {
      super("Quiescence Announcer");
    }

    public void start() {
      running = true;
      super.start();
    }

    public synchronized void interrupt() {
      running = false;
      super.interrupt();
    }

    public synchronized void announceNonquiescence(String announcement) {
      if (pendingAnnouncement == null) {
        event(announcement);
      } else {
        // No need to announce because we have not yet announced quiescence
        pendingAnnouncement = null;
      }
    }

    public synchronized void announceQuiescence(String announcement) {
      // Replace the pending announcement
      pendingAnnouncement = announcement;
      // and restart the timeout
      announcementTime = System.currentTimeMillis() + ANNOUNCEMENT_DELAY;
      notify();
    }

    private long getDelay() {
      return announcementTime - System.currentTimeMillis();
    }

    public synchronized void run() {
      long delay;
      while (running) {
        try {
          if (pendingAnnouncement == null) {
            wait();
          } else if ((delay = getDelay()) > 0L) {
            wait(delay);
          } else {
            event(pendingAnnouncement);
            pendingAnnouncement = null;
          }
        } catch (Exception e) {
          logger.error("QuiescenceAnnouncer", e);
        }
      }
    }
  }

  private class QuiescenceState {
    public QuiescenceState(MessageAddress me) {
      this.me = me;
    }

    public Map getOutgoingMessageNumbers() {
      return outgoingMessageNumbers;
    }

    public Map getIncomingMessageNumbers() {
      return incomingMessageNumbers;
    }

    public Integer getOutgoingMessageNumber(MessageAddress receiver) {
      return (Integer) getOutgoingMessageNumbers().get(receiver);
    }

    public Integer getIncomingMessageNumber(MessageAddress sender) {
      return (Integer) getIncomingMessageNumbers().get(sender);
    }

    public boolean isQuiescent() {
      return nonQuiescenceCount == 0;
    }

    public void setQuiescent(boolean isQuiescent) {
      if (isQuiescent) {
        nonQuiescenceCount--;
        if (logger.isDetailEnabled()) {
          logger.detail("nonQuiescenceCount is " + nonQuiescenceCount + " for " + me);
        } else if (nonQuiescenceCount == 0 && logger.isDebugEnabled()) {
          logger.debug(me + " is quiescent");
        }
      } else {
        nonQuiescenceCount++;
        if (logger.isDetailEnabled()) {
          logger.detail("nonQuiescenceCount is " + nonQuiescenceCount + " for " + me);
        } else if (nonQuiescenceCount == 1 && logger.isDebugEnabled()) {
          logger.debug(me + " is not quiescent");
        }
      }
    }

    public void setMessageNumbers(Map outgoing, Map incoming) {
      outgoingMessageNumbers = updateMap(outgoingMessageNumbers, outgoing);
      incomingMessageNumbers = updateMap(incomingMessageNumbers, incoming);
      if (logger.isDetailEnabled()) {
        logger.detail("setMessageNumbers for " + me
                      + ", outgoing=" + outgoing
                      + ", incoming=" + incoming);
      } else if (logger.isDebugEnabled()) {
        logger.debug("setMessageNumbers for " + me);
      }
    }

    public String getAgentName() {
      return me.toString();
    }

    private Map incomingMessageNumbers = new HashMap(13);
    private Map outgoingMessageNumbers = new HashMap(13);
    private int nonQuiescenceCount = 0;
    private MessageAddress me;
  }

  /**
   * Update an existing map to equal a new Map where the keys in the
   * old map are very likely to be the same as the keys in the new Map
   * while minimizing additional memory allocation.
   **/
  private static Map updateMap(Map oldMap, Map newMap) {
    if (newMap == null) {
      throw new IllegalArgumentException("Null Map");
    }
    if (oldMap == null) return new HashMap(newMap);
    // Flush all keys missing from the new map
    oldMap.keySet().retainAll(newMap.keySet());
    // Avoid oldMap.putAll(newMap) since it expands the (Hash)Map
    // assuming the newMap has a non-intersecting keyset.
    for (Iterator i = newMap.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      oldMap.put(entry.getKey(), entry.getValue());
    }
    return oldMap;
  }

  private class QuiescenceReportServiceImpl implements QuiescenceReportService {
    private QuiescenceState quiescenceState = null;
    private boolean isQuiescent = true; // We haven't been counted as not quiescent yet
    private String requestorName;

    public QuiescenceReportServiceImpl(MessageAddress agent) {
      quiescenceState = getQuiescenceState(agent);
      this.requestorName= agent.toString();
    }

    public QuiescenceReportServiceImpl(String requestorName) {
      this.requestorName= requestorName;
    }

    public void setAgentIdentificationService(AgentIdentificationService ais) {
      if (ais == null) {
        quiescenceState = null;
      } else {
        quiescenceState = getQuiescenceState(ais.getMessageAddress());
      }
    }

    /**
     * Specify the maps of quiescence-relevant outgoing and incoming
     * message numbers associated with a newly achieved state of
     * quiescence. For efficiency, this method should be called before
     * calling setQuiescentState().
     * @param outgoingMessageNumbers a Map from agent MessageAddresses
     * to Integers giving the number of the last message sent. The
     * arguments must not be null.
     * @param incomingMessageNumbers a Map from agent MessageAddresses
     * to Integers giving the number of the last message received. The
     * arguments must not be null.
     **/
    public void setMessageNumbers(Map outgoing, Map incoming) {
      if (quiescenceState == null) throw new RuntimeException("AgentIdentificationService has not be set");
      QuiescenceReportServiceProvider.this.setMessageNumbers(quiescenceState, outgoing, incoming);
      checkQuiescence();
    }

    /**
     * Specifies that, from this service instance point-of-view, the
     * agent is quiescent.
     **/
    public void setQuiescentState() {
      if (isQuiescent) return;
      if (logger.isDebugEnabled()) {
        logger.debug("setQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName());
      }
      QuiescenceReportServiceProvider.this.setQuiescent(quiescenceState, true);
      isQuiescent = true;
      checkQuiescence();
    }

    /**
     * Specifies that this agent is no longer quiescent.
     **/
    public void clearQuiescentState() {
      if (quiescenceState == null) {
        throw new RuntimeException("AgentIdentificationService has not be set");
      }
      if (!isQuiescent) return;
      if (logger.isDebugEnabled()) {
        logger.debug("clearQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName());
      }
      QuiescenceReportServiceProvider.this.setQuiescent(quiescenceState, false);
      isQuiescent = false;
      cancelQuiescence();
    }
  }
}
