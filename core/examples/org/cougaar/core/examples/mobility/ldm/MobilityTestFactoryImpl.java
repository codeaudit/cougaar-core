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
package org.cougaar.core.examples.mobility.ldm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;

/**
 * Package-private factory implementation.
 */
class MobilityTestFactoryImpl implements MobilityTestFactory {

  private final MessageAddress self;
  private final UIDService uidService;

  public MobilityTestFactoryImpl(
      MessageAddress self,
      UIDService uidService) {
    this.self = self;
    this.uidService = uidService;
  }

  public Script createScript(String text) {
    UID uid = uidService.nextUID();
    Script script = new ScriptImpl(uid, text);
    return script;
  }

  public Proc createProc(UID scriptUID) {
    // verify that a matching script exists
    UID uid = uidService.nextUID();
    long nowTime = System.currentTimeMillis();
    Proc proc = new ProcImpl(uid, scriptUID, nowTime);
    return proc;
  }

  public Step createStep(StepOptions options) {
    UID uid = uidService.nextUID();
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
