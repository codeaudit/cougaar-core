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

package org.cougaar.core.node;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.node.service.RealTimeService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The TimeComponent adds the {@link NaturalTimeService}
 * and {@link RealTimeService} services to the root-level
 * service broker, which are used to implement the agent's
 * {@link org.cougaar.core.service.AlarmService}.
 */
public final class TimeComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private TimeServiceProvider tsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    NodeControlService ncs = (NodeControlService)
      sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    tsp = new TimeServiceProvider();
    tsp.start();
    rootsb.addService(NaturalTimeService.class, tsp);
    rootsb.addService(RealTimeService.class, tsp);
  }

  public void unload() {
    super.unload();

    rootsb.revokeService(RealTimeService.class, tsp);
    rootsb.revokeService(NaturalTimeService.class, tsp);
    tsp.stop();
    tsp = null;
  }
}
