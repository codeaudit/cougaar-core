/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.util;

/**
 * Signals that an assertion has failed.
 *
 * @author unascribed
 * @version $Revision: 1.2 $, $Date: 2001-04-05 19:27:24 $
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
