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
  public void pushPhrase(ConstraintPhrase cp) {
    list.add(cp);
  }
  
  /**
   * Add operator
   * @param BooleanOperator
   */
  public void pushOperator(BooleanOperator operator) {
    list.add(operator);
  }

  /**
   * Push an entire ConstrainingClause into this clause
   **/
  public void pushClause(ConstrainingClause cc) {
    list.addAll(cc.list);
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

  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Iterator i = iterator(); i.hasNext(); ) {
      buf.append(' ').append(i.next());
    }
    return buf.substring(1);
  }
}




