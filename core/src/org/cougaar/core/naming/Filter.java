/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.naming;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

public interface Filter {
    boolean match(Attributes a) throws NamingException;
    void toString(StringBuffer b);
}
