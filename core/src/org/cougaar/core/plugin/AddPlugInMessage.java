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

import org.cougaar.util.StringUtility;
import java.util.Vector;
import org.cougaar.core.cluster.ClusterMessage;


/** 
 * Requests that the cluster add a plugin.
 *
 * Will likely change to include additional load/run parameters,
 * perhaps using an <code>org.cougaar.util.PropertyTree</code>.
 **/
public class AddPlugInMessage extends ClusterMessage
{
  private String theplugin;
  private Vector arguments;
        
  /** no-arg Constructor */
  public AddPlugInMessage() {
    super();
  }
        
  /** @return The class name of the PlugIn to be added */
  public String getPlugIn() {
    return theplugin;
  }
        
  public Vector getArguments() {
    return arguments;
  }

  /** 
   * @param piName The class name of the PlugIn to be added 
   */
  public void setPlugIn(String piName) {
    this.theplugin = piName.intern();
  }

  /** 
   * @param piArgs Vector of PlugIn parameters
   */
  public void setArguments(Vector piArgs) {
    this.arguments = piArgs;
  }

  /**
   * @deprecated
   */
  public void setPlugInAndArguments(String s) {
    int p1 = s.indexOf('(');
    int p2 = s.indexOf(')');
    String piName;
    Vector piArgs;
    if (p1 >= 0 && p2>=p1) {
      // has arguments
      piName = s.substring(0,p1);
      piArgs = StringUtility.parseCSV(s, p1+1, p2);
    } else {
      // no arguments
      piName = s;
      piArgs = new Vector();
    }
    setPlugIn(s);
    setArguments(piArgs);
  }

  public String toString() {
    return super.toString() + " " + theplugin;
  }
}
