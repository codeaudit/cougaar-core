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

import org.cougaar.core.cluster.ClusterMessage;

/** 
 * RemovePlugInMessage
 **/

public class RemovePlugInMessage extends ClusterMessage
{
  private String genplugin;
	
  /** no-arg Constructor **/
  public RemovePlugInMessage() {
    super();
  }
	
  /** Constructor that takes a String name of a PlugIn **/
  public RemovePlugInMessage(String plugin) {
    genplugin = plugin;
  }
	
  /** @return String The String name of the PlugIn to be removed from the Component */
  public String getPlugIn() {
    return genplugin;
  }
	
  /** @param aplugin The String name of the PlugIn to be removed from the Component */
  public void setPlugIn(String aplugin) {
    genplugin = aplugin;
  }
	
  public String toString() {
    return super.toString() + " " + genplugin;
  }
}
