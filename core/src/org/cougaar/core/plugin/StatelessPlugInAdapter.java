/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.domain.*;

import org.cougaar.core.service.*;

import org.cougaar.core.agent.service.alarm.*;

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
    public void setPlugInState(PlugIn.State state) {
      if (theState != null) { 
        throw new RuntimeException("PlugIns may not set the state multiple times.");
      }
      theState = state;
    }
    public PlugIn.State getPlugInState() {
      return theState;
    }
  }

}
