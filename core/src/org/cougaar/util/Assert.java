/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.util;

/**
 * Assert provides an assertion facility in Java comparable to the
 * assert macros in C/C++.  This class was taken from a FAQ on dejanews,
 * and is of unknown origin.  Here is the original entry in the FAQ:
 * <p>
 * <pre>
 * 11.6 How can I write C/C++ style assertions in Java?
 *
 * A.  The two classes shown below provide an assertion facility in Java.
 *     Set Assert.enabled to true to enable the assertions, and to false to
 *     disable assertions in production code. The AssertionException is not
 *     meant to be caught--instead, let it print a trace.
 *
 *     With a good optimizing compiler there will be no run time overhead
 *     for many uses of these assertions when Assert.enabled is set to false.
 *     However, if the condition in the assertion may have side effects, the
 *     condition code cannot be optimized away. For example, in the assertion
 * 	<code>Assert.assert(size() <= maxSize, "Maximum size exceeded");</code>
 *     the call to size() cannot be optimized away unless the compiler can
 *     see that the call has no side effects. C and C++ use the preprocessor
 *     to guarantee that assertions will never cause overhead in production
 *     code. Without a preprocessor, it seems the best we can do in Java is
 *     to write
 *	<code>Assert.assert(Assert.enabled && size() <= maxSize, "Too big");</code>
 *     In this case, when Assert.enabled is false, the method call can always
 *     be optimized away, even if it has side effects.
 * </pre>
 * </p>
 *
 * @author unascribed
 * @author Maintained by: Tom Mitchell (tmitchell@bbn.com)
 * @version $Revision: 1.1 $, $Date: 2000-12-15 20:16:46 $
 */
public final class Assert {

    /**
     * Don't allow construction, all methods are static.
     */
    private Assert () {}

    /**
     * Globally enable or disable assertions.
     */
    public static final boolean enabled = true;


    /**
     * Assert a condition to be true.  If it is not true,
     * an exception is thrown.
     *
     * @param b An expression expected to be true
     * @param s Exception string if expression is false
     * @exception AssertionException if expression is false
     */
    public static final void assert(boolean b, String s) {
	if (enabled && !b)
	    throw new AssertionException(s);
    }
}
