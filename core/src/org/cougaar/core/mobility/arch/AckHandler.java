/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility.arch;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.GenericStateModel;

/**
 */
public class AckHandler extends AbstractHandler {

  public AckHandler(MobilitySupport support) {
    super(support);
  }

  public void run() {
    ack();
  }

  /**
   * Received an ACK response from the destination node.
   */
  private void ack() {

    // FIXME race condition between move & agent-add!

    // FIXME do handshake

    if (log.isInfoEnabled()) {
      log.info("Agent "+id+" transfer ack from "+sender);
    }

    GenericStateModel model = takePendingModel();
    if (model == null) {
      if (log.isErrorEnabled()) {
        log.error("Ack on transfer for non-existing agent "+id);
      }
      return;
    }

    try {

      model.stop();
      model.unload();
      removeAgent();

      onRemoval();

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error("Agent removal after move failed", e);
      }

    } finally {
      // GC!
      model = null;
    }

    // agent will be GC'ed now

    if (log.isInfoEnabled()) {
      log.info("Agent "+id+" removed");
    }

  }

  public String toString() {
    return "Move (ack) of agent "+id;
  }
}
