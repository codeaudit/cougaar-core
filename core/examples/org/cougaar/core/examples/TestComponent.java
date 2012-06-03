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

package org.cougaar.core.examples;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;

/** Trivial component which does nothing but print a message when it is loaded and 
 * dumps a stack to indicate from where the code is being invoked.
 * Will try to print information about which agent and node it is in (if known at load point),
 * and any parameters it was invoked with.
 **/
public class TestComponent
  extends ComponentSupport
{
  private Object param = null;

  public void setParameter(Object o) {
    param = o;
  }

  @Override
public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    
    String nodeName = "unknown";
    {
      NodeIdentificationService nis =
        sb.getService(this, NodeIdentificationService.class, null);
      if (nis != null) {
        nodeName = nis.getMessageAddress().toString();
      }
    }

    String agentName = "unknown";
    {
      AgentIdentificationService ais =
        sb.getService(this, AgentIdentificationService.class, null);
      if (ais != null) {
        agentName = ais.getName();
      }
    }

    System.err.println("Loaded ComponentTest("+param+") into "+nodeName+"/"+agentName+":");
    Thread.dumpStack();
  }

}
