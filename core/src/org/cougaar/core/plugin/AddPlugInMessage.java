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

import org.cougaar.util.StringUtility;
import java.util.Vector;
import org.cougaar.core.cluster.ClusterMessage;


/** 
 * Requests that the cluster add a plugin
 **/
public class AddPlugInMessage extends ClusterMessage
{
  private String theplugin;
  private Vector arguments;
        
  /** no-arg Constructor */
  public AddPlugInMessage() {
    super();
  }
        
  /** Constructor that takes a String name of a PlugIn **/
  public AddPlugInMessage(String plugin) {
    parsePlugIn(plugin);
  }
        
  /** @return The class name of the PlugIn to be added */
  public String getPlugIn() {
    return theplugin;
  }
        
  public Vector getArguments() {
    return arguments;
  }

  /** @param aplugin The class name of the PlugIn to be added **/
  public void setPlugIn(String aplugin) {
    parsePlugIn(aplugin);
  }
        
  public String toString() {
    return super.toString() + " " + theplugin;
  }

  // argument utilities
  
  private void parsePlugIn(String s) {
    int p1 = s.indexOf('(');
    int p2 = s.indexOf(')');
    if (p1 >= 0 && p2>=p1) {
      // has arguments
      theplugin = s.substring(0,p1);
      arguments = StringUtility.parseCSV(s, p1+1, p2);
    } else {
      // no arguments
      theplugin = s.intern();
      arguments = new Vector();
    }
  }

}
