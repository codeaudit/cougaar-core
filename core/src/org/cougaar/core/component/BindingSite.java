/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A BindingSite class names and specifies a particular service API.
 * A given Service may have several binding sites.  For example, a different
 * BindingSite for each type of client component.
 * <P>
 * There are no methods common to all BindingSites and there is no 
 * (current) requirement for BindingSites to actually extend this class.
 **/
public interface BindingSite
{
}
