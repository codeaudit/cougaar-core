/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModel; // inlined
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component registers with persistence as the component
 * model tracker and either rehydrates or reads the agents'
 * component descriptions.
 *
 * @see FindComponentsLoadService 
 */
public final class FindComponentsEarly
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private MessageAddress localAgent;
  private MessageAddress localNode;
  private boolean isNode;

  private LoggingService log;

  private MySP fclsp;

  private PersistenceService ps;
  private PersistenceClient pc;

  private boolean foundDescs;
  private List initialDescs;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    localAgent = find_local_agent();
    localNode = find_local_node();
    isNode = 
      (localAgent == null ||
       localAgent.equals(localNode));

    get_logger();

    register_persistence();

    // rehydrate from mobile state (if available)
    load_early();

    fclsp = new MySP();
    sb.addService(FindComponentsLoadService.class, fclsp);

    // called later via FindComponentsLoadService:
    //load_late();
  }

  public void start() {
    super.start();

    initialDescs = null;

    // once loaded we revoke our service
    if (fclsp != null) {
      sb.revokeService(
          FindComponentsLoadService.class,
          fclsp);
      fclsp = null;
    }
  }

  public void unload() {
    super.unload();

    unregister_persistence();
  }

  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }

  private void get_logger() {
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    String prefix = localAgent+": ";
    log = LoggingServiceWithPrefix.add(log, prefix);
  }

  private void load_early() {
    if (log.isDebugEnabled()) {
      log.debug(
          "Attempting early rehydrate, in case mobile persistence"+
          " data exists");
    }

    List l = null;

    // get list from persisted state
    Object o = rehydrate();
    if (o instanceof List) {
      l = (List) o;
    }
    o = null;

    if (l != null) {
      foundDescs = true;
      if (log.isInfoEnabled()) {
        log.info("Restoring mobile components descriptions");
      }
      overrideComponentList(l);
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "No persistence data found, loading component descriptions"+
          " from the initializer service");
    }
    l = readDescsFromConfig();
    overrideComponentList(l);
    // leave foundDescs as false, for use in "load_late()"
  }

  private void load_late() {
    // See comment in load_early() above.
    if (foundDescs) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Already rehydrated component descriptions from"+
            "  mobile state");
      } 
      return;
    }
    foundDescs = true;

    // We "load_late" to allow other components to load before
    // the second rehydration attempt, such as necessary security
    // components required to decrypt the snapshot.
    if (log.isDebugEnabled()) {
      log.debug(
          "Attempting second rehydrate");
    }

    List l = null;

    // try to rehydrate a second time!
    Object o = rehydrate(); 
    if (o instanceof List) {
      l = (List) o;
    }
    o = null;

    if (l == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "No components found in snapshot, keeping our"+
            " initial config");
      }
      return;
    }

    // rehydrating late, so we've already added our
    // "FindComponentsLate" component, which we must now
    // trim out of the list
    List l2 = 
      trimListUntilAfter(l, FindComponentsLate.class.getName());

    if (log.isInfoEnabled()) {
      log.info("Restoring components descriptions");
    }

    overrideComponentList(l2);
  }

  private Object captureState() {
    if (log.isInfoEnabled()) {
      log.info("Capturing component descriptions");
    }
    return getComponentList();
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName();
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          Object o = captureState();
          // must return mutable list!
          List l = new ArrayList(1);
          l.add(o);
          return l;
        }
      };
    ps = 
      (PersistenceService)
      sb.getService(
          pc, PersistenceService.class, null);
  }

  private void unregister_persistence() {
    if (ps != null) {
      sb.releaseService(
          pc, PersistenceService.class, ps);
      ps = null;
      pc = null;
    }
  }

  private Object rehydrate() {
    RehydrationData rd = ps.getRehydrationData();
    if (rd == null) {
      if (log.isInfoEnabled()) {
        log.info("No rehydration data found");
      }
      return null;
    }

    List l = rd.getObjects();
    rd = null;
    int lsize = (l == null ? 0 : l.size());
    if (lsize < 1) {
      if (log.isInfoEnabled()) {
        log.info("Invalid rehydration list? "+l);
      }
      return null;
    }
    Object o = l.get(0);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("Null rehydration state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Found rehydrated state");
      if (log.isDetailEnabled()) {
        log.detail("state is "+o);
      }
    }

    return o;
  }

  private List readDescsFromConfig() {
    List l = new ArrayList();

    // get ".ini" descriptions
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            localAgent.toString(),
            Agent.INSERTION_POINT);
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      if (log.isInfoEnabled()) {
        log.info("\nUnable to add "+localAgent+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    String configFormat =
      System.getProperty("org.cougaar.core.node.InitializationComponent");
    if (!"XML".equals(configFormat)) {
      // backwards compatibility for non-XML configs!
      //
      // convert to component descriptions, for priority-based sorting
      ComponentDescriptions descs = new ComponentDescriptions(l);

      // add hard-coded "template" components and convert into
      // sorted list
      l = DefaultComponents.getHardCodedComponents(descs, isNode);
    }

    // return list of ComponentDescriptions and StateTuples
    return l;
  }

  private void overrideComponentList(List l) {
    if (log.isDebugEnabled()) {
      log.debug("overrideComponentList("+l+")");
    }

    // save for state capture while loading
    initialDescs = l;

    AgentBootstrapService abs = (AgentBootstrapService)
      sb.getService(this, AgentBootstrapService.class, null);
    if (abs == null) {
      throw new RuntimeException(
          "Unable to obtain AgentBootstrapService"+
          ", can not override component list");
    }
    abs.overrideComponentList(l);
    sb.releaseService(this, AgentBootstrapService.class, abs);
    abs = null;
  }

  private List getComponentList() {
    if (initialDescs != null) {
      // actively loading, use override list
      if (log.isInfoEnabled()) {
        log.info(
            "Asked to \"getComponentList\" while loading,"+
            " which would find a partially loaded model,"+
            " so instead return our initial component list["+
            initialDescs.size()+"]");
        if (log.isDebugEnabled()) {
          log.debug(
              "initialDescs["+initialDescs.size()+
              "]="+initialDescs);
        }
      }
      return initialDescs;
    } 

    if (getModelState() == GenericStateModel.LOADED &&
        log.isErrorEnabled()) {
      // this shouldn't happen, since during load we always
      // save our override list
      log.error(
          "Attempting to capture model state while loading,"+
          " initialDescs is null,"+
          " this may find a partial configuration!");
    }

    // get descriptions
    AgentComponentModelService acms = (AgentComponentModelService)
      sb.getService(this, AgentComponentModelService.class, null);
    if (acms == null) {
      throw new RuntimeException(
          "Unable to obtain AgentComponentModelService"+
          ", can not override component list");
    }
    ComponentDescriptions descs = 
      acms.getComponentDescriptions();
    sb.releaseService(this, AgentComponentModelService.class, acms);
    acms = null;

    if (log.isDebugEnabled()) {
      log.debug("ComponentModel found: "+descs);
    }

    // convert to list
    List l = DefaultComponents.flattenComponents(descs);

    if (log.isDebugEnabled()) {
      log.debug("Flattened to list: "+l);
    }

    // trim to exclude our component and components loaded before us
    //
    // Note that this is the correct list for mobility, but if
    // we're capturing the list for persistence we should further
    // trim the list until "FindComponentsLate".  For now we'll
    // do this when we "load_late()" rehydrate.
    List l2 = 
      trimListUntilAfter(l, FindComponentsEarly.class.getName());

    if (log.isDebugEnabled()) {
      log.debug("Trimmed list to: "+l2);
    }

    return l2;
  }

  private List trimListUntilAfter(List l, String classname) {
    // find the index of the classname
    int i = 0;
    int n = l.size();
    while (true) {
      if (i >= n) {
        // not found?
        if (log.isDetailEnabled()) {
          log.detail("classname "+classname+" not found in "+l);
        }
        return l;
      }
      Object o = l.get(i++);
      ComponentDescription cd =
        ((o instanceof StateTuple) ?
         (((StateTuple) o).getComponentDescription()) :
         ((ComponentDescription) o));
      if (classname.equals(cd.getClassname())) {
        if (log.isDetailEnabled()) {
          log.detail(
              "found classname "+classname+" as "+cd+
              " at index "+(i-1)+" of "+n+" in list "+l);
        }
        break;
      }
    }
    // trim list to remainder
    //
    // can't l.subList, since it's not serializable!
    // just loop instead... 
    List ret = new ArrayList(n-(i+1)); 
    for (int j = i; j < n; j++) {
      ret.add(l.get(j));
    }
    if (log.isDetailEnabled()) {
      log.detail("trimmed list["+i+".."+n+"] to "+ret);
    }
    return ret;
  }

  private final class MySP
    implements ServiceProvider {
      private final FindComponentsLoadService fcls;
      public MySP() {
        fcls = new FindComponentsLoadService() {
          public void rehydrate() {
            load_late();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (FindComponentsLoadService.class.isAssignableFrom(
              serviceClass)) {
          return fcls;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }
}
