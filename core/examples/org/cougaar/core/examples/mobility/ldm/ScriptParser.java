/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.examples.mobility.ldm;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;
import org.cougaar.util.StringUtility;

/**
 * Package-private script parser.
 * <p>
 * Expects lines of text:<ul>
 *   <li>Empty lines are ignored</li>
 *   <li>Lines starting with "#" are ignored</li>
 *   <li>Lines starting with "label " are goto labels, and require 
 *       a name (e.g. "label X").</li>
 *   <li>Lines starting with "goto " are goto commands, and require 
 *       a name (e.g. "goto X").  They may branch forward or 
 *       backwards in the script, but the label name must be
 *       contained in the script.
 *       </li>
 *   <li>Lines starting with "move " are mobility step commands,
 *       and are detailed below.</li>
 * </ul>
 * <p>
 * Note that, at this time, the scripting language is rather 
 * limited.  Future enhancements will include variables, 
 * conditionals, loops, math, etc.
 * <p>
 * Step commands are single with 7 comma-separated parameters:<ol>
 *   <li><i>actor</i><br>
 *   The agent that should run the step.  If an empty value is 
 *   specified then the script runner's agent is assumed.
 *   </li>
 *   <li><i>[+@]pauseTime</i> with ":" time dividers<br>
 *   The pause time in seconds before starting the step.
 *   If <i>+</i> is specified then the time is relative to the 
 *   runtime completion time of the prior step.  If <i>@</i> is 
 *   specified then the time is relative to the start time of
 *   the script process.  If no ":"s are present in the time,
 *   then milliseconds is assumed.  If one ":" is present, then
 *   the time is the addition of the value before the ":" as 
 *   seconds, and the value after the ":" as milliseconds, 
 *   i.e.:<pre>
 *      (v[1] + 1000 * (v[0]))
 *   </pre>
 *   If two ":"s are present, then the formula yields
 *   minutes:seconds:millis<pre>
 *      (v[3] + 1000 * (v[1] * 60 * (v[0])))
 *   </pre>
 *   <p>
 *   If no value or a negative value is specified, then there 
 *    will be zero pause.
 *   </li>
 *   <li><i>[+@]timeoutTime</i> with ":" time dividers<br>
 *   The timeout time in milliseconds for the step, following the 
 *   optional pause time.  See the pauseTime <i>+@</i> notes.
 *   </li>
 *   <li><i>mobileAgent</i><br>
 *   Agent that should be moved, or the script runner's agent
 *   if an empty value is specified.
 *   </li>
 *   <li><i>origNode</i><br>
 *   Node that the mobile agent should be on when the step
 *   is started, or an empty value if any node is allowed.
 *   </li>
 *   <li><i>destNode</i><br>
 *   Node that the mobile agent should be on when the step
 *   has completed, or an empty value if the agent should
 *   move to its current node.  See the "forceRestart" 
 *   option below.
 *   </li>
 *   <li><i>forceRestart</i><br>
 *   Specify "true" if the mobile agent should be fully
 *   restarted, even if the destination node is the same
 *   as that agent's starting node.
 *   </li>
 * </ol>
 * <p>
 * Example script:<pre>
 *
 *   # my comment
 *   # 20 seconds after launch, move agent A from node X to node Y
 *   move , @20:00, , A, X, Y,
 *
 *   # 60 seconds after launch, move A to Z, timeout after 30 seconds
 *   # note that if a time ends in ":", then ":00" is assumed
 *   move , @60:, , A, , Z, +30:
 *
 *   # have agent B move A to X after the above step finishes
 *   # must complete by 1 minute 5 seconds 10 milliseconds after 
 *   # script launch
 *   move B, , @1:5:10, A, , X,
 *
 *   # begin infinite loop
 *   label foo
 *   
 *     # after 20 seconds, move B from X to Y, wait at most 60 seconds
 *     move , +20:, +60:, B, X, Y,
 *
 *     # move B back with no pause and same timeout
 *     move , , +60:, B, Y, X,
 *
 *     # keep moving B back and forth forever
 *   goto foo
 *
 *   # never reach here!
 *
 * </pre>
 */
class ScriptParser {

  public static List parse(String text) {

    // not pretty!

    StringReader sr;
    BufferedReader br;
    try {
      sr = new StringReader(text);
      br = new BufferedReader(sr);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to read text", e);
    }

    List ret = new ArrayList();
    int maxGotoIdx = -1;
    int maxGotoLine = -1;
    boolean hasNamedGotos = false;
    Map labels = new HashMap(5);
    for (int i = 0; ; i++) {
      String origLine = null;
      try {
        origLine = br.readLine();
        String line = origLine;
        if (line == null) {
          break;
        }
        line = line.trim();
        if ((line.length() == 0) ||
            (line.charAt(0) == '#')) {
          continue;
        }
        if (line.startsWith("label ")) {
          String label = line.substring(6).trim();
          if (label.length() == 0) {
            throw new RuntimeException(
                "Empty label name");
          }
          Object o = labels.put(label, new Integer(ret.size()));
          if (o != null) {
            throw new RuntimeException(
                "Label "+label+" already defined");
          }
          ScriptLabel sl = new ScriptLabel(label);
          ret.add(sl);
          continue;
        }
        if (line.startsWith("goto ")) {
          String name = line.substring(5).trim();
          if (name.length() == 0) {
            throw new RuntimeException(
                "Empty goto name");
          }
          Object o = labels.get(name);
          if (o != null) {
            int idx = ((Integer) o).intValue();
            ret.add(new ScriptGoto(name, idx));
          } else {
            hasNamedGotos = true;
            // use a temporary entry, fix after main parse loop
            ret.add(new NamedGoto(name, ret.size()));
          }
          continue;
        }
        if (line.startsWith("move ")) {
          line = line.substring(5);
          line = line.trim();
          List l = StringUtility.parseCSV(line);
          ScriptStep ss = parseStep(l);
          ret.add(ss);
          continue;
        }
        throw new RuntimeException(
            "Unknown command, not (label, goto, move)");
      } catch (Exception e) {
        throw new RuntimeException(
            "Invalid line["+i+"] "+origLine, e);
      }
    }
    if (hasNamedGotos) {
      // replace all NamedGotos with resolved ScriptGotos
      for (int j = 0, nj = ret.size(); j < nj; j++)  {
        Object oj = ret.get(j);
        if (oj instanceof NamedGoto) {
          NamedGoto ng = (NamedGoto) oj;
          String name = ng.getName();
          Object lb = labels.get(name);
          if (lb == null) {
            throw new RuntimeException(
                "Unknown goto "+name+" at line "+
                ng.getIdx());
          } else {
            int idx = ((Integer) lb).intValue();
            ret.set(j, new ScriptGoto(name, idx));
          }
        }
      }
    }
    return ret;
  }
  
  private static ScriptStep parseStep(List l) {
    // expecting:
    //   {A, P, T, M, O, N, F}
    // where:
    //   A := (actorAgent)
    //   P := ([@+]pauseTime)
    //   T := ([@+]timeoutTime)
    //   M := (mobileAgent)
    //   O := (origNode)
    //   D := (destNode)
    //   F := (forceRestart)
    if (l.size() != 7) {
      throw new RuntimeException(
          "Expecting a step with 7 (not "+l.size()+")"+
          " comma-separated elements");
    }

    int flags = 0;
    String s;

    // refactor me!

    MessageAddress actorAgent;
    s = (String) l.get(0);
    if ((s != null) && (s.length() > 0)) {
      // FIXME RelayLP
      actorAgent = ClusterIdentifier.getClusterIdentifier(s);
    } else {
      actorAgent = null;
    }

    long pauseTime;
    s = (String) l.get(1);
    if ((s != null) && (s.length() > 0)) {
      char ch = s.charAt(0);
      if (ch == '@') {
        flags |= ScriptStep.REL_PAUSE;
        s = s.substring(1);
      } else if (ch == '+') {
        flags |= ScriptStep.ADD_PAUSE;
        s = s.substring(1);
      }
      pauseTime = parseTime(s);
    } else {
      pauseTime = -1;
    }

    long timeoutTime;
    s = (String) l.get(2);
    if ((s != null) && (s.length() > 0)) {
      char ch = s.charAt(0);
      if (ch == '@') {
        flags |= ScriptStep.REL_TIMEOUT;
        s = s.substring(1);
      } else if (ch == '+') {
        flags |= ScriptStep.ADD_TIMEOUT;
        s = s.substring(1);
      }
      timeoutTime = parseTime(s);
    } else {
      timeoutTime = -1;
    }

    MessageAddress mobileAgent;
    s = (String) l.get(3);
    if ((s != null) && (s.length() > 0)) {
      // FIXME RelayLP
      mobileAgent = ClusterIdentifier.getClusterIdentifier(s);
    } else {
      mobileAgent = null;
    }

    MessageAddress origNode;
    s = (String) l.get(4);
    if ((s != null) && (s.length() > 0)) {
      origNode = new MessageAddress(s);
    } else {
      origNode = null;
    }

    MessageAddress destNode;
    s = (String) l.get(5);
    if ((s != null) && (s.length() > 0)) {
      destNode = new MessageAddress(s);
    } else {
      destNode = null;
    }

    boolean forceRestart;
    s = (String) l.get(6);
    forceRestart = "true".equals(s);

    Ticket ticket = new Ticket(
        null, mobileAgent, origNode, destNode, forceRestart);

    StepOptions opts = new StepOptions(
        null, null, actorAgent, ticket, pauseTime, timeoutTime);

    ScriptStep ret = new ScriptStep(flags, opts);

    return ret;
  }

  private static long parseTime(String s) {
    int i = s.lastIndexOf(':');
    if (i < 0) {
      // exact millis
      return Long.parseLong(s);
    }
    long ret = 0;
    if (i > 0) {
      // minutes
      long t = Long.parseLong(s.substring(0, i));
      t *= 60*1000;
      ret += t;
    }
    s = s.substring(i+1);
    if (s.length() == 0) {
      // minutes only
      return ret;
    }
    i = s.indexOf('.');
    if (i >= 0) {
      // millis
      if ((i+1) < s.length()) {
        ret += Long.parseLong(s.substring(i+1));
      }
      s = s.substring(0, i);
    }
    if (s.length() > 0) {
      // seconds
      long t = Long.parseLong(s);
      t *= 1000;
      ret += t;
    }
    return ret;
  }

  private static class NamedGoto {
    private final String name;
    private final int origIdx;
    public NamedGoto(String name, int origIdx) {
      this.name = name;
      this.origIdx = origIdx;
    }
    public String getName() { 
      return name;
    }
    public int getIdx() { 
      return origIdx;
    }
  }
}
