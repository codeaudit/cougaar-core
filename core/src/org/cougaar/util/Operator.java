/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * An extended <code>UnaryPredicate</code> with improved <tt>toString<tt>,
 * XML generation, and new "implies" capability.
 * <p>
 * <code>Operator</code>s feature improved:
 * <ul>
 *   <li><tt>toString</tt> in "paren" or "xml" formats</li>
 *   <li>added XML generation (<tt>getXML</tt>)</li>
 *   <li>(most important) comparison capabilities based upon the
 *       behavior of the <code>Operator</code> 
 *       (<tt>implies</tt>, <tt>allows</tt>, <tt>equals</tt>)</li>
 * </ul>
 * <p>
 * The comparison methods will be used for automated analysis, such as
 * the ability to generate <code>PlugIn</code> "contracts" of 
 * publish/subscribe behavior.
 * <p>
 * <b>Note:</b> The default implementation of <code>Operator</code> 
 * is currently kept in the utility module as "org.cougaar.lib.contract.lang" -- 
 * see the "index.html" file there for language details.
 * <p>
 * @see OperatorFactory
 */
public interface Operator 
    extends UnaryPredicate, XMLizable {

  /**
   * Constants for <code>Operator</code> <tt>parse</tt> and 
   * <tt>toString</tt> style.
   */
  public static final int PAREN_FLAG =   (1 << 0);
  public static final int XML_FLAG =     (1 << 1);
  public static final int PRETTY_FLAG =  (1 << 2);
  public static final int VERBOSE_FLAG = (1 << 3);

  /**
   * Default style for <tt>toString</tt> -- currently pretty-printed XML.
   */
  public static final int DEFAULT_STYLE = 
    (XML_FLAG | PRETTY_FLAG);

  /**
   * <tt>execute</tt> method from <code>UnaryPredicate</code>.
   */
  public boolean execute(final Object o);

  /**
   * <tt>operate</tt> method acts like <tt>execute</tt>, but returns
   * an <code>Object</code> -- <code>UnaryPredicate</code> users should
   * use <tt>execute</tt> instead!
   */
  public Object operate(final Object o);

  /**
   * Sets a constant value in the <code>Operator</code>, which is accessed
   * internally via <tt>(get "key")</tt>.
   * <p><pre>
   * For example, a paren-style predicate might be
   *   <tt>(and (isString) (equals (get "mystr")))</tt>
   * and parsed into <code>Operator</code> op.  One can then set
   * the value of variable "mystr" to be the <code>String</code> 
   * "foo" via
   *   <tt>op.setConst("mystr", "foo")</tt>
   * and the predicate will behave as
   *   <tt>(and (isString) (equals (get "foo")))</tt>
   * <p>
   * One can also specify the expected type of the constant to be
   * other than <code>String</code> within the predicate via
   *   <tt>(and (isInteger) (equals (get "myint" "Integer")))</tt>
   * and then set it via
   *   <tt>op.setConst("mystr", new Integer(123))</tt>
   * <p>
   * The goal is to allow predicates to be used as simple parameterized
   * templates.
   * <p>
   * @param key the "get" constant identifier
   * @param val the new value of the constant
   */
  public void setConst(final String key, final Object val);

  /**
   * <pre>
   * "(this.implies(oper))" is defined as:
   *   for all Objects <i>o1..on</i>,
   *     if <tt>(this.execute(<i>oi</i>) == true)</tt>
   *     then <tt>(oper.execute(<i>oi</i>) == true)</tt>.
   * </pre>
   * <p>
   * Note that there might never be an Object where <tt>this.execute(o)</tt>
   * is <tt>true</tt>, and <tt>oper</tt> might match other Objects.
   * <p>
   * One can use "implies" to see if the given <tt>oper</tt> <u>will</u>
   * "fire" if <tt>this</tt> Operator fires.
   *
   * @see #allows
   * @see #equals
   */
  public boolean implies(final Operator oper);

  /**
   * Equivalent to <tt>oper.implies(this)</tt>.
   */
  public boolean impliedBy(final Operator oper);

  /**
   * <pre>
   * "(this.allows(oper))" is defined as:
   *   there exists an Object <i>o</i> such that
   *     <tt>((this.execute(<i>o</i>) == true) &amp;&amp;
   *          (oper.execute(<i>o</i>) == true))</tt>.
   * </pre>
   * <p>
   * Note that this Object might never arise in practice, but
   * the two <code>Operator</code>s are at least logically compatable.
   * <p>
   * One often uses <tt>publishOp.allows(subscribeOp)</tt>, since we
   * are looking to see if any Object <u>might</u> pass both the publishOp
   * and the subscribeOp.  Although it's logically equivalent
   * to test <tt>subscribeOp.allows(publishOp)</tt>, the prior form
   * is likely faster and better supported.
   *
   * @see #implies
   * @see #equals
   */
  public boolean allows(final Operator oper);

  /**
   * <pre>
   * "(this.equals(oper))" is defined as:
   *   for all Objects <i>o1..on</i>,
   *     <tt>(this.execute(<i>oi</i>) == oper.execute(<i>oi</i>))</tt>.
   * </pre>
   * <p>
   * Note that real "program" equivalency is NP-complete (Halting problem),
   * so this is at best an approximation!  The same caveat goes for 
   * <tt>implies</tt> and <tt>allows</tt>.
   *
   * @see #implies
   * @see #allows
   */
  public boolean equals(final Operator oper);

  /**
   * Convert <code>this</code> to a <code>String</code>.
   *
   * @param style use a bit-mix of <tt>*_FLAG</tt> constants, such as
   *    (XML_FLAG | VERBOSE_FLAG) or
   *    (PAREN_FLAG | PRETTY_FLAG), etc
   */
  public String toString(final int style);

  /**
   * Get an XML <code>Element</code> representation in the given <tt>style</tt>.
   * <p>
   * @param style currently only VERBOSE_FLAG is used
   */
  public Element getXML(Document doc, final int style);

  /**
   * Get an XML <code>Element</code> representation in the 
   * <tt>DEFAULT_STYLE</tt>.
   * <p>
   * <tt>getXML</tt> method from <code>XMLizable</code>.
   */
  public Element getXML(Document doc);
}
