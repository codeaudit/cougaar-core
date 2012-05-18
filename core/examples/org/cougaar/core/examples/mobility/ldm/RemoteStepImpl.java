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

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Package-private implementation for a step where
 * the target ("actor") is a remote agent.
 * <p>
 * This allows the client on agent A to ask agent
 * B to tell agent C to move.
 * <p>
 * This uses the Relay support to transfer the data
 * between the source agent and actor agent.
 */
class RemoteStepImpl 
extends LocalStepImpl
implements Relay.Source, Relay.Target {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private transient Set _targets;

  public RemoteStepImpl(
      UID uid,
      StepOptions opt) {
    super(uid, opt);
    cacheTargets();
  }

  // Relay.Source:

  private void cacheTargets() {
    MessageAddress target = getOptions().getTarget();
    _targets = Collections.singleton(target);
  }
  public Set getTargets() {
    return _targets;
  }
  public Object getContent() {
    return getOptions();
  }
  public Relay.TargetFactory getTargetFactory() {
    return RemoteStepImplFactory.INSTANCE;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    StepStatus oldS = getStatus();
    // assert oldS != null
    StepStatus newS = (StepStatus) response;
    if (newS == null) {
      newS = StepStatus.NONE;
    }
    // assert local-agent == getSource()
    if (!(oldS.equals(newS))) {
      setStatus(newS);
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Relay.Target:

  public MessageAddress getSource() {
    return getOptions().getSource();
  }
  public Object getResponse() {
    StepStatus ss = getStatus();
    return ((ss != StepStatus.NONE) ? ss : null);
  }
  public int updateContent(Object content, Token token) {
    // currently the content (opt) is immutable
    return Relay.NO_CHANGE;
  }

  private void readObject(java.io.ObjectInputStream os) 
    throws ClassNotFoundException, java.io.IOException {
      os.defaultReadObject();
      cacheTargets();
    }

  @Override
public String toString() {
    return "remote-"+_toString();
  }

  /**
   * Simple factory implementation.
   */
  private static class RemoteStepImplFactory 
    implements Relay.TargetFactory, Serializable {

      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      public static RemoteStepImplFactory INSTANCE = 
        new RemoteStepImplFactory();

      private RemoteStepImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        StepOptions so = (StepOptions) content;
        return new RemoteStepImpl(uid, so);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}
