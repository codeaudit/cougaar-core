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

package org.cougaar.planning.plugin.legacy;


public class StatelessPluginAdapter 
  extends SimplePlugin
{
  private Plugin thePlugin;

  private PluginContext theContext;

  public StatelessPluginAdapter(Plugin plugin) {
    thePlugin = plugin;
    theContext = new PluginContextAdapter();
  }

  protected void setupSubscriptions() {
    thePlugin.initialize(theContext);
  }
  protected void execute() {
    thePlugin.execute(theContext);
  }

  protected PluginDelegate createDelegate() {
    return new PluginContextAdapter();
  }

  private class PluginContextAdapter 
    extends Delegate 
    implements PluginContext
  {
    private Plugin.State theState = null;
    public void setPluginState(Plugin.State state) {
      if (theState != null) { 
        throw new RuntimeException("Plugins may not set the state multiple times.");
      }
      theState = state;
    }
    public Plugin.State getPluginState() {
      return theState;
    }
  }

}
