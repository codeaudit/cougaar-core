/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.wp.resolver;

import java.util.List;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
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

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
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
