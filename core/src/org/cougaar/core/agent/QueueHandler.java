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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The QueueHandler buffers blackboard messages while the agent is
 * loading, plus switches threads when receiving messages to avoid
 * blocking the message transport thread.
 */
public final class QueueHandler
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private MessageSwitchService mss;

  private BlackboardForAgent bb;

  private MessageAddress localAgent;

  private QueueHandlerThread thread;
  private boolean isStarted;
  private Object lock = new Object();

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    mss = (MessageSwitchService)
      sb.getService(this, MessageSwitchService.class, null);

    // register message handler to observe all incoming messages
    MessageHandler mh = new MessageHandler() {
      public boolean handleMessage(Message message) {
        if (message instanceof ClusterMessage) {
          // internal message queue
          getThread().addMessage((ClusterMessage) message);
          return true;
        } else {
          return false;
        }
      }
    };
    mss.addMessageHandler(mh);
  }

  public void start() {
    super.start();

    // get blackboard service
    //
    // this is delayed until "start()" because the queue handler
    // is loaded after the blackboard.  Messages are buffered
    // between the top-level "load()" MTS unpend and our "start()",
    // then released by "startThread()".
    bb = (BlackboardForAgent)
      sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null) {
      throw new RuntimeException(
          "Unable to obtain BlackboardForAgent");
    }

    startThread();
  }

  public void suspend() {
    super.suspend();
    stopThread();
    bb.suspend();
  }

  public void resume() {
    super.resume();
    bb.resume();
    startThread();
  }

  public void stop() {
    super.stop();
    stopThread();
    if (bb != null) {
      sb.releaseService(this, BlackboardForAgent.class, null);
      bb = null;
    }
  }

  public void unload() {
    super.unload();

    if (mss != null) {
      // mss.unregister?
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }
  }

  private void startThread() {
    synchronized (lock) {
      if (!isStarted) {
        getThread().start();
        isStarted = true;
      }
    }
  }

  private void stopThread() {
    synchronized (lock) {
      if (isStarted) {
        getThread().halt();
        isStarted = false;
        thread = null;
      }
    }
  }

  private final void receiveMessages(List messages) {
    try {
      bb.receiveMessages(messages);
    } catch (Exception e) {
      log.error("Uncaught Exception while handling Queued Messages", e);
    }
  }

  private QueueHandlerThread getThread() {
    synchronized (lock) {
      if (thread == null) {
        QueueClient qc = new QueueClient() {
          public MessageAddress getMessageAddress() {
            return localAgent;
          }
          public void receiveQueuedMessages(List messages) {
            receiveMessages(messages);
          }
        };
        thread = new QueueHandlerThread(qc);
      }
      return thread;
    }
  }

  interface QueueClient {
    MessageAddress getMessageAddress();
    void receiveQueuedMessages(List messages);
  }

  private static final class QueueHandlerThread extends Thread {
    private QueueClient client;
    private final List queue = new ArrayList();
    private final List msgs = new ArrayList();
    private boolean running = true;
    public QueueHandlerThread(QueueClient client) {
      super(client.getMessageAddress()+"/RQ");
      this.client = client;
    }
    public void halt() {
      synchronized (queue) {
        running = false;
        queue.notify();
      }
      try {
        // wait for this thread to stop
        join(); 
      } catch (InterruptedException ie) {
      }
      client = null;
    }
    public void run() {
      while (true) {
        synchronized (queue) {
          if (!running) {
            return;
          }
          while (running && queue.isEmpty()) {
            try {
              queue.wait();
            } catch (InterruptedException ie) {
            }
          }
          msgs.addAll(queue);
          queue.clear();
        }
        if (!msgs.isEmpty()) {
          client.receiveQueuedMessages(msgs);
          msgs.clear();
        }
      }
    }
    public void addMessage(ClusterMessage m) {
      synchronized (queue) {
        if (!running) {
          System.err.println(
              "Queue is not running, message will be ignored!");
        }
        queue.add(m);
        queue.notify();
      }
    }
  }
}
