/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.examples.mobility.ldm;

import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.agent.ClusterContext;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.util.UID;

/**
 * Package-private factory implementation.
 */
class MobilityTestFactoryImpl implements MobilityTestFactory {

  private MessageAddress self;
  private UIDServer uidServer;

  public MobilityTestFactoryImpl(LDMServesPlugin ldm) {
    self = ldm.getMessageAddress();
    uidServer = ((ClusterContext) ldm).getUIDServer();
  }

  public Script createScript(String text) {
    UID uid = uidServer.nextUID();
    Script script = new ScriptImpl(uid, text);
    return script;
  }

  public Proc createProc(UID scriptUID) {
    // verify that a matching script exists
    UID uid = uidServer.nextUID();
    long nowTime = System.currentTimeMillis();
    Proc proc = new ProcImpl(uid, scriptUID, nowTime);
    return proc;
  }

  public Step createStep(StepOptions options) {
    UID uid = uidServer.nextUID();
    MessageAddress actorAgent = options.getTarget();
    if ((actorAgent == null) ||
        (actorAgent.equals(self))) {
      // local step
      return new LocalStepImpl(uid, options);
    } else {
      // remote step
      return new RemoteStepImpl(uid, options);
    }
  }

}
