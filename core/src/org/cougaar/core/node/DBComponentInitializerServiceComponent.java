/*
* <copyright>
*  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * ServiceComponent Initializer for CSMART database component
 * Initialization.
 * <p>
 * Provides the CSMART DB compatible DBInitializerService.
 */
public class DBComponentInitializerServiceComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceProvider theInitSP;
  private ServiceProvider theDBSP;

  public void setBindingSite(BindingSite bs) {
    // this is the *node* service broker!  The NodeControlService
    // is not available until the node-agent is created...
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    LoggingService log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // Do not provide this service if there is already one there.
    // This allows someone to provide their own component to provide
    // the asset initializer service in their configuration
    DBInitializerService dbInit;
    String experimentId = System.getProperty(Node.EXPTID_PROP);
    if (sb.hasService(DBInitializerService.class)) {
      // already have DBInitializer service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DBInitializer service"+
            ", it already exists");
      }
      dbInit = (DBInitializerService) sb.getService(
          this, DBInitializerService.class, null);
    } else if (experimentId == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DBInitializer service"+
            ", missing system property -D"+
            Node.EXPTID_PROP);
      }
      dbInit = null;
    } else {
      if (log.isInfoEnabled()) {
        log.info(
            "Creating a new DBInitializer service"+
            ", using system property -D"+
            Node.EXPTID_PROP+"="+experimentId);
      }
      try {
        dbInit = new DBInitializerServiceImpl(experimentId);
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to load Database Initializer.");
      }
      theInitSP = new DBInitializerServiceProvider(dbInit);
      sb.addService(DBInitializerService.class, theInitSP);
    }

    if (sb.hasService(ComponentInitializerService.class)) {
      // already have a ComponentInitializer? 
      // Leave the existing one in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DB ComponentInitializer service"+
            ", it already exists");
      }
    } else if (dbInit == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DB ComponentInitializer service"+
            ", missing the DBInitializer service");
      }
    } else {
      if (log.isDebugEnabled())
        log.debug(
            "Creating a new DB ComponentInitializer service"+
            " based on the DBInitializer service");
      theDBSP = new DBComponentInitializerServiceProvider(dbInit);
      sb.addService(ComponentInitializerService.class, theDBSP);
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  public void unload() {
    if (theInitSP != null) {
      sb.revokeService(DBInitializerService.class, theInitSP);
      theInitSP = null;
    }
    if (theDBSP != null) {
      sb.revokeService(ComponentInitializerService.class, theDBSP);
      theDBSP = null;
    }
    super.unload();
  }

  private static class DBInitializerServiceProvider
    implements ServiceProvider {

      private final DBInitializerService dbInit;

      public DBInitializerServiceProvider(
          DBInitializerService dbInit) {
        this.dbInit = dbInit;
      }

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass != DBInitializerService.class) {
          throw new IllegalArgumentException(
              getClass()+" does not furnish "+serviceClass);
        }
        return dbInit;
      }

      public void releaseService(ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
