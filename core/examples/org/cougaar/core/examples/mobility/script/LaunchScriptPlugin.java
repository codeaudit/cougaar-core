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
package org.cougaar.core.examples.mobility.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
