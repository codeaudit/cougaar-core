/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.examples.mobility.script;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import org.cougaar.core.examples.mobility.ldm.MobilityTestFactory;
import org.cougaar.core.examples.mobility.ldm.Proc;
import org.cougaar.core.examples.mobility.ldm.Script;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;

/**
 * This is an optional plugin that launches a script at startup.
 * <p>
 * One parameter is expected, to specify for the script filename.
 * The file is located using the ConfigFinder.
 * <p>
 * Upon startup this plugin creates and publish-adds a matching
 * mobility script and proc.
 *
 * @see org.cougaar.core.mobility.ldm.ScriptParser script language
 */
public class LaunchScriptPlugin 
extends ComponentPlugin 
{

  private LoggingService log;
  private DomainService domain;

  private MobilityFactory mobilityFactory;
  private MobilityTestFactory mobilityTestFactory;

  public void setLoggingService(LoggingService log) {
    this.log = (log != null ? log : LoggingService.NULL);
  }

  public void setDomainService(DomainService domain) {
    if (domain != null) {
      this.domain = domain;
      this.mobilityFactory = 
        (MobilityFactory) domain.getFactory("mobility");
      if (mobilityFactory == null) {
        throw new RuntimeException(
            "Mobility factory (and domain \"mobility\")"+
            " not enabled");
      }
      this.mobilityTestFactory = 
        (MobilityTestFactory) domain.getFactory("mobilityTest");
      if (mobilityTestFactory == null) {
        throw new RuntimeException(
            "Mobility Test factory (and domain"+
            " \"mobilityTest\") not enabled");
      }
    }
  }

  public void unload() {
    if (domain != null) {
      getServiceBroker().releaseService(
          this, DomainService.class, domain);
      domain = null;
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    if (blackboard.didRehydrate()) {
      // already created our script & proc
      return;
    }

    // get the script file name
    Collection c = getParameters();
    if (c == null || c.size() != 1) {
      throw new IllegalArgumentException(
          "Expecting one plugin parameter for the filename, not "+
          c);
    }
    String file = (String) c.iterator().next();
  
    // start the script
    launchScript(file);
  }

  protected void execute() {
    // never
  }

  protected void launchScript(String file) {
    if (log.isDebugEnabled()) {
      log.debug("Read script from file "+file);
    }
    String content = readFile(file);
    Script script = mobilityTestFactory.createScript(content);
    if (log.isDebugEnabled()) {
      log.debug("Created new script: "+script);
    }
    blackboard.publishAdd(script);
    Proc proc = mobilityTestFactory.createProc(script.getUID());
    if (log.isDebugEnabled()) {
      log.debug("Created new proc: "+proc);
    }
    blackboard.publishAdd(proc);
    if (log.isInfoEnabled()) {
      log.info(
          "Launched script \""+file+
          "\" (script: "+script.getUID()+
          ", proc: "+proc.getUID()+")");
    }
  }

  protected String readFile(String file) {
    StringBuffer ret = new StringBuffer();
    try {
      InputStream is = getConfigFinder().open(file);
      BufferedReader br = 
        new BufferedReader(
            new InputStreamReader(is));
      char[] buf = new char[1024];
      while (true) {
        int len = br.read(buf);
        if (len < 0) {
          break;
        }
        ret.append(buf, 0, len);
      }
      br.close();
    } catch (IOException ioe) {
      throw new RuntimeException(
        "Unable to read file \""+file+"\"", ioe);
    }
    return ret.toString();
  }

}
