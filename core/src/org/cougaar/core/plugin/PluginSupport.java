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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.BindingSite;
import org.cougaar.util.GenericStateModelAdapter;

  /** 
   * First cut at a class that performs basic new-fangled Plugin functions
   **/

public abstract class PluginSupport 
  extends GenericStateModelAdapter
  implements PluginBase
{

  private PluginBindingSite pluginBindingSite = null;
  /**
   * Found by introspection
   **/
  public void setBindingSite(BindingSite bs) {
    if (bs instanceof PluginBindingSite) {
      pluginBindingSite = (PluginBindingSite)bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
  }

  protected final PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }

  /** storage for wasAwakened. 
   **/
  private boolean explicitlyAwakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only while running in the main plugin thread.
   */
  protected boolean wasAwakened() { return explicitlyAwakened; }

  /** For PluginBinder use only **/
  public final void setAwakened(boolean value) { explicitlyAwakened = value; }

}
