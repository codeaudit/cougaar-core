/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Rules {
  private List rules = new ArrayList();

  static class Rule {
    boolean deny = false;
    Class cls;

    /**
     * Create a rule from a rule specificaton of the form:
     * [{allow,deny} ]<classname>
     * If allow/deny is missing, allow is assumed.
     **/
    public Rule(String ruleSpec) throws ClassNotFoundException {
      int pos = ruleSpec.indexOf(' ');
      String spec = ruleSpec;
      if (pos >= 0) {
        String ad = ruleSpec.substring(0, pos);
        spec = ruleSpec.substring(pos + 1);
        if (ad.equalsIgnoreCase("allow")) {
          deny = false;
        } else if (ad.equalsIgnoreCase("deny")) {
          deny = true;
        } else {
          throw new IllegalArgumentException("Bad ruleSpec");
        }
      }
      cls = Class.forName(spec);
    }

    public Rule(Class c, boolean deny) {
      this.cls = c;
      this.deny = deny;
    }

    public boolean matches(Class c) {
      return cls.isAssignableFrom(c);
    }

    public boolean isAllowRule() {
      return !deny;
    }

    public String toString() {
      return (deny ? "deny " : "allow ") + cls.getName();
    }
  }
  
  public void addDenyRule(Class c) {
      rules.add(new Rule(c, true));
  }

  public void addAllowRule(Class c) {
    rules.add(new Rule(c, false));
  }

  public void addRule(String ruleSpec) throws ClassNotFoundException {
    rules.add(new Rule(ruleSpec));
  }

  public boolean allow(Object o) {
    return allow(o.getClass());
  }

  public boolean allow(Class c) {
    for (int i = 0, n = rules.size(); i < n; i++) {
      Rule rule = (Rule) rules.get(i);
      if (rule.matches(c)) return rule.isAllowRule();
    }
    return true;
  }

  Iterator iterator() {
    return rules.iterator();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0, n = rules.size(); i < n; i++) {
      if (i > 0) buf.append(",");
      buf.append(rules.get(i));
    }
    return buf.toString();
  }

  public static void main(String[] args) {
    Rules rules = new Rules();
    boolean testMode = false;
    try {
      for (int i = 0; i < args.length; i++) {
        if (testMode) {
          Class cls = Class.forName(args[i]);
          System.out.println("rules.allow(" + cls.getName() + ")=" + rules.allow(cls));
        } else if ("-test".equals(args[i])) {
          testMode = true;
          System.out.println("rules=" + rules);
        } else {
          rules.addRule(args[i]);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
