/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
