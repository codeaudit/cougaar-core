/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.core.mts.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logging;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.plugin.asset.AssetDataReader;
import org.cougaar.planning.plugin.asset.AssetDataFileReader;

import org.cougaar.core.node.CommunityConfig;
import org.cougaar.core.node.CommunityConfigUtils;

/** An InitializerServiceProvider which delegates to the real
 * one based on runtime context.
 * @see FileInitializerServiceProvider
 * @see DBInitializerServiceProvider
 **/
public final class InitializerServiceProvider implements ServiceProvider {
  public static final String EXPTID_PROP = "org.cougaar.experiment.id";
  public static final String FILENAME_PROP = "org.cougaar.filename";

  /** the real serviceProvider as chosen by chooseISP **/
  private ServiceProvider theInitializerSP;

  // note - package protected!
  InitializerServiceProvider(String nn) {
    theInitializerSP = chooseISP(nn);
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return getISP().getService(sb, requestor, serviceClass);
  }
  
  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
    getISP().releaseService(sb, requestor, serviceClass, service);
  }

  private final ServiceProvider getISP() {
    return theInitializerSP;
  }

  private ServiceProvider chooseISP(String nodeName) {
    String filename = System.getProperty(FILENAME_PROP);
    String experimentId = System.getProperty(EXPTID_PROP);
    if (filename == null) {
      if (experimentId == null) {
        // use the default "name.ini"
        filename = nodeName + ".ini";
	Logging.getLogger(InitializerServiceProvider.class).info("Got no filename or experimentId! Using default " + filename);
      } else {
        // use the experiment ID to read from the DB
	Logging.getLogger(InitializerServiceProvider.class).info("Got no filename, using exptID " + experimentId);
      }
    } else if (experimentId == null) {
      // use the filename provided
      Logging.getLogger(InitializerServiceProvider.class).info("Got no exptID, using given filename " + filename);
    } else {
      throw new IllegalArgumentException(
          "Both file name (-f) and experiment -X) specified. "+
          "Only one allowed.");
    }


    try {
      ServiceProvider sp;
      if (filename != null) {
        sp = new FileInitializerServiceProvider();
      } else {
        sp = new DBInitializerServiceProvider(experimentId);
      }
      return sp;
    } catch (Exception e) {
      throw new RuntimeException("Exception while creating InitializerServiceProvider", e);
    }
  }
}
