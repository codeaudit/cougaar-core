/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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
package org.cougaar.core.examples.mobility.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.examples.mobility.ldm.MobilityTestFactory;
import org.cougaar.core.examples.mobility.ldm.Proc;
import org.cougaar.core.examples.mobility.ldm.Script;
import org.cougaar.core.examples.mobility.ldm.ScriptGoto;
import org.cougaar.core.examples.mobility.ldm.ScriptLabel;
import org.cougaar.core.examples.mobility.ldm.ScriptStep;
import org.cougaar.core.examples.mobility.ldm.Step;
import org.cougaar.core.examples.mobility.ldm.StepOptions;
import org.cougaar.core.examples.mobility.ldm.StepStatus;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mobility.ldm.MoveAgent.Status;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin watches for new script Procs and
 * runs them.
 * <p>
 * Steps are created by this plugin.
 */
public class ProcRunnerPlugin 
extends ComponentPlugin 
{

  private static final UnaryPredicate PROCESS_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Proc);
      }
    };

  private static final UnaryPredicate SCRIPT_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Script);
      }
    };

  private MessageAddress todd;
  private MessageAddress agentId;
  private MessageAddress nodeId;

  private IncrementalSubscription scriptSub;
  private IncrementalSubscription procSub;
  private IncrementalSubscription stepSub;

  private LoggingService log;
  private DomainService domain;

  private MobilityFactory mobilityFactory;
  private MobilityTestFactory mobilityTestFactory;

  // Non-persisted cache of blackboard objects, for quick access. 
  // On rehydration it is fully reconstructed from the blackboard.
  //
  // maps proc UID to internal Entry
  private Map uidToEntry = new HashMap(13);

  public void load() {
    super.load();

    // get the logger
    log = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      getServiceBroker().getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }
    todd=agentId;

    // get the nodeId
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      getServiceBroker().getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }

    // get the mobility factories
    this.domain = (DomainService)
      getServiceBroker().getService(
          this,
          DomainService.class,
          null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain domain service");
    }
    this.mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain \"mobility\")"+
          " not enabled");
    }
    this.mobilityTestFactory = 
      (MobilityTestFactory) domain.getFactory("mobilityTest");
    if (mobilityTestFactory == null) {
      throw new RuntimeException(
          "Mobility Test factory (and domain"+
          " \"mobilityTest\") not enabled");
    }

    if (log.isDebugEnabled()) {
      log.debug(todd+"Loaded");
    }
  }

  public void unload() {
    if (domain != null) {
      getServiceBroker().releaseService(
          this, DomainService.class, domain);
      domain = null;
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    // subscribe to procs that we'll execute
    procSub = (IncrementalSubscription)
      blackboard.subscribe(PROCESS_PRED);

    // subscribe to scripts, in case they're removed
    scriptSub = (IncrementalSubscription)
      blackboard.subscribe(SCRIPT_PRED);

    // subscribe to our own steps
    stepSub = (IncrementalSubscription)
      blackboard.subscribe(createStepPredicate(agentId));

    if (blackboard.didRehydrate()) {
      // recreate cache from blackboard
      recreateEntries();
    } else {
      // create new cache
    }
  }

  protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug(todd+"Execute");
    }

    // watch procs
    if (procSub.hasChanged()) {
      // added procs
      Enumeration en = procSub.getAddedList();
      while (en.hasMoreElements()) {
        Proc proc = (Proc) en.nextElement();
        addedProc(proc);
      }
      // ignore changes: all are done this plugin
      // watch removes
      en = procSub.getRemovedList();
      while (en.hasMoreElements()) {
        Proc proc = (Proc) en.nextElement();
        removedProc(proc);
      }
    }

    // watch scripts
    if (scriptSub.hasChanged()) {
      // ignore adds: separate plugins create procs
      // ignore changes: immutable
      Enumeration en = scriptSub.getRemovedList();
      while (en.hasMoreElements()) {
        Script script = (Script) en.nextElement();
        removedScript(script);
      }
    }

    // watch steps
    if (stepSub.hasChanged()) {
      // ignore adds: this plugin did it
      // watch changes
      Enumeration en = stepSub.getChangedList();
      while (en.hasMoreElements()) {
        Step step = (Step) en.nextElement();
        changedStep(step);
      }
      // ignore removes
    }
  }

  private void addedProc(Proc proc) {

    UID procUID = proc.getUID();
    UID scriptUID = proc.getScriptUID();

    // see if already listed
    Object oldEntry = uidToEntry.get(procUID);
    if (oldEntry != null) {
      // maybe just rehydration
      if (log.isDebugEnabled()) {
        log.debug(
            "Proc "+procUID+" with script "+
            scriptUID+" overrides existing entry");
      }
      return;
    }

    // lookup script
    Script script = null;
    Collection scripts = scriptSub.getCollection();
    Iterator iter = scripts.iterator();
    for (int i = 0, n = scripts.size(); i < n; i++) {
      Script s = (Script) iter.next();
      if (scriptUID.equals(s.getUID())) {
        script = s;
        break;
      }
    }
    if (script == null) {
      if (log.isErrorEnabled()) {
        log.error(
            "Proc "+procUID+" added with unknown script "+
            scriptUID);
      }
      return;
    }

    // create entry
    Entry entry = new Entry(proc, script);
    uidToEntry.put(procUID, entry);

    advanceEntry(entry);
  }

  private void removedProc(Proc proc) {
    // find entry
    Entry entry = (Entry) uidToEntry.remove(proc.getUID());
    if (entry == null) {
      // not listed?
      return;
    }

    // kill step
    Step step = entry.step;
    if (step != null) {
      blackboard.publishRemove(step);
    }
  }

  private void changedStep(Step step) {
    // find entry
    Entry entry;
    if (uidToEntry.isEmpty()) {
      // not listed?
      return;
    } else {
      int n = uidToEntry.size();
      Iterator iter = uidToEntry.values().iterator();
      for (int i = 0; ; i++) {
        if (i >= n) {
          // not listed?
          return;
        }
        Entry e = (Entry) iter.next();
        if (e.step != step) {
          continue;
        }
        entry = e;
        break;
      }
    }
    advanceEntry(entry);
  }

  private void advanceEntry(Entry entry) {
    Proc proc = entry.proc;
    UID procUID = proc.getUID();
    Script script = entry.script;

    Step step = entry.step;

    // check for step in blackboard, in case of restart
    if (step == null) {
      Collection c = stepSub.getCollection();
      if (!(c.isEmpty())) {
        int n = c.size();
        Iterator iter = c.iterator();
        for (int i = 0; i < n; i++) {
          Step s = (Step) iter.next();
          Object ownerId = s.getOptions().getOwnerId();
          if (procUID.equals(ownerId)) {
            step = s;
            entry.step = s;
            break;
          }
        }
      }
    }

    // check step status
    long startTime = -1;
    if (step != null) {
      StepStatus status = step.getStatus();
      startTime = status.getStartTime();
      long endTime = status.getEndTime();
      if (endTime <= 0) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Still working on step "+step.getUID()+
              " <"+status.getStateAsString()+
              "> of proc "+procUID);
        }
        return;
      }
      if (status.getState() != StepStatus.SUCCESS) {
        if (log.isErrorEnabled()) {
          log.error(
              status.getStateAsString()+" on step "+
              step.getUID()+" (proc "+procUID+
              ", script "+script.getUID()+"), proc failed!");
        }
        // abort proc on failure
        proc.setEndTime(System.currentTimeMillis());
        blackboard.publishChange(proc);
        // remove entry
        uidToEntry.remove(entry);
        // leave step for debugging
        return;
      }
      if (log.isDebugEnabled()) {
        log.debug(
            "Completed step "+step.getUID()+
            " of proc "+proc.getUID()+
            " with status "+status);
      }
      // remove completed step
      blackboard.publishRemove(step);
      step = null;
      entry.step = null;
    }

    // get next script index
    int nextIdx = 1 + proc.getScriptIndex();
    int nGotos = 0;
    ScriptStep nextScriptStep;
    while (true) {
      if (nextIdx >= script.getSize()) {
        // end of script
        if (log.isInfoEnabled()) {
          log.info(
              "Completed proc "+procUID+
              " of script "+script.getUID());
        }
        // update proc
        proc.setEndTime(System.currentTimeMillis());
        proc.setScriptIndex(nextIdx);
        proc.setStepUID(null);
        blackboard.publishChange(proc);

        // update entry
        entry.step = null;
        return;
      }
      Script.Entry x = script.getEntry(nextIdx);
      if (x instanceof ScriptStep) {
        nextScriptStep = (ScriptStep) x;
        break;
      }
      if (x instanceof ScriptLabel) {
        ++nextIdx;
        continue;
      }
      if (!(x instanceof ScriptGoto)) {
        throw new RuntimeException(
            "Unknown script element["+nextIdx+"]: "+
            x);
      }
      nextIdx = ((ScriptGoto) x).getIndex();
      if (++nGotos > 20) {
        if (log.isErrorEnabled()) {
          ScriptGoto sg = (ScriptGoto) 
            script.getEntry(
                1 + proc.getScriptIndex());
          log.error(
              "Possible infinite loop in script "+
              script.getUID()+" beginning at "+
              sg.getName());
        }
        return;
      }
    }

    // create fleshed-out step options
    StepOptions nextOpts = 
      createStepOptions(
          proc, startTime, nextScriptStep);

    // create step
    Step nextStep = mobilityTestFactory.createStep(nextOpts);
    blackboard.publishAdd(nextStep);

    // update proc
    proc.setScriptIndex(nextIdx);
    proc.setMoveCount(1 + proc.getMoveCount());
    proc.setStepUID(nextStep.getUID());
    blackboard.publishChange(proc);

    // update entry
    entry.step = nextStep;
    
    if (log.isInfoEnabled()) {
      log.info(
          "Created step #"+proc.getMoveCount()+
          " at script index "+nextIdx+
          " (script: "+script.getUID()+
          ", proc: "+proc.getUID()+
          ", step: "+nextStep.getUID()+
          ")");
    }
  }

  private StepOptions createStepOptions(
      Proc proc, long priorMoveStartTime, ScriptStep scriptStep) {
    StepOptions opts = scriptStep.getStepOptions();
    MessageAddress newTarget = opts.getTarget();
    if (newTarget == null) {
      newTarget = agentId;
    }
    Ticket ticket = opts.getTicket();
    Object ticketId = mobilityFactory.createTicketIdentifier();
    Ticket newTicket = 
      new Ticket(
          ticketId,
          ticket.getMobileAgent(),
          ticket.getOriginNode(),
          ticket.getDestinationNode(),
          ticket.isForceRestart());
    long nowTime = System.currentTimeMillis();
    long newPauseTime = opts.getPauseTime();
    if (scriptStep.hasFlag(ScriptStep.ADD_PAUSE)) {
      newPauseTime += nowTime;
    } else if (scriptStep.hasFlag(ScriptStep.PRI_PAUSE)) {
      newPauseTime += 
        (priorMoveStartTime > 0 ? priorMoveStartTime : nowTime);
    } else if (scriptStep.hasFlag(ScriptStep.REL_PAUSE)) {
      newPauseTime += proc.getStartTime();
    }
    long newTimeoutTime = opts.getTimeoutTime();
    if (scriptStep.hasFlag(ScriptStep.ADD_TIMEOUT)) {
      newTimeoutTime += newPauseTime;
    } else if (scriptStep.hasFlag(ScriptStep.PRI_TIMEOUT)) {
      newTimeoutTime += 
        (priorMoveStartTime > 0 ? priorMoveStartTime : nowTime);
    } else if (scriptStep.hasFlag(ScriptStep.REL_TIMEOUT)) {
      newTimeoutTime += proc.getStartTime();
    }
    StepOptions ret =
      new StepOptions(
          proc.getUID(),
          agentId,
          newTarget,
          newTicket,
          newPauseTime,
          newTimeoutTime);
    return ret;
  }

  private void removedScript(Script script) {
    if (!(uidToEntry.isEmpty())) {
      int n = uidToEntry.size();
      Iterator iter = uidToEntry.values().iterator();
      for (int i = 0; i < n; i++) {
        Entry entry = (Entry) iter.next();
        if (entry.script != script) {
          continue;
        }
        // remove entry
        iter.remove();
        --n;
        // cleanup
        Step step = entry.step;
        if (step != null) {
          blackboard.publishRemove(step);
        }
        Proc proc = entry.proc;
        if (procSub.contains(proc)) {
          // kill proc!  shouldn't happen if
          // proc creator does cleanup.
          blackboard.publishRemove(proc);
        }
      }
    }
  }

  private void recreateEntries() {
    // this should be empty!
    uidToEntry.clear();

    // recreate from blackboard contents
    Collection c = procSub.getCollection();
    if (!(c.isEmpty())) {
      int n = c.size();
      Iterator iter = c.iterator();
      for (int i = 0; i < n; i++) {
        Proc proc = (Proc) iter.next();
        addedProc(proc);
      }
    }
  }

  private static UnaryPredicate createStepPredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof Step) {
            Step step = (Step) o;
            MessageAddress target = step.getOptions().getTarget();
            return agentId.equals(target);
          }
          return false;
        }
      };
  }

  private static class Entry {

    public final Proc proc;
    public final Script script;
    public Step step;

    public Entry(
        Proc proc,
        Script script) {
      this.proc = proc;
      this.script = script;
      if ((proc == null) ||
          (script == null)) {
        throw new IllegalArgumentException(
            "null proc/script");
      }
    }

    public String toString() {
      return 
        "Entry {"+
        "\n procUID: "+proc.getUID()+
        "\n proc:    "+proc+
        "\n script:  "+script+
        "\n step:    "+step+
        "\n}";
    }
  }

}
