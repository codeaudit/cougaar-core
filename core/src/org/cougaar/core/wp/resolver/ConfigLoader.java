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

package org.cougaar.core.wp.resolver;

import java.util.List;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component simply loads the entries listed by the
 * ConfigReader into the white pages cache as "hints".
 * 
 * @see ConfigReader
 */
public class ConfigLoader
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    LoggingService logger = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    WhitePagesService wps = (WhitePagesService)
      sb.getService(this, WhitePagesService.class, null);

    // do all our work
    List l;
    try {
      l = ConfigReader.listEntries();
    } catch (Exception e) {
      logger.error("Failed white pages configuration", e);
      l = null;
    }

    int n = (l == null ? 0 : l.size());
    for (int i = 0; i < n; i++) {
      AddressEntry ae = (AddressEntry) l.get(i);
      if (logger.isInfoEnabled()) {
        logger.info("Add bootstrap entry: "+ae);
      }
      try {
        wps.hint(ae);
      } catch (Exception e) {
        if (logger.isErrorEnabled()) {
          logger.error(
              "Unable to add bootstrap hint: "+ae, e);
        }
      }
    }

    sb.releaseService(this, WhitePagesService.class, wps);
    sb.releaseService(this, LoggingService.class, logger);
    sb = null;
  }
}
