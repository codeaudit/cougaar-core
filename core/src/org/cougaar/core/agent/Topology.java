/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The Topology component binds the agent's "version://" and
 * "node://" information in the white pages, and preserves the
 * incarnation across agent moves.
 * <p> 
 */
public final class Topology
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private WhitePagesService wps;

  private PersistenceService ps;
  private PersistenceClient pc;

  private MessageAddress localAgent;
  private MessageAddress localNode;

  // incarnation for this agent, which is incremented every time
  // this agent restarts but not when the agent moves.
  private long incarnation;

  // move identity of this agent, which is incremented every time this
  // agent moves.
  private long moveId;

  private boolean needsRestart = true;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // get our local agent's address
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // get our local node's address
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis != null) {
      localNode = nis.getMessageAddress();
      sb.releaseService(
          this, NodeIdentificationService.class, nis);
    }

    // get wp
    wps = (WhitePagesService) 
      sb.getService(this, WhitePagesService.class, null);
    if (wps == null) {
      throw new RuntimeException(
          "Unable to obtain WhitePagesService");
    }

    register_persistence();

    // get mobile state
    Object o = rehydrate();
    if (o instanceof TopologyState) {
      TopologyState ts = (TopologyState) o;
      needsRestart = false;
      incarnation = ts.incarnation;
      // ignore moveId, maybe use someday
    }
    o = null;

    try {
      bindRestart();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to load restart checker", e);
    }
  }

  public void start() {
    super.start();
    // do restart reconciliation if necessary
    reconcileBlackboard();
  }

  public void unload() {
    super.unload();

    unregister_persistence();

    if (wps != null) {
      sb.releaseService(
          this, WhitePagesService.class, wps);
      wps = null;
    }
  }

  private Object captureState() {
    if (getModelState() == ACTIVE) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring persist while active");
      }
      return null;
    }
    return new TopologyState(
        localAgent,
        incarnation,
        moveId);
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName();
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          Object o = captureState();
          // must return mutable list!
          List l = new ArrayList(1);
          l.add(o);
          return l;
        }
      };
    ps = 
      (PersistenceService)
      sb.getService(
          pc, PersistenceService.class, null);
  }

  private void unregister_persistence() {
    if (ps != null) {
      sb.releaseService(
          pc, PersistenceService.class, ps);
      ps = null;
      pc = null;
    }
  }

  private Object rehydrate() {
    RehydrationData rd = ps.getRehydrationData();
    if (rd == null) {
      if (log.isInfoEnabled()) {
        log.info("No rehydration data found");
      }
      return null;
    }

    List l = rd.getObjects();
    rd = null;
    int lsize = (l == null ? 0 : l.size());
    if (lsize < 1) {
      if (log.isInfoEnabled()) {
        log.info("Invalid rehydration list? "+l);
      }
      return null;
    }
    Object o = l.get(0);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("Null rehydration state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Found rehydrated state");
      if (log.isDetailEnabled()) {
        log.detail("state is "+o);
      }
    }

    return o;
  }

  /**
   * The local agent has restarted.
   */
  private void reconcileBlackboard() {
    if (!(needsRestart)) {
      if (log.isInfoEnabled()) {
        log.info("No restart blackboard synchronization required");
      }
      return;
    }

    needsRestart = false;
    if (log.isInfoEnabled()) {
      log.info("Restarting, synchronizing blackboards");
    }

    BlackboardForAgent bb = (BlackboardForAgent)
      sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null) {
      throw new RuntimeException(
          "Unable to find BlackboardForAgent");
    }
    try {
      // restart this agent.  The "null" is shorthand for 
      // "all agents that are not this agent".
      bb.restartAgent(null);
    } catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info("Restart failed", e);
      }
    }
    sb.releaseService(this, BlackboardForAgent.class, bb);
    bb = null;
  }

  private void bindRestart() throws Exception {
    String localHost;
    try {
      InetAddress localAddr = InetAddress.getLocalHost();
      localHost = localAddr.getHostName();
    } catch (Exception e) {
      localHost = "?";
    }

    String identifier = localAgent.getAddress();

    final LoggingService ls = log;
    Callback callback = new Callback() {
      public void execute(Response res) {
        if (res.isSuccess()) {
          if (ls.isInfoEnabled()) {
            ls.info("WP Response: "+res);
          }
        } else {
          ls.error("WP Error: "+res);
        }
      }
    };

    // register WP version numbers
    if (log.isInfoEnabled()) {
      log.info("Updating white pages");
    }
    if (incarnation == 0) {
      incarnation = System.currentTimeMillis();
    }
    moveId = System.currentTimeMillis();
    // ignore prior moveId
    URI versionURI = 
      URI.create("version:///"+incarnation+"/"+moveId);
    AddressEntry versionEntry = 
      AddressEntry.getAddressEntry(
          identifier,
          "version",
          versionURI);
    wps.rebind(versionEntry, callback); // should really pay attention

    // register WP node location
    URI nodeURI = 
      URI.create("node://"+localHost+"/"+localNode.getAddress());
    AddressEntry nodeEntry = 
      AddressEntry.getAddressEntry(
          identifier,
          "topology",
          nodeURI);
    wps.rebind(nodeEntry, callback); // really should pay attention
  }

  private static final class TopologyState
    implements Serializable {

      public final MessageAddress agentId;
      public final long incarnation;
      public final long moveId;

      public TopologyState(
          MessageAddress agentId,
          long incarnation,
          long moveId) {
        this.agentId = agentId;
        this.incarnation = incarnation;
        this.moveId = moveId;
        if (agentId == null) {
          throw new IllegalArgumentException("null agentId");
        }
      }

      public String toString() {
        return 
          "Agent "+agentId+
          ", incarnation "+incarnation+
          ", moveId "+moveId;
      }

      private static final long serialVersionUID = 1890394862083942083L;
    }
}
