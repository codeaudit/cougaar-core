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

package org.cougaar.core.wp.bootstrap.config;

import java.util.Map;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.DiscoveryService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component copies bundles found in the {@link ConfigService}
 * to the {@link DiscoveryService}.
 * <p>
 * This is the simple static bootstrap.
 */
public class ConfigDiscovery
extends GenericStateModelAdapter
implements Component, DiscoveryService.Client
{
  private ServiceBroker sb;

  private LoggingService log;

  private DiscoveryService discoveryService;

  private ConfigService configService;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void load() {
    super.load();

    configService = (ConfigService)
      sb.getService(this, ConfigService.class, null);
    if (configService == null) {
      throw new RuntimeException("Unable to obtain ConfigService");
    }

    discoveryService = (DiscoveryService)
      sb.getService(this, DiscoveryService.class, null);
    if (discoveryService == null) {
      throw new RuntimeException("Unable to obtain DiscoveryService");
    }
  }

  public void unload() {
    if (discoveryService != null) {
      sb.releaseService(this, DiscoveryService.class, discoveryService);
      discoveryService = null;
    }
    if (configService != null) {
      sb.releaseService(this, ConfigService.class, configService);
      configService = null;
    }

    super.unload();
  }

  public void startSearching() {
    Map m = configService.getBundles();
    if (log.isDetailEnabled()) {
      log.detail("startSearching, ds.update("+m+")");
    }
    discoveryService.update(m);
  }

  public void stopSearching() {
    if (log.isDetailEnabled()) {
      log.detail("stopSearching");
    }
  }
}
