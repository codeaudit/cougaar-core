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




