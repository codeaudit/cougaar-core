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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.QuiescenceReportForDistributorService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.util.FilteredIterator;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

class QuiescenceReportServiceProvider implements ServiceProvider {
  private Map quiescenceStates = new HashMap();
  private boolean isQuiescent = false;
  /**
   * A List of all agents local to this node regardless of whether
   * they are "active" or not.
   **/
  private UnaryPredicate enabledQuiescenceStatePredicate = new UnaryPredicate() {
      public boolean execute(Object o) {
        return ((QuiescenceState) o).isEnabled();
      }
    };
  private Logger logger;
  private String nodeName;
  private AgentContainer agentContainer;
  private ServiceBroker sb;
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
    if (serviceClass == QuiescenceReportService.class) {
      return new QuiescenceReportServiceImpl(requestor.toString());
    }
    if (serviceClass == QuiescenceReportForDistributorService.class) {
      return new QuiescenceReportForDistributorServiceImpl(requestor.toString());
    }
    throw new IllegalArgumentException("Cannot provide " + serviceClass.getName());
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
    return new QuiescenceReportServiceImpl(agent.getPrimary()); // drop MessageAttributes
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
      quiescenceState = new QuiescenceState(me, logger);
      quiescenceStates.put(me, quiescenceState);
    }
    return quiescenceState;
  }

  private Iterator getQuiescenceStatesIterator() {
    return new FilteredIterator(quiescenceStates.values().iterator(), enabledQuiescenceStatePredicate);
  }

  private boolean isLocalAgent(MessageAddress otherAgent) {
    QuiescenceState otherState = (QuiescenceState) quiescenceStates.get(otherAgent);
    if (otherState == null) return false;
    return otherState.isEnabled();
  }

  /**
   * An agent has become quiescent. If all agents are quiescent and no
   * messages are outstanding, we announce our quiescence. Otherwise,
   * we cancel quiescence.
   **/
  private void checkQuiescence() {
    // We could be quiescent
    // If an agent moves out of this node, we need to clean out any
    // remembered message numbers.
    // FIXME: To be safe, should do getPrimary() on all the addresses returned
    // by the agentContainer, to strip off MessageAttributes
    quiescenceStates.keySet().retainAll(agentContainer.getAgentAddresses());
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

  private void cancelQuiescence() {
    if (isQuiescent) {
      announceNonQuiescence();
      isQuiescent = false;
    }
  }

  private synchronized void setQuiescenceReportEnabled(QuiescenceState quiescenceState,
                                                       boolean enabled)
  {
    quiescenceState.setEnabled(enabled);
    if (logger.isInfoEnabled()) {
      logger.info((enabled ? "Enabled " : "Disabled ")
                  + quiescenceState.getAgentName());
    }
    checkQuiescence();
  }

  private synchronized void setMessageNumbers(QuiescenceState quiescenceState,
                                              Map outgoing, Map incoming)
  {
    quiescenceState.setMessageNumbers(outgoing, incoming);
    checkQuiescence();
  }

  private synchronized void setQuiescent(QuiescenceState quiescenceState, boolean isQuiescent) {
    quiescenceState.setQuiescent(isQuiescent);
    if (isQuiescent) {
      checkQuiescence();
    } else if (quiescenceState.isEnabled()) {
      // Only cancel quiescence if the state that announced it was not quiescent isEnabled
      // This prevents early-loading plugins from toggling quiescence of the Node
      cancelQuiescence();
    }
  }

  private boolean allAgentsAreQuiescent() {
    boolean result = true;
    for (Iterator theseStates = getQuiescenceStatesIterator(); theseStates.hasNext(); ) {
      QuiescenceState thisAgentState = (QuiescenceState) theseStates.next();
      if (!thisAgentState.isQuiescent()) {
        result = false;
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgentState.getAgentName() + " is not quiescent");
        } else {
          break;
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgentState.getAgentName() + " is quiescent");
        }
      }
    }
    return result;
  }

  private boolean noMessagesAreOutstanding() {
    checkQuiescence:
    for (Iterator theseStates = getQuiescenceStatesIterator(); theseStates.hasNext(); ) {
      QuiescenceState thisAgentState = (QuiescenceState) theseStates.next();
      MessageAddress thisAgent = thisAgentState.getAgent();
      for (Iterator thoseStates = getQuiescenceStatesIterator(); thoseStates.hasNext(); ) {
        QuiescenceState thatAgentState = (QuiescenceState) thoseStates.next();
        MessageAddress thatAgent = thatAgentState.getAgent();
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
      if (isLocalAgent(otherAgent)) continue; // Exclude local agents
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
    for (Iterator states = getQuiescenceStatesIterator(); states.hasNext(); ) {
      QuiescenceState quiescenceState = (QuiescenceState) states.next();
      ms.append(" <agent name=\"")
        .append(quiescenceState.getAgentName())
        .append("\">")
        .append(EOL);
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
    private boolean lastAnnouncedQuiescence = true;
    private long announcementTime;
    private boolean running;
    private EventService eventService;
    private Object eventServiceLock = new Object();

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
      // Cancel pending announcment if any
      pendingAnnouncement = null;
      if (lastAnnouncedQuiescence) {
        event(announcement);
        lastAnnouncedQuiescence = false;
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

    private void event(String msg) {
      synchronized (eventServiceLock) {
        if (eventService == null) {
          eventService = (EventService) sb.getService(QuiescenceReportServiceProvider.this, EventService.class, null);
          if (eventService == null) {
            logger.error("No EventService available for " + EOL + msg);
            return;
          }
        }
      }
      eventService.event(msg);
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
            lastAnnouncedQuiescence = true;
          }
        } catch (Exception e) {
          logger.error("QuiescenceAnnouncer", e);
        }
      }
    }
  }

  private class QuiescenceReportForDistributorServiceImpl
    extends QuiescenceReportServiceImpl
    implements QuiescenceReportForDistributorService
  {
    public QuiescenceReportForDistributorServiceImpl(String requestorName) {
      super(requestorName);
    }

    public void setQuiescenceReportEnabled(boolean enabled) {
      QuiescenceReportServiceProvider.this.setQuiescenceReportEnabled(quiescenceState, enabled);
    }
  }

  private class QuiescenceReportServiceImpl implements QuiescenceReportService {
    protected QuiescenceState quiescenceState = null;
    private boolean isQuiescent = true; // We haven't been counted as not quiescent yet
    private String requestorName;

    public QuiescenceReportServiceImpl(MessageAddress agent) {
      quiescenceState = getQuiescenceState(agent); // Drop messageAttributes
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
      isQuiescent = true;
      QuiescenceReportServiceProvider.this.setQuiescent(quiescenceState, true);
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
    }
  }
}
