/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
 */

public interface ParameterizedPlugIn {

  /** Lets the Component (or other plugin loader) tell the plugin about
   * any parameters to be applied.
   * @param arguments the plugin Parameters in the form of a Vector 
   * of Strings. 
   **/
  void setParameters(Vector arguments);
}
