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

import org.cougaar.core.service.*;

import javax.naming.directory.Attributes;
import javax.naming.NamingException;

public interface Filter {
    boolean match(Attributes a) throws NamingException;
    void toString(StringBuffer b);
}
