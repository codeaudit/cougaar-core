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

import java.io.IOException;
import java.sql.SQLException;
import org.cougaar.util.log.Logging;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.GenericStateModelAdapter;

/** 
 */
public final class DBInitializerServiceComponent
extends GenericStateModelAdapter
implements Component 
{
  public static final String EXPTID_PROP = "org.cougaar.experiment.id";
  public static final String FILENAME_PROP = "org.cougaar.filename";

  private ServiceBroker sb;
  private String nodeName;
  private DBInitializerService dbInit;
  private ServiceProvider theSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setNodeIdentificationService(NodeIdentificationService nis) {
    MessageAddress nodeAddr = ((nis == null) ? null : nis.getMessageAddress());
    this.nodeName = ((nodeAddr == null) ? null : nodeAddr.getAddress());
  }

  public void load() {
    super.load();
    try {
      dbInit = createDBInitializerService();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to load database initializer service", e);
    }
    if (dbInit != null) {
      theSP = new DBInitializerServiceProvider();
      sb.addService(DBInitializerService.class, theSP);
    }
  }

  public void unload() {
    if (theSP != null) {
      sb.revokeService(DBInitializerService.class, theSP);
      theSP = null;
    }
    super.unload();
  }

  private DBInitializerService createDBInitializerService(
      ) throws IOException, SQLException {
    String filename = System.getProperty(FILENAME_PROP);
    String experimentId = System.getProperty(EXPTID_PROP);
    if ((filename == null) && (experimentId == null)) {
      // use the default "name.ini"
      filename = nodeName + ".ini";
      Logging.getLogger(getClass()).info(
          "Got no filename or experimentId! Using default " + filename);
      return null;
    }
    if (filename == null) {
      // use the experiment ID to read from the DB
      Logging.getLogger(getClass()).info(
          "Got no filename, using exptID " + experimentId);
      return new DBInitializerServiceImpl(experimentId);
    } 
    if (experimentId == null) {
      // use the filename provided
      Logging.getLogger(getClass()).info(
          "Got no exptID, using given filename " + filename);
      return null;
    }
    throw new IllegalArgumentException(
        "Both file name ("+FILENAME_PROP+
        ") and experiment ("+EXPTID_PROP+
        ") specified. Only one allowed.");
  }

  private class DBInitializerServiceProvider implements ServiceProvider {
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (serviceClass != DBInitializerService.class) {
        throw new IllegalArgumentException(
            getClass()+" does not furnish "+serviceClass);
      }
      return dbInit;
    }

    public void releaseService(ServiceBroker sb, Object requestor,
        Class serviceClass, Object service)
    {
    }
  }

}
