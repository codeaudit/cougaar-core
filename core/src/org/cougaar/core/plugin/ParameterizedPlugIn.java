/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.util.Vector;

/** PlugIn classes which implement ParameterizedPlugIn
 * may be passed a Vector of Strings to parameterize the instance
 * between class instantiation and PlugIn.initialize().
 * While it is up to each implementation to decide what to do with
 * the parameters, the org.cougaar.core.plugin.* plugin classes all provide
 * a protected getParameters() method which may be used by subclasses
 * to retrieve the strings.
 * @deprecated We use introspection and the Component Model to find and 
 * call setParameter(Object) now instead of setParameters(Vector).
 */

public interface ParameterizedPlugIn {
}
