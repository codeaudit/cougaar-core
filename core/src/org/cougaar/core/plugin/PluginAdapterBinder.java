/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.core.plugin.ScheduleablePlugIn;
import org.cougaar.core.cluster.UIDServer;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.StateModelException;
import org.cougaar.util.GenericStateModelAdapter;

/** The standard Binder for Plugins.
 **/
public class PluginAdapterBinder extends PluginBinder
{

  public PluginAdapterBinder(Object parentInterface, Component child) {
    super( parentInterface, child );
  }


  /** package-private kickstart method for use by the PluginBinderFactory **/
  void initialize() {
    initializeChild(); 

    Component child = getChildComponent();
    if (child instanceof PlugInServesCluster) { // old-style compatability until we do more porting

      PlugInServesCluster plugin = (PlugInServesCluster) child;
      PluginManager parent = (PluginManager) getParentComponent();

      theLDM = parent.getLDM();
      theLDMF = theLDM.getFactory();
      
      plugin.load(null); // hack - shouldn't have any argument at all any more

      // this should get replaced with an LDM Service
      if (plugin instanceof PrototypeProvider) {
        parent.addPrototypeProvider((PrototypeProvider)plugin);
      }
      if (plugin instanceof PropertyProvider) {
        parent.addPropertyProvider((PropertyProvider)plugin);
      }
      if (plugin instanceof LatePropertyProvider) {
        parent.addLatePropertyProvider((LatePropertyProvider)plugin);
      }
      setThreadingModel(createThreadingModel());
      startThreadingModel();
    }
  }

  // 
  // LDM access
  //
  protected LDMServesPlugIn theLDM = null;

  public final LDMServesPlugIn getLDM() {
    return theLDM;
  }

  protected RootFactory theLDMF = null;
  public final RootFactory getFactory() {
    return theLDMF;
  }

  public final Factory getFactory(String s) {
    return theLDM.getFactory(s);
  }
  

  protected final PlugInAdapter getPlugInAdapter() {
    return (PlugInAdapter)getPlugin();
  }

  //
  // cluster
  // 

  public final ClusterIdentifier getClusterIdentifier() {
    //return getPluginManager().getClusterIdentifier();
    return getAgentIdentifier();
  }


  // override PluginBinder
  protected Threading createThreadingModel() {
    Threading t;
    switch (getThreadingChoice()) {
    case NO_THREAD:
      t = new NoThreading();
      break;
    case SHARED_THREAD: 
      t = new PABSharedThreading();
      break;
    case SINGLE_THREAD:
      t = new PABSingleThreading();
      break;
    case ONESHOT_THREAD:
      t = new OneShotThreading();
      break;
    default:
      throw new RuntimeException("Invalid Threading model "+getThreadingChoice());
    }
    return t;
  }


  /** shares a Thread with other SharedThreading plugins in the same cluster **/
  protected class PABSharedThreading extends SharedThreading {
    public void start() {
      getPlugInAdapter().getSharedThreadingService().registerPlugIn(this);
      plugin_prerun();
    }

    //
    // implementation of ScheduleablePlugIn API 
    //

    public void addExternalActivityWatcher(SubscriptionWatcher watcher) { 
     (getPlugInAdapter().getBlackboardService()).registerInterest(watcher);
     //System.out.println("PluginAdapterBinder.addExternalActivityWatcher()");
    }
  }

  /** has its own Thread **/
  protected class PABSingleThreading extends SingleThreading {
    public void load(Object object) {
      setWaker(getPlugInAdapter().getBlackboardService().registerInterest());
    }

  }

  /** Called by all the standard Threading models to instruct the plugin 
   * to do plugin-specific initializations.  E.g. setup its subscriptions, etc.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  protected void plugin_prerun() {
    getPlugInAdapter().start(); // just in case..
    getPlugInAdapter().plugin_prerun();
  }

  /** Called by all the standard Threading models (except for OneShotThreading)
   * each time there is work to be done.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  protected void plugin_cycle() {
    //System.out.println("PluginAdapaterBinder:plugin_cycle()");
    getPlugInAdapter().plugin_cycle();
  }

}
