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

package org.cougaar.core.relay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.cougaar.core.blackboard.ChangeReport;

/**
 * A ChangeReport to be used when publishing changes to the target set
 * of a Relay. Failure to do so will cause dangling relay targets in
 * agents that are no longer in the target set.
 *
 * Usage is:<pre>
 *   Collection changes = Collections.singleton(new RelayChangeReport(relay));
 *   relay.setTargets(newTargets);
 *   blackboard.publishChange(relay, changes);
 * <pre>
 * The details of how you change the targets of your relay
 * implementation are, of course, your responsibility, but whatever
 * method you use, it is critical that the RelayChangeReport be
 * created before you change the targets since the change report
 * carries a copy of the old set to the RelayLP which uses it to
 * insure that the old targets are correctly reconciled with the new
 * targets.
 **/
public class RelayChangeReport implements ChangeReport {
  private Collection oldTargets;

  /**
   * Constructor from a Relay.Source. The about-to-become-old targets
   * are recorded.
   **/
  public RelayChangeReport(Relay.Source rs) {
    Set targets = rs.getTargets();
    oldTargets = new ArrayList(targets.size());
    oldTargets.addAll(targets);
  }

  /**
   * Get the recorded list of old target addresses. For use by the
   * RelayLP.
   **/
  Collection getOldTargets() {
    return oldTargets;
  }
}
