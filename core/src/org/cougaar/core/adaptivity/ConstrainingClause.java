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
 * Holds a parsed expression for testing the values of Conditions. The
 * expression is inserted in reverse Polish or postfix notation, but
 * read out in the opposite (prefix) order.
 **/

public class ConstrainingClause {

  private List list = new ArrayList();
  /** 
   * Append an operator or operand onto the list. It is assumed that
   * the caller is constructing a well-formed expression.
   * @param o a String, Operator, ConstraintOpValue, or
   * ConstrainingClause. If another ConstrainingClause is pushed, its
   * entire contents is appended, otherwise the item itself is
   * appended.
   **/
  public void push(Object o) {
    if (o instanceof ConstrainingClause) {
      list.addAll(((ConstrainingClause) o).list);
    } else {
      list.add(o);
    }
  }
  
  /**
   * Gets an iterator over the contents in prefix order.
   * @return an iterator that can walk the clause for evaluation. The
   * iterator runs through the contents in the reverse order from
   * which the information was appended. This has the effect of
   * turning the reverse Polish (postfix) entry order into forward
   * Polish (prefix) order.
   **/
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

  /**
   * Gets the expression in infix format.
   * @return a string representation of the expression using infix
   * notation. Parentheses are inserted liberally to make operator
   * precedence clear.
   **/
  public String toString() {
    return toString(iterator());
  }
}




