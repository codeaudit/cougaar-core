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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The Reconcile component watches blackboard message traffic
 * and periodically checks the white pages for agent restarts,
 * which require blackboard-to-blackboard state reconciliation.
 */
public final class Reconcile
extends GenericStateModelAdapter
implements Component
{

  private static final boolean VERBOSE_RESTART = true;//false;
  private static final long RESTART_CHECK_INTERVAL = 43000L;

  private ServiceBroker sb;

  private LoggingService log;
  private WhitePagesService wps;

  private PersistenceService ps;
  private PersistenceClient pc;

  private ReconcileAddressWatcherServiceProvider rawsp;
  private ReconcileEnablerServiceProvider resp;

  private BlackboardForAgent bb;

  private Schedulable restartTimer;

  // map of agent name to most recently observed incarnation, used
  // to detect the restart of remote agents, which requires a
  // resync beteen this agent and the restarted agent.
  private final Map incarnationMap = new HashMap();

    private final Map callbackMap = new HashMap();


    private class BlockingWPCallback implements Callback {
	AddressEntry entry;
	boolean completed;
	
	BlockingWPCallback()
	{
	    completed = false;
	}
	
	public void execute(Response response) 
	{
	    completed = true;
	    if (response.isSuccess()) {
		if (log.isInfoEnabled()) {
		    log.info("WP Response: "+response);
		}
		entry = ((Response.Get) response).getAddressEntry();
	    } else {
		log.error("WP Error: "+response);
	    }
	}
    }


  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

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
    if (o instanceof Map) {
      Map m = (Map) o;
      synchronized (incarnationMap) {
        incarnationMap.putAll(m);
      }
    }
    o = null;

    rawsp = new ReconcileAddressWatcherServiceProvider();
    sb.addService(ReconcileAddressWatcherService.class, rawsp);

    resp = new ReconcileEnablerServiceProvider();
    sb.addService(ReconcileEnablerService.class, resp);
  }

  public void start() {
    super.start();
    // called later via ReconcileEnablerService:
    //startRestartTimer();
  }

  public void suspend() {
    super.suspend();
    // called earlier via ReconcileEnablerService:
    //stopRestartTimer();
  }

  public void resume() {
    super.resume();
    // called later via ReconcileEnablerService:
    //startRestartTimer();
  }

  public void stop() {
    super.stop();
    // called earlier via ReconcileEnablerService:
    //stopRestartTimer();
  }

  public void unload() {
    super.unload();

    if (resp != null) {
      sb.revokeService(ReconcileEnablerService.class, resp);
      resp = null;
    }
    if (rawsp != null) {
      sb.revokeService(ReconcileAddressWatcherService.class, rawsp);
      rawsp = null;
    }

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

    Map m;
    synchronized (incarnationMap) {
      m = new HashMap(incarnationMap);
    }
    return m;
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

  /*
   * Ensure that we are tracking incarnations number for the agent at
   * a given address. If the specified agent is not in the restart
   * incarnation map it means we have never before communicated with that 
   * agent or we have just restarted and are sending restart messages. In 
   * both cases, it is ok to store the special "unknown incarnation" marker
   * because we do not want to detect any restart.
   **/
  private void recordAddress(MessageAddress agentId) {
    // only include agent addresses in restart checking
    synchronized (incarnationMap) {
      if (incarnationMap.get(agentId) == null) {
        if (VERBOSE_RESTART && log.isDebugEnabled()) {
          log.debug("Adding "+agentId+" to restart table");
        }
        incarnationMap.put(agentId, new Long(0L));
      }
    }
  }

  /**
   * Get the latest incarnation number for the specified agent.
   *
   * @return -1 if the WP lacks a version entry for the agent
   */
    private long lookupCurrentIncarnation(MessageAddress agentId) 
    {
	AddressEntry entry;
	BlockingWPCallback callback;

	callback = (BlockingWPCallback) callbackMap.get(agentId);
	    
	if (callback == null) {
	    // No pending callback yet.
	    callback = new BlockingWPCallback();
	    wps.get(agentId.getAddress(), "version", callback);
	    if (callback.completed) {
		// cache hit
		entry = callback.entry;
	    } else {
		// No cache hit.  Remember the callback.
		callbackMap.put(agentId, callback);
		return -1;
	    }
	} else if (!callback.completed) {
	    // Pending callback not completed yet
	    return -1;
	} else {
	    // Pending callback completed
	    entry = callback.entry;
	    callbackMap.remove(agentId);
	}

	if (entry != null) {
	    // If it gets here, the entry is available
	    String path = entry.getURI().getPath();
	    int end = path.indexOf('/', 1);
	    String incn_str = path.substring(1, end);
	    return Long.parseLong(incn_str);
	} else {
	    // log this?
	    // Return error code
	    return -1;
	}


    }

  private void startRestartTimer() {
    if (restartTimer != null) {
      return;
    }

    bb = (BlackboardForAgent)
      sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null) {
      throw new RuntimeException(
          "Unable to obtain BlackboardForAgent");
    }

    Runnable taskBody = 
      new Runnable() {
        public void run() {
          checkRestarts();
        }
      };
    ThreadService threadService = (ThreadService)
      sb.getService(this, ThreadService.class, null);
    restartTimer = threadService.getThread(this, taskBody, "Reconciler");
    sb.releaseService(this, ThreadService.class, threadService);

    restartTimer.schedule(
        RESTART_CHECK_INTERVAL,
        RESTART_CHECK_INTERVAL);
  }

  private void stopRestartTimer() {
    if (restartTimer == null) {
      return;
    }

    if (bb != null) {
      sb.releaseService(this, BlackboardForAgent.class, bb);
      bb = null;
    }

    restartTimer.cancelTimer();
    restartTimer = null;
  }

    private boolean doReconcile(MessageAddress agentId,
				long cachedInc,
				long currentInc)
    {
      if (VERBOSE_RESTART && log.isDebugEnabled()) {
        log.debug(
            "Update agent "+agentId+
            " incarnation from "+cachedInc+
            " to "+currentInc);
      }
      if (currentInc > 0 && currentInc != cachedInc) {
        Long l = new Long(currentInc);
        synchronized (incarnationMap) {
          incarnationMap.put(agentId, l);
        }
        if (cachedInc > 0) {
	  return true;
        }
      }
      return false;
    }


  /**
   * Periodically called to check remote agent restarts.
   * <p>
   * The incarnation map has an entry for every agent that we have
   * communicated with.  The value is the last known incarnation number
   * of the agent.
   * <p>
   * The first time we check restarts, we have ourself restarted so we
   * proceed to verify our state against _all_ the other agents. We do
   * this because we have no record with whom we have been in
   * communication. In this case, we notify the blackboard, which will
   * instruct the domains to reconcile the objects in the local
   * blackboard.  Messages will be sent only to those agents for which
   * we have communicated with.  The sending of those messages will add
   * entries to the restart incarnation map. So after doing a restart 
   * with an agent if there is an entry in the map for that agent, we set
   * the saved incarnation number to the current value for that agent.
   * This avoids repeating the restart later. If the current incarnation
   * number differs, the agent must have restarted so we initiate the
   * restart reconciliation process.
   */
  private void checkRestarts() {
    if (VERBOSE_RESTART && log.isDebugEnabled()) {
      log.debug("Check restarts");
    }
    // snapshot the incarnation map
    Map restartMap;
    synchronized (incarnationMap) {
      if (incarnationMap.isEmpty()) {
        return; // nothing to do
      }
      restartMap = new HashMap(incarnationMap);
    }
    // get the latest incarnations from the white pages
    List reconcileList = null;
    for (Iterator iter = restartMap.entrySet().iterator();
        iter.hasNext();
        ) {
	Map.Entry me = (Map.Entry) iter.next();
	MessageAddress agentId = (MessageAddress) me.getKey();
	long cachedInc = ((Long) me.getValue()).longValue();
	long currentInc;
	boolean must_reconcile;
	currentInc = lookupCurrentIncarnation(agentId);
	must_reconcile = doReconcile(agentId, cachedInc, currentInc);
	if (must_reconcile) {
	    // must reconcile with this agent
	    if (reconcileList == null) {
		reconcileList = new ArrayList();
	    }
	    reconcileList.add(agentId);
	}
    }
    // reconcile with any agent (that we've communicated with) that
    // has a new incarnation number
    int n = (reconcileList == null ? 0 : reconcileList.size());
    for (int i = 0; i < n; i++) {
      MessageAddress agentId = (MessageAddress) reconcileList.get(i);
      if (log.isInfoEnabled()) {
        log.info(
            "Detected (re)start of agent "+agentId+
            ", synchronizing blackboards");
      }
      bb.restartAgent(agentId);
    }
  }

  private final class ReconcileAddressWatcherServiceProvider
    implements ServiceProvider {
      private final ReconcileAddressWatcherService raws;
      public ReconcileAddressWatcherServiceProvider() {
        raws = new ReconcileAddressWatcherService() {
          public void sentMessageTo(MessageAddress addr) {
            recordAddress(addr);
          }
          public void receivedMessageFrom(MessageAddress addr) {
            recordAddress(addr);
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (ReconcileAddressWatcherService.class.isAssignableFrom(serviceClass)) {
          return raws;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }

  private final class ReconcileEnablerServiceProvider
    implements ServiceProvider {
      private final ReconcileEnablerService res;
      public ReconcileEnablerServiceProvider() {
        res = new ReconcileEnablerService() {
          public void startTimer() {
            startRestartTimer();
          }
          public void stopTimer() {
            stopRestartTimer();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (ReconcileEnablerService.class.isAssignableFrom(serviceClass)) {
          return res;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }
}
