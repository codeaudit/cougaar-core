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

import org.cougaar.util.StateModelException;

public class StatelessPlugInAdapter 
  extends SimplePlugIn
{
  private PlugIn thePlugIn;

  private PlugInContext theContext;

  public StatelessPlugInAdapter(PlugIn plugin) {
    thePlugIn = plugin;
    theContext = new PlugInContextAdapter();
  }

  protected void setupSubscriptions() {
    thePlugIn.initialize(theContext);
  }
  protected void execute() {
    thePlugIn.execute(theContext);
  }

  protected PlugInDelegate createDelegate() {
    return new PlugInContextAdapter();
  }

  private class PlugInContextAdapter 
    extends Delegate 
    implements PlugInContext
  {
    private PlugIn.State theState = null;
    public void setState(PlugIn.State state) {
      if (theState != null) { 
        throw new RuntimeException("PlugIns may not set the state multiple times.");
      }
      theState = state;
    }
    public PlugIn.State getState() {
      return theState;
    }
  }

}
