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
 * Signals that an assertion has failed.
 *
 * @author unascribed
 * @version $Revision: 1.1 $, $Date: 2000-12-15 20:16:46 $
 * @see Assert
 */
public class AssertionException extends RuntimeException {

    /**
     * Constructs an <code>AssertionException</code> with the
     * specified detail message.
     *
     * @param s the detail message
     */
    public AssertionException (String s) {
	super(s);
    }
}
