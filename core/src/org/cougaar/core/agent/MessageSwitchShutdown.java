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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component invokes the {@link MessageSwitchShutdownService}
 * during agent suspend and resume.
 *
 * @see MessageSwitchUnpendService 
 */
public final class MessageSwitchShutdown
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private MessageSwitchShutdownService msss;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    msss = (MessageSwitchShutdownService)
      sb.getService(
          this, MessageSwitchShutdownService.class, null);
    if (msss == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchShutdownService");
    }
  }
  public void suspend() {
    super.suspend();
    msss.shutdown();
  }
  public void resume() {
    super.resume();
    msss.restore();
  }
  public void unload() {
    super.unload();
    if (msss != null) {
      sb.releaseService(
          this, MessageSwitchShutdownService.class, msss);
      msss = null;
    }
  }
}
