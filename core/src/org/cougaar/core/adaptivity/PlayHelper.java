/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

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
package org.cougaar.core.adaptivity;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.BlackboardService;

/**
 * Helper class for computing OperatingModes from plays and
 * Conditions
 **/
public class PlayHelper { 

  private static class OMMapEntry {
    public OMCRangeList newValue;
    public List plays = new ArrayList();
    public OMMapEntry(OMCRangeList nv, Play firstPlay) {
      newValue = nv;
      plays.add(firstPlay);
    }
  }

  private Map omMap = new HashMap();
  private LoggingService logger;
  private OperatingModeService operatingModeService;
  private ConditionService conditionService;
  private BlackboardService blackboard;
  private Map smMap;

  public PlayHelper(LoggingService ls,
                    OperatingModeService oms,
                    ConditionService sms,
                    BlackboardService bb,
                    Map smm)
  {
    logger = ls;
    operatingModeService = oms;
    conditionService = sms;
    blackboard = bb;
    smMap = smm;
  }

  /**
   * Update all operating modes based on conditions and the playbook.
   * This is the real workhorse of this plugin and carries out
   * playbook-based adaptivity. All the active plays from the playbook
   * are considered. If the ifClause evaluates to true, then the
   * operating mode values are saved in a Map under the operating mode
   * name. When multiple plays affect the same operating mode, the
   * values are combined by intersecting the allowed value ranges. If
   * a play specifies a constraint that would have the effect of
   * eliminating all possible values for an operating mode, that
   * constraint is logged and ignored. Finally, the operating modes
   * are set to the effective value of the combined constraints.
   **/
  public void updateOperatingModes(Play[] plays) {
    if (logger.isDebugEnabled()) logger.debug("updateOperatingModes");

    /* run the plays - that is, do the comparisons in the "If" parts
     * of the plays and if they evaluate to true, set the operating modes in
     * the "Then" parts of the plays and publish the new values to the
     * blackboard.
     */

    for (int i = 0; i < plays.length; i++) {
      Play play = plays[i];
      try {
        if (eval(play.getIfClause().iterator())) {
          ConstraintPhrase[] playConstraints = play.getOperatingModeConstraints();
          for (int j = 0; j < playConstraints.length; j++) {
            ConstraintPhrase cp = playConstraints[j];
            String operatingModeName = cp.getProxyName();
            OMCRangeList av =
              cp.getAllowedValues().applyOperator(cp.getOperator());
            OMMapEntry omme = (OMMapEntry) omMap.get(operatingModeName);
            if (omme == null) {
              omMap.put(operatingModeName, new OMMapEntry(av, play));
            } else {
              OMCRangeList intersection = omme.newValue.intersect(av);
              if (intersection.isEmpty()) {
                logger.error("Play conflict for play " + play + " against " + omme.plays);
              } else {
                omme.newValue = intersection;
              }
            }
          }
        }
      } catch (Exception iae) {
        logger.error("Error in play: " + play, iae);
      }
    }
    Set operatingModes = new HashSet(operatingModeService.getAllOperatingModeNames());
    logger.debug("Updating operating modes");
    for (Iterator i = omMap.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      String operatingModeName = (String) entry.getKey();
      OMMapEntry omme = (OMMapEntry) entry.getValue();
      Comparable value = omme.newValue.getEffectiveValue();
      OperatingMode om = operatingModeService.getOperatingModeByName(operatingModeName);
      if (logger.isInfoEnabled()) logger.info("Setting OperatingMode " + operatingModeName + " to " + value);
      if (om == null) {
        if (logger.isDebugEnabled()) logger.debug("OperatingMode not present: " + operatingModeName);
      } else {
        om.setValue(value);
        blackboard.publishChange(om);
      }
      operatingModes.remove(operatingModeName); // This one has been accounted for
    }
    if (!operatingModes.isEmpty()) {
      for (Iterator i = operatingModes.iterator(); i.hasNext(); ) {
        logger.error("No play found to set operating mode: " + i.next());
      }
    }
    omMap.clear();
  }

  /**
   * Evaluate an if clause.
   **/
  private boolean eval(Iterator x) {
    if (!x.hasNext()) throw new IllegalArgumentException("Incomplete play");
    Object o = x.next();
    if (o.equals(BooleanOperator.NOT)) {
      return !eval(x);
    }
    if (o.equals(BooleanOperator.AND)) {
      return eval(x) & eval(x);
    }
    if (o.equals(BooleanOperator.OR)) {
      return eval(x) | eval(x);
    }
    if (o.equals(BooleanOperator.TRUE)) {
      return true;
    }
    if (o.equals(BooleanOperator.FALSE)) {
      return false;
    }
    if (o instanceof ConstraintOpValue) {
      ConstraintOpValue phrase = (ConstraintOpValue) o;
      Comparable paramValue = phrase.getValue();
      OMCRangeList paramRanges = phrase.getAllowedValues();
      ConstraintOperator op = phrase.getOperator();
      Comparable conditionValue = evalArithmetic(x);
      if (logger.isDebugEnabled()) logger.debug("conditionValue = " + conditionValue);
      if (op.equals(ConstraintOperator.IN) || op.equals(ConstraintOperator.NOTIN)) {
        boolean isIn = paramRanges.isAllowed(conditionValue);
        if (op.equals(ConstraintOperator.IN)) return isIn;
        return !isIn;
      }
      int diff = conditionValue.compareTo(paramValue);
      if (op.equals(ConstraintOperator.GREATERTHAN       )) return diff >  0;
      if (op.equals(ConstraintOperator.GREATERTHANOREQUAL)) return diff >= 0;
      if (op.equals(ConstraintOperator.LESSTHAN          )) return diff <  0;
      if (op.equals(ConstraintOperator.LESSTHANOREQUAL   )) return diff <= 0;
      if (op.equals(ConstraintOperator.EQUAL             )) return diff == 0;
      if (op.equals(ConstraintOperator.NOTEQUAL          )) return diff != 0;
      throw new IllegalArgumentException("invalid ConstraintOperator " + op);
    }
    throw new IllegalArgumentException("invalid if clause " + o);
  }

  private Comparable evalArithmetic(Iterator x) {
    if (!x.hasNext()) throw new IllegalArgumentException("Incomplete play");
    Object o = x.next();
    if (o instanceof ArithmeticOperator) {
      ArithmeticOperator op = (ArithmeticOperator) o;
      if (op.equals(ArithmeticOperator.ADD)) {
        Comparable r = evalArithmetic(x);
        Comparable l = evalArithmetic(x);
        return ComparableHelper.add(l, r);
      }
      if (op.equals(ArithmeticOperator.SUBTRACT)) {
        Comparable r = evalArithmetic(x);
        Comparable l = evalArithmetic(x);
        return ComparableHelper.subtract(l, r);
      }
      if (op.equals(ArithmeticOperator.MULTIPLY)) {
        Comparable r = evalArithmetic(x);
        Comparable l = evalArithmetic(x);
        return ComparableHelper.multiply(l, r);
      }
      if (op.equals(ArithmeticOperator.DIVIDE)) {
        Comparable r = evalArithmetic(x);
        Comparable l = evalArithmetic(x);
        return ComparableHelper.divide(l, r);
      }
      if (op.equals(ArithmeticOperator.NEGATE)) {
        Comparable r = evalArithmetic(x);
        return ComparableHelper.negate(r);
      }
      throw new IllegalArgumentException("Unknown ArithmeticOperator: " + op);
    }
    if (o instanceof String) {
      String conditionName = (String) o;
      Condition condition = (Condition) smMap.get(conditionName);
      return condition.getValue();
    }
    throw new IllegalArgumentException("invalid if clause " + o);
  }
}
