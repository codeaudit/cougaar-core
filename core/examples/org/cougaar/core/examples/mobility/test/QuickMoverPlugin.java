/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.examples.mobility.test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.util.UnaryPredicate;

/**
 * Ugly testing code to force rapid agent mobility.
 */
public class QuickMoverPlugin 
extends AbstractMoverPlugin 
{

  private static final String DEFAULT_ID = "id";

  // whole bunch o' moves
  // fixme to be read from a file, etc...
  private static final String[][] MOVES =
  {
    {"AgentA", null, "NodeA", "true"},
    {"AgentA", null, "NodeB", "false"},
    {"AgentB", null, "NodeA", "true"},
    {"AgentA", null, "NodeB", "false"},
    {"AgentA", null, "NodeA", "false"},
    {"AgentA", null, "NodeB", "true"},
    {"AgentB", null, "NodeB", "false"},
    {"AgentA", null, "NodeA", "false"},
    {"AgentB", null, "NodeB", "false"},
    {"AgentA", null, "NodeA", "true"},
    {"AgentA", null, "NodeB", "true"},
    {"AgentA", null, "NodeB", "true"},
    {"AgentB", null, "NodeA", "true"},
    {"AgentB", null, "NodeB", "true"},
    {"AgentB", null, "NodeA", "true"},
    {"AgentA", null, "NodeB", "true"},
  };

  private String id;
  private MyState state;

  // remember where we are in the list of MOVES
  private static class MyState implements Serializable {
    String id;
    int i;
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();

    id = DEFAULT_ID;
    Collection c = getParameters();
    if (c.size() > 0) {
      id = (String) c.iterator().next();
    }

    state = findState();

    if (blackboard.didRehydrate()) {
      if (state == null) {
        log.error("Unable to find state with id \""+id+"\"!");
      }
    } else {
      if (state != null) {
        log.error(
            "Found prior state for id \""+id+
            "\", multiple conflicting plugins?");
      }
      state = new MyState();
      state.id = id;
      blackboard.publishAdd(state);
      nextMove(null);
    }
  }

  private MyState findState() {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof MyState) &&
           (id.equals(((MyState) o).id)));
      }
    };
    Collection real = blackboard.query(pred);
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        Object o = iter.next();
        if (o instanceof MyState) {
          return ((MyState) o);
        }
      }
    }
    return null;
  }

  private void nextMove(MoveAgent ma){ 
    if (state == null) {
      log.error("No state");
      return;
    }

    int j = state.i;
    if (j < 0) {
      log.error("Mover \""+id+"\" move "+(-j)+" with prior error");
      return;
    } else if (j >= MOVES.length) {
      log.info("Mover \""+id+"\" finished all "+j+" moves");
      return;
    }

    if (ma != null) {
      blackboard.publishRemove(ma);
    }

    ++state.i;
    blackboard.publishChange(state);

    String[] sa = MOVES[j];

    Ticket t = createTicket(sa[0], sa[1], sa[2], "true".equals(sa[3]));
    requestMove(t);

    log.info(
        "Mover \""+id+"\" submitting new move "+
        t.getIdentifier()+" with ticket: "+t);
  }

  protected void handleCompletedMove(MoveAgent ma) {
    MoveAgent.Status mstat = ma.getStatus();
    if ((mstat == null) || 
        (mstat.getCode() !=  MoveAgent.Status.OKAY)) {
      if (state.i > 0) {
        state.i = -(state.i);
        blackboard.publishChange(state);
      }
      log.error(
          "Mover \""+id+"\" failed move "+
          ma.getUID()+" with status "+mstat+
          " and exception", mstat.getThrowable());
      return;
    }
    log.info(
        "Mover \""+id+"\" successful move "+
        ma.getUID()+" with status "+mstat);

    nextMove(ma);
  }

}
