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

package org.cougaar.core.agent;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The MessageSwitchShutdown component invokes the
 * {@link MessageSwitchShutdownService} during agent
 * suspend and resume.
 *
 * @see MessageSwitchUnpendService 
 */
public final class MessageSwitchShutdown
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private MessageSwitchShutdownService msss;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    msss = (MessageSwitchShutdownService)
      sb.getService(
          this, MessageSwitchShutdownService.class, null);
    if (msss == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchShutdownService");
    }
  }
  public void suspend() {
    super.suspend();
    msss.shutdown();
  }
  public void resume() {
    super.resume();
    msss.restore();
  }
  public void unload() {
    super.unload();
    if (msss != null) {
      sb.releaseService(
          this, MessageSwitchShutdownService.class, msss);
      msss = null;
    }
  }
}
