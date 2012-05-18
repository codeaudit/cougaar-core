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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;

/** A very simple Component which can be plugged into just about any
 * non-descriminating insertion points in the system.  When loaded, it
 * prints a short message describing the node and agent where it is loaded
 * and then dumps a stack. <br>
 * This class is very useful for debugging the component model and load process.
 * It is here (in the core module) because it depends on core cougaar (identification) services,
 **/
public class DummyComponent
  extends ComponentSupport
{
  String p = "unknown";
  public void setParameter(Object o) {
    if (o instanceof Collection) {
      Iterator it =((Collection)o).iterator(); 
      if (it.hasNext()) p = (String)it.next();
    }
  }

  @Override
public void load() {
    ServiceBroker sb = getBindingSite().getServiceBroker();

    // what node are we in?
    String nn = "?";
    {
      NodeIdentificationService nis = sb.getService(this,NodeIdentificationService.class,null);
      if (nis != null) {
        nn = nis.getMessageAddress().toString();
      }
    }

    // what agent?
    String an = "?";
    {
      AgentIdentificationService ais = sb.getService(this,AgentIdentificationService.class,null);
      if (ais != null) {
        an = ais.getName();
      }
    }

    System.err.println("Loading DummyComponent("+p+") in "+nn+"/"+an+":");
    Thread.dumpStack();
    super.load();
  }
}
