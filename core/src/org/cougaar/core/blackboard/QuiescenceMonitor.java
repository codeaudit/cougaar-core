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

package org.cougaar.core.blackboard;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logger;

class QuiescenceMonitor {
  private static final String CONFIG_FILE = "quiescencemonitor.dat";
  private static final String[] defaultExcludedClients = {
    "exclude .*"
  };

  private static final String finalExclusion = "exclude .*";

  private QuiescenceReportService quiescenceReportService;
  private Logger logger;
  private boolean isQuiescent = false;
  private String messageNumbersChangedFor = null;
  private static class State implements Serializable {
    State(Map imn, Map omn) {
      outgoingMessageNumbers = omn;
      incomingMessageNumbers = imn;
    }
    private Map outgoingMessageNumbers;
    private Map incomingMessageNumbers;
  }
  private Map outgoingMessageNumbers = new HashMap();
  private Map incomingMessageNumbers = new HashMap();
  private int messageNumberCounter;
  private Map checkedClients = new HashMap();
  private List exclusions = new ArrayList();

  QuiescenceMonitor(QuiescenceReportService qrs, Logger logger) {
    this.logger = logger;
    quiescenceReportService = qrs;
    initMessageNumberCounter();
    try {
      BufferedReader is =
        new BufferedReader(new InputStreamReader(ConfigFinder.getInstance().open(CONFIG_FILE)));
      try {
        String line;
        while ((line = is.readLine()) != null) {
          int hash = line.indexOf('#');
          if (hash >= 0) {
            line = line.substring(0, hash);
          }
          line = line.trim();
          if (line.length() > 0) {
            addExclusion(line);
          }
        }
        addExclusion(finalExclusion);
      } finally {
        is.close();
      }
    } catch (FileNotFoundException e) {
      if (logger.isWarnEnabled()) logger.warn("File not found: " + e.getMessage() + ". Using defaults");
      installDefaultExclusions();
    } catch (Exception e) {
      logger.error("Error parsing " + CONFIG_FILE + ". Using defaults", e);
      installDefaultExclusions();
    }
  }

  void setState(Object newState) {
    State state = (State) newState;
    incomingMessageNumbers = state.incomingMessageNumbers;
    outgoingMessageNumbers = state.outgoingMessageNumbers;
    messageNumbersChangedFor = "setState";
    setSubscribersAreQuiescent(isQuiescent);
  }

  Object getState() {
    return new State(incomingMessageNumbers, outgoingMessageNumbers);
  }

  private void initMessageNumberCounter() {
    messageNumberCounter = (int) System.currentTimeMillis();
    nextMessageNumber();
  }

  private int nextMessageNumber() {
    if (++messageNumberCounter == 0) messageNumberCounter++;
    return messageNumberCounter;
  }

  private void installDefaultExclusions() {
    exclusions.clear();
    for (int i = 0; i < defaultExcludedClients.length; i++) {
      addExclusion(defaultExcludedClients[i]);
    }
  }

  private void addExclusion(String line) {
    exclusions.add(new Exclusion(line));
  }

  boolean isQuiescenceRequired(BlackboardClient client) {
    String clientName = client.getBlackboardClientName();
    Boolean required = (Boolean) checkedClients.get(clientName);
    if (required == null) {
      required = Boolean.TRUE;
      loop:
      for (int i = 0, n = exclusions.size(); i < n; i++) {
        Exclusion p = (Exclusion) exclusions.get(i);
        switch (p.match(clientName)) {
        case Exclusion.EXCLUDE:
          required = Boolean.FALSE;
          break loop;
        case Exclusion.INCLUDE:
          required = Boolean.TRUE;
          break loop;
        default:
          continue loop;
        }
      }
      if (logger.isInfoEnabled()) {
        logger.info("isQuiescenceRequired(" + clientName + ")=" + required);
      }
      checkedClients.put(clientName, required);
    }
    return required.booleanValue();
  }

  synchronized void setSubscribersAreQuiescent(boolean subscribersAreQuiescent) {
    if (subscribersAreQuiescent) {
      if (messageNumbersChangedFor != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("updateMessageNumbers because of " + messageNumbersChangedFor);
        }
        quiescenceReportService
          .setMessageNumbers(outgoingMessageNumbers,
                             incomingMessageNumbers);
        messageNumbersChangedFor = null;
      }
      if (!isQuiescent) {
        isQuiescent = true;
        if (logger.isDebugEnabled()) {
          logger.debug("setQuiescentState");
        }
        quiescenceReportService.setQuiescentState();
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Still quiescent");
        }
      }
    } else {
      if (isQuiescent) {
        isQuiescent = false;
        if (messageNumbersChangedFor != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("clearQuiescentState: messageNumbersChangedFor " + messageNumbersChangedFor);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("clearQuiescentState: subscribers active");
          }
        }
        quiescenceReportService.clearQuiescentState();
      }
    }
  }

  synchronized boolean numberIncomingMessage(DirectiveMessage msg) {
    MessageAddress src = msg.getSource();
    src = src.getPrimary();
    int messageNumber = msg.getContentsId();
    if (messageNumber == 0) return false; // Message from plugin not required for quiescence
    incomingMessageNumbers.put(src, new Integer(messageNumber));
    messageNumbersChangedFor = src.toString();
    return true;
  }

  synchronized void numberOutgoingMessage(DirectiveMessage msg) {
    MessageAddress dst = msg.getDestination();
    dst = dst.getPrimary();
    int messageNumber = nextMessageNumber();
    msg.setContentsId(messageNumber);
    outgoingMessageNumbers.put(dst, new Integer(messageNumber));
    messageNumbersChangedFor = dst.toString();
  }

  private static class Exclusion {
    private static final int EXCLUDE = 0;
    private static final int INCLUDE = 1;
    private static final int DONT_KNOW = -1;
    private static final String EXCLUDE_PREFIX = "exclude ";
    private static final String INCLUDE_PREFIX = "include ";
    private int matchCode;
    private Pattern p;

    public Exclusion(String line) {
      if (line.startsWith(EXCLUDE_PREFIX)) {
        matchCode = EXCLUDE;
        p = Pattern.compile(line.substring(EXCLUDE_PREFIX.length()));
      } else if (line.startsWith(INCLUDE_PREFIX)) {
        matchCode = INCLUDE;
        p = Pattern.compile(line.substring(INCLUDE_PREFIX.length()));
      } else {
        throw new IllegalArgumentException("Parse error: " + line);
      }
    }

    public int match(String clientName) {
      if (p.matcher(clientName).matches()) {
        return matchCode;
      }
      return DONT_KNOW;
    }
  }
}
