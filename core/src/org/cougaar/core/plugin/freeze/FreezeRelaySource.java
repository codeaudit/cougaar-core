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

package org.cougaar.core.plugin.freeze;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.SimpleUniqueObject;

public class FreezeRelaySource
  extends SimpleUniqueObject
  implements Relay.Source, NotPersistable
{
  transient Map targets;

  FreezeRelaySource(Set targets) {
    this.targets = new HashMap();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      Object target = i.next();
      this.targets.put(target, Collections.singleton(target));
    }
  }

  // Application Source API
  synchronized Set getUnfrozenAgents() {
    Set ret = new HashSet();
    for (Iterator i = targets.values().iterator(); i.hasNext(); ) {
      Set unfrozen = (Set) i.next();
      ret.addAll(unfrozen);
    }
    return ret;
  }

  // Relay.Source implementation
  public Set getTargets() {
    return targets.keySet();
  }
  public Object getContent() {
    return null;                // No actual content; only responses
  }
  public TargetFactory getTargetFactory() {
    return FreezeRelayFactory.getTargetFactory();
  }
  public int updateResponse(MessageAddress target, Object response) {
    synchronized (targets) {
      targets.put(target, response);
      return Relay.RESPONSE_CHANGE;
    }
  }
}
