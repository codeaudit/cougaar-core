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

package org.cougaar.planning.plugin.completion;

import java.text.SimpleDateFormat;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.EmptyIterator;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates completion information from
 * agents in a society to determin the "completion" of the current
 * tasks. In most agents, it gathers the information and forwards the
 * completion status of the agent to another agent. This process
 * continues through a hierarchy of such plugins until the plugin at
 * the root of the tree is reached. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public abstract class CompletionPlugin extends ServiceUserPlugin {
  protected CompletionPlugin(Class[] requiredServices) {
    super(requiredServices);
  }
  protected static UnaryPredicate targetRelayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof CompletionRelay) {
          CompletionRelay relay = (CompletionRelay) o;
          return relay.getSource() != null;
        }
        return false;
      }
    };

  /**
   * A Collection implementation the retains nothing. Used for a
   * Subscription to note publish events of interest, but for which
   * the actual objects are unneeded. Most of the work is done by the
   * AbstractCollection base class. We implement the minimum required
   * for a mutable Collection.
   **/
  protected class AmnesiaCollection extends AbstractCollection {
    public Iterator iterator() {
      return EmptyIterator.iterator();
    }
    public int size() {
      return 0;
    }
    public boolean add(Object o) {
      return false;
    }
  }

  protected void checkPersistenceNeeded(Collection relays) {
    for (Iterator i = relays.iterator(); i.hasNext(); ) {
      CompletionRelay relay = (CompletionRelay) i.next();
      if (relay.persistenceNeeded()) {
        setPersistenceNeeded();
        relay.resetPersistenceNeeded();
        blackboard.publishChange(relay);
      }
    }
  }

  protected abstract void setPersistenceNeeded();

  private static final SimpleDateFormat dateFormat =
    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  private static Date fdate = new Date();
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  public static String formatDate(long time) {
    synchronized (fdate) {
      fdate.setTime(time);
      return dateFormat.format(fdate);
    }
  }
}
