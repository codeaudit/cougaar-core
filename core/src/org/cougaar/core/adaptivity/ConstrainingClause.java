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

import java.util.*;

/** 
 * A set of ConstraintPhrases connected with boolean operators  
 */

/* needs work! */
public class ConstrainingClause {

  private List list = new ArrayList();
  /** 
   * AND or OR ConstraintOperator previous c new ConstraintOperator(lause with the new clause) 
   * @param ConstraintPhrase 
   */
  public void push(Object o) {
    if (o instanceof ConstrainingClause) {
      list.addAll(((ConstrainingClause) o).list);
    } else {
      list.add(o);
    }
  }
  
  /** 
   * @return an iterator that can walk the phrase for interpretation  
   */
  public Iterator iterator() {
    return new Iterator() {
      private ListIterator iter = list.listIterator(list.size());
      public boolean hasNext() {
        return iter.hasPrevious();
      }
      public Object next() {
        return iter.previous();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Print this clause in infix notation with liberal parentheses
   **/
  private String toString(Iterator x) {
    if (!x.hasNext()) return "";
    String r = null;
    String l = null;
    StringBuffer buf = new StringBuffer();
    Object o = x.next();
    if (o instanceof ConstraintOpValue) {
      buf.append('(').append(toString(x)).append(' ').append(o).append(')');
    } else if (o instanceof Operator) {
      Operator op = (Operator) o;
      switch (op.getOperandCount()) {
      case 2:
        r = toString(x);
        l = toString(x);
        break;
      case 1:
        r = toString(x);
        break;
      default:
      }
      buf.append('(');
      if (l != null) buf.append(l).append(' ');
      buf.append(op);
      if (r != null) buf.append(' ').append(r);
      buf.append(')');
    } else {
      buf.append(o);
    }
    return buf.toString();
  }

  public String toString() {
    return toString(iterator());
  }
}




