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

package org.cougaar.core.plugin.completion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.SimpleUniqueObject;
import org.cougaar.core.util.UID;

public class CompletionRelay
  extends SimpleUniqueObject
  implements Relay.Source, Relay.Target, Relay.TargetFactory, NotPersistable
{
  transient MessageAddress sourceAddress;
  transient Set targetAddresses;
  // Map from target to Laggard
  transient Map laggardsByTarget;
  transient Laggard response;
  transient Token token;
  private double completionThreshold;
  private double cpuThreshold;
  private int persistenceCount = 0;
  transient int oldPersistenceCount = 0;

  CompletionRelay(MessageAddress source, Set targets,
                  double completionThreshold, double cpuThreshold)
  {
    this.sourceAddress = source;
    this.completionThreshold= completionThreshold;
    this.cpuThreshold= cpuThreshold;
    this.targetAddresses = targets;
    laggardsByTarget = new HashMap();
  }

  // Application Target API
  void setResponseLaggard(Laggard laggard) {
    response = laggard;
  }

  Laggard getResponseLaggard() {
    return response;
  }

  double getCompletionThreshold() {
    return completionThreshold;
  }

  double getCPUThreshold() {
    return cpuThreshold;
  }

  boolean persistenceNeeded() {
    return persistenceCount != oldPersistenceCount;
  }

  void resetPersistenceNeeded() {
    if (response != null) {
      oldPersistenceCount = persistenceCount;
    }
  }

  // Application Source API
  SortedSet getLaggards() {
    synchronized (laggardsByTarget) {
      return new TreeSet(laggardsByTarget.values());
    }
  }

  void setPersistenceNeeded() {
    persistenceCount++;
  }

  void setTargets(Set newTargets) {
    this.targetAddresses = newTargets;
  }

  // Relay.Source implementation
  public Set getTargets() {
    return targetAddresses == null ? Collections.EMPTY_SET : targetAddresses;
  }
  public Object getContent() {
    return this;
  }
  public TargetFactory getTargetFactory() {
    return this;
  }
  public int updateResponse(MessageAddress target, Object response) {
    if (response instanceof Laggard) {
      synchronized (laggardsByTarget) {
        laggardsByTarget.put(target, response);
      }
      return Relay.RESPONSE_CHANGE;
    } else {
      throw new IllegalArgumentException("Not a CompletionResponse: " + response);
    }
  }

  // TargetFactory implementation
  public Relay.Target create(UID uid,
                             MessageAddress source,
                             Object content,
                             Token token)
  {
    CompletionRelay result;
    if (targetAddresses != null) {
      // intra-vm case, must clone
      result = new CompletionRelay(source, null, completionThreshold, cpuThreshold);
      result.setUID(uid);
    } else {
      result = this;
      result.sourceAddress = source;
    }
    result.token = token;
    return result;
  }
    
  // Target implementation
  public MessageAddress getSource() {
    return sourceAddress;
  }

  public Object getResponse() {
    return response;
  }

  public int updateContent(Object newContent, Token token) {
    if (newContent instanceof CompletionRelay) {
      CompletionRelay cr = (CompletionRelay) newContent;
      if (this.completionThreshold != cr.completionThreshold ||
          this.cpuThreshold != cr.cpuThreshold ||
          this.persistenceCount != cr.persistenceCount) {
        this.cpuThreshold = cr.cpuThreshold;
        this.completionThreshold = cr.completionThreshold;
        this.persistenceCount = cr.persistenceCount;
        return Relay.CONTENT_CHANGE;
      }
      return Relay.NO_CHANGE;
    }
    throw new IllegalArgumentException("Not a CompletionRelay: " + newContent);
  }
}
