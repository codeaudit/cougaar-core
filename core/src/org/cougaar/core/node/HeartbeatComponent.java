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
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.PropertyParser;

/**
 * The HeartbeatComponent loads the Heartbeat class,
 * which periodically prints "."s to indicate that the
 * node is running.
 *
 * @property org.cougaar.core.agent.heartbeat
 *   If enabled, a low-priority thread runs and prints
 *   a '.' every few seconds when nothing else much is going on.
 *   This is a one-per-vm function.  Default <em>true</em>.
 */
public final class HeartbeatComponent
extends GenericStateModelAdapter
implements Component
{

  private static final boolean isHeartbeatOn =
    PropertyParser.getBoolean(
        "org.cougaar.core.agent.heartbeat",
        true);

  private Heartbeat heartbeat;

  public void setBindingSite(BindingSite bs) {}

  public void load() {
    super.load();
    if (isHeartbeatOn) {
      heartbeat = new Heartbeat();
      heartbeat.start();
    }
  }

  public void unload() {
    super.unload();
    if (isHeartbeatOn && heartbeat != null) {
      heartbeat.stop();
      heartbeat = null;
    }
  }
}
