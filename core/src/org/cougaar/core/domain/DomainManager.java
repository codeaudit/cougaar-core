/*
 * <copyright>
 *  Copyright 2001,2002 BBNT Solutions, LLC
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

package org.cougaar.core.domain;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BoundComponent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentFactory;
import org.cougaar.core.component.ComponentRuntimeException;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainForBlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.PropertyParser;

/** A container for Domain Components.
 * <p>
 * A DomainManager expects all subcomponents to be bound with 
 * implementations of DomainBinder.  In return, the DomainManager
 * offers the DomainManagerForBinder to each DomainBinder.
 *
 * @property org.cougaar.core.load.planning
 *   If enabled, the domain manager will load the planning-specific
 *   PlanningDomain.  See bug 2522.  Default <em>true</em>
 **/
public class DomainManager 
  extends ContainerSupport
  implements StateObject
{

  private static final String FILENAME = "L"+"DMDomains.ini";

  private final static String PREFIX = "org.cougaar.domain.";
  private final static int PREFIXLENGTH = PREFIX.length();

  private final static boolean verbose = "true".equals(System.getProperty("org.cougaar.verbose","false"));

  private final static boolean isPlanningEnabled;

  static {
    isPlanningEnabled=PropertyParser.getBoolean("org.cougaar.core.load.planning", true);
  }

  /** Insertion point for a DomainManager, defined relative to its parent, Agent. **/
  public static final String INSERTION_POINT = 
    Agent.INSERTION_POINT + ".DomainManager";
  private final static String CONTAINMENT_POINT = INSERTION_POINT;

  private Object loadState = null;
  private HashSet xplans = new HashSet();
  private Blackboard blackboard = null;
  private ServiceBroker serviceBroker;

  private MessageAddress self;
  private AgentIdentificationService agentIdService;
  private LoggingService loggingService;
  private DomainServiceProvider domainSP;
  private DomainForBlackboardServiceProvider domainForBlackboardSP;

  public DomainManager() {
    if (!attachBinderFactory(new DefaultDomainBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultDomainBinderFactory");
    }
  }

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    serviceBroker = bs.getServiceBroker();
    setChildServiceBroker(new DomainManagerServiceBroker(bs));
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    this.self = ais.getMessageAddress();
  }

  private NodeControlService nodeControlService = null;
  public void setNodeControlService(NodeControlService ncs) {
    nodeControlService = ncs;
  }
  protected NodeControlService getNodeControlService() {
    return nodeControlService;
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  public void load() {
    super.load();

    domainSP = new DomainServiceProvider(new DomainServiceImpl(this));
    serviceBroker.addService(DomainService.class, domainSP);

    domainForBlackboardSP = 
      new DomainForBlackboardServiceProvider(new DomainForBlackboardServiceImpl(this));
    serviceBroker.addService(DomainForBlackboardService.class, 
                             domainForBlackboardSP);

    loggingService = 
      (LoggingService) serviceBroker.getService(this, LoggingService.class,
                                                null);
    if (loggingService == null) {
      System.out.println("DomainManager: unable to get LoggingService");
    }

    // display the agent id
    if (loggingService.isDebugEnabled()) {
      loggingService.debug("DomainManager "+this+" loading Domains for agent "+self);
    }

    // get an array of child Components
    Object[] children;
    if (loadState instanceof StateTuple[]) {
      // use the existing state
      children = (StateTuple[])loadState;
      loadState = null;
      // load the child Components (Domains, etc)
      int n = ((children != null) ? children.length : 0);
      for (int i = 0; i < n; i++) {
        add(children[i]);
      }
    } else {
      /* Order - root, planning, domain-file, agent.ini */
      List descs = new ArrayList(5);
      // setup the root domain
      addDomain(descs, "root", 
                "org.cougaar.core.domain.RootDomain");

      if (isPlanningEnabled) {
        // setup the planning domain
        addDomain(
            descs,
            "planning", 
            "org.cougaar.planning.ldm.PlanningDomain");
      }

      /* read domain file */ 
      initializeFromProperties(descs);
      initializeFromConfigFiles(descs);

      /* load root domain and domains specified in domain file */
      children = descs.toArray();
      for (int i = 0; i < children.length; i++) {
        add(children[i]);
      }      

      /* load domains specified in  agent.ini */
      loadFromComponentInitializer();
    }
  }

  public Object getState() {
    synchronized (boundComponents) {
      int n = boundComponents.size();
      StateTuple[] tuples = new StateTuple[n];
      for (int i = 0; i < n; i++) {
        BoundComponent bc = (BoundComponent)boundComponents.get(i);
        Object comp = bc.getComponent();
        if (comp instanceof ComponentDescription) {
          ComponentDescription cd = (ComponentDescription)comp;
          Binder b = bc.getBinder();
          Object state = b.getState();
          tuples[i] = new StateTuple(cd, state);
        }
      }
      return tuples;
    } 
  }


  public Collection getXPlans() {
    return (Collection) xplans.clone();
  }

  public XPlan getXPlanForDomain(String domainName) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      if (b.getDomain().getDomainName().equals(domainName)) {
        return b.getDomain().getXPlan();
      }
    }
    return null;
  }

  public XPlan getXPlanForDomain(Class domainClass) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      if (b.getDomain().getClass().equals(domainClass)) {
        return b.getDomain().getXPlan();
      }
    }
    return null;
  }

  public void setBlackboard(Blackboard blackboard) {
    if (this.blackboard != null) {
      LoggingService logger = 
        (LoggingService) serviceBroker.getService(this, LoggingService.class,
                                                  null);
      logger.warn("DomainManager: ignoring duplicate call to setBlackboard. " +
                  "Blackboard can only be set once.");
      return;
    }

    this.blackboard = blackboard;
    
    for (Iterator i = xplans.iterator(); i.hasNext();) {
      XPlan xplan = (XPlan) i.next();
      xplan.setupSubscriptions(this.blackboard);
    }
  }

  public void invokeDelayedLPActions() {
    for (Iterator i = xplans.iterator(); i.hasNext();) {
      XPlan xplan = (XPlan) i.next();
      if (xplan instanceof SupportsDelayedLPActions) {
        ((SupportsDelayedLPActions) xplan).executeDelayedLPActions();
      }
    }
  }

  public Factory getFactoryForDomain(String domainName) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      if (b.getDomain().getDomainName().equals(domainName)) {
        return b.getDomain().getFactory();
      }
    }
    return null;
  }

  public Factory getFactoryForDomain(Class domainClass) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      if (b.getDomain().getClass().equals(domainClass)) {
        return b.getDomain().getFactory();
      }
    }
    return null;
  }

  /** return a List of all domain-specific factories **/
  public List getFactories() {
    ArrayList factories = new ArrayList(size());
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      Factory f = b.getDomain().getFactory();
      if (f != null) {
        factories.add(f);
      }
    }
    return factories;
  }


  /** invoke EnvelopeLogicProviders across all currently loaded domains **/
  public void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                           boolean persistenceEnv) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      b.getDomain().invokeEnvelopeLogicProviders(tuple, persistenceEnv);
    }
  }

  /** invoke MessageLogicProviders across all currently loaded domains **/
  public void invokeMessageLogicProviders(DirectiveMessage message) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      b.getDomain().invokeMessageLogicProviders(message);
    }
  }

  /** invoke RestartLogicProviders across all currently loaded domains **/
  public void invokeRestartLogicProviders(MessageAddress cid) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      b.getDomain().invokeRestartLogicProviders(cid);
    }
  }

  /** invoke ABAChangeLogicProviders across all currently loaded domains **/
  public void invokeABAChangeLogicProviders(Set communities) {
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();) {
      DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
      b.getDomain().invokeABAChangeLogicProviders(communities);
    }
  }

  //
  // binding services
  //

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return CONTAINMENT_POINT;
  }

  protected Blackboard getBlackboard() {
    return blackboard;
  }


  /* load domains specified in  agent.ini */
  protected void loadFromComponentInitializer() {
    ComponentDescription [] children;
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    String cname = self.toString();

    try {
      children = cis.getComponentDescriptions(cname, specifyContainmentPoint());
    } catch (ComponentInitializerService.InitializerException cise) {
      //loggingService.error("Unable to add "+cname+"'s Domains", cise);
      if (loggingService.isInfoEnabled()) {
        loggingService.info("Unable to add "+cname+"'s Domains", cise);
      }
      children = null; 
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    } 
    
    // load the child Components (Plugins, etc)
    int n = ((children != null) ? children.length : 0);
    for (int i = 0; i < n; i++) {
      ComponentDescription cd = (ComponentDescription) children[i];
      
      if (cd != null) {
        String ip = cd.getInsertionPoint();
        // DomainManager should only load Domains!
        if (ip != null &&
            ip.startsWith(specifyContainmentPoint())) {
          try {
            add(cd);
          } catch (ComponentRuntimeException cre) {
            Throwable th = cre;
            while (true) {
              Throwable nx = th.getCause();
              if (nx == null) break;
              th = nx;
            }
            loggingService.error("Failed to load "+cd+":");
            th.printStackTrace();
          }
        }
      }
    }
  }

  
  private DomainManagerForBinder containerProxy = 
    new DomainManagerForBinder() {
        public ServiceBroker getServiceBroker() {
          return DomainManager.this.getServiceBroker();
        }
        public boolean remove(Object childComponent) {
          return DomainManager.this.remove(childComponent);
        }
        public void requestStop() {}

        public Collection getXPlans() {
          return DomainManager.this.getXPlans();
        }

        public XPlan getXPlanForDomain(String domainName) {
          return DomainManager.this.getXPlanForDomain(domainName);
        }

        public XPlan getXPlanForDomain(Class domainClass) {
          return DomainManager.this.getXPlanForDomain(domainClass);
        }

        public Factory getFactoryForDomain(String domainName) {
          return DomainManager.this.getFactoryForDomain(domainName);
        }

        public Factory getFactoryForDomain(Class domainClass) {
          return DomainManager.this.getFactoryForDomain(domainClass);
        }

      };

  protected ContainerAPI getContainerProxy() {
    return containerProxy;
  }

  //
  // typical implementations of state transitions --
  //   these might be moved into a base class...
  //
  // We really need a "container.lock()" to make these
  //   operations safe.  Mobility would like to lock down
  //   multiple steps, e.g. "suspend(); stop(); ..", without
  //   another Thread calling "add(..)" in between.
  //   
  protected boolean loadComponent(Object c, Object cstate) {
    if (super.loadComponent(c, cstate)) {

      // find the domain we just loaded
      Domain domain = null;
      if (c instanceof ComponentDescription) {
        Object []parameters = ((List)((ComponentDescription) c).getParameter()).toArray();

        if (parameters.length < 1) {
          throw new RuntimeException(
              "First element of the Domain ComponentDescription parameter List must specify the Domain name.");
        }
 
        String domainName = (String) parameters[0]; 
        for (Iterator childBinders = binderIterator();
            childBinders.hasNext();) {
          DefaultDomainBinder b = (DefaultDomainBinder) childBinders.next();
          Domain d = b.getDomain();
          if (d.getDomainName().equals(domainName)) {
            domain = d;
            break;
          }
        }
      } else if (c instanceof Domain) {
        // should be disabled!
        domain = (Domain) c;
      }

      if (domain == null) {
        throw new RuntimeException(
            "Unable to find domain for loaded "+c);
      }

      if (loggingService.isDebugEnabled()) {
        loggingService.debug("Loading : " + domain.getDomainName());
      }

      XPlan xplan = domain.getXPlan();
      if ((xplan != null) &&
          (xplans.add(xplan)) &&
          (getBlackboard() != null)) {
        xplan.setupSubscriptions(blackboard);
      }
      return true;
    } else {
      return false;
    }
  }
        

    // Can't simply cast o to a domain so .. iterate over the children
    // and see whether any have an xplan that I don't know about

    
  
  public void suspend() {
    super.suspend();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.suspend();
    }
  }

  public void resume() {
    super.resume();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.resume();
    }
  }

  public void stop() {
    super.stop();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.stop();
    }
  }

  public void halt() {
    // this seems reasonable:
    suspend();
    stop();
  }

  public void unload() {
    super.unload();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.unload();
    }
    boundComponents.clear();

    serviceBroker.revokeService(DomainService.class, domainSP);
    serviceBroker.revokeService(DomainForBlackboardService.class, 
                                domainForBlackboardSP);

    serviceBroker.releaseService(this, LoggingService.class, loggingService);
    if (agentIdService != null) {
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  
  
  //
  // support classes
  //

  private static class DomainManagerServiceBroker 
    extends PropagatingServiceBroker 
  {
    public DomainManagerServiceBroker(BindingSite bs) {
      super(bs);
    }
  }
  
  // 
  // other services
  //
  
  public String toString() {
    return self+"/DomainManager";
  }


  /** Set up a Domain from the argument strings.
   * @param descs a list of component-descriptions for all
   *    previously added domains
   * @param domainName the name to register the domain under.
   * @param className the name of the class to instantiate as the domain.
   **/
  private void addDomain(List descs, String domainName, 
                         String className) {
    // Unique?
    for (int i = 0, n = descs.size(); i < n; i++) {
      ComponentDescription cd =
        (ComponentDescription) descs.get(i);
      if (domainName.equals(cd.getName())) {
        if (loggingService.isWarnEnabled()) {
          loggingService.warn(
              "Domain \""+domainName+"\" multiply defined!  "+
              cd.getClassname()+" and "+className);
        }
        return;
      }
    }

    // we do not synchronize because it is only called from initialize()
    // which is synchronized...

    // pass the domain-name as a parameter
    Object parameter =
      Collections.singletonList(domainName);

    ComponentDescription desc = 
      new ComponentDescription(
          domainName,
          containmentPrefix+"Domain",
          className,
          null,  // codebase
          parameter,
          null,  // certificate
          null,  // lease
          null); // policy

    descs.add(desc);

    if (loggingService.isDebugEnabled()) {
      loggingService.debug(
          "Will add domain \""+domainName+"\" from class \""+className+"\".");
    }
  }

  private void initializeFromProperties(List descs) {
    Properties props = SystemProperties.getSystemPropertiesWithPrefix(PREFIX);
    for (Enumeration names = props.propertyNames(); names.hasMoreElements(); ) {
      String key = (String) names.nextElement();
      if (key.startsWith(PREFIX)) {
        String name = key.substring(PREFIXLENGTH);
        // domain names have no extra "." characters, so we can 
        // use -D arguments to control domain-related facilities.
        if (name.indexOf('.')<0) {
          String value = props.getProperty(key);
          addDomain(descs, name, value);
        }
      }
    }
  }
  
  private void initializeFromConfigFiles(List descs) {
    try {
      InputStream in = org.cougaar.util.ConfigFinder.getInstance().open(
          FILENAME);
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);

      String line;
      int lc = 0;
      for (line = br.readLine(); line != null; line=br.readLine()) {
        lc++;
        line = line.trim();
        if (line.length() == 0) continue;
        char c;
        if ( (c = line.charAt(0)) == ';' || c == '#' ) {
          continue;
        }
        int l = line.indexOf('=');
        if (l == -1) {
          loggingService.error(FILENAME+" syntax error: line "+lc);
          continue;
        }
        String name = line.substring(0,l).trim();
        String val = line.substring(l+1).trim();
        if (name.length()==0 || val.length()==0) {
          loggingService.error(FILENAME+" syntax error: line "+lc);
          continue;
        }
        addDomain(descs, name, val);
      }
    } catch (Exception ex) {
      if (! (ex instanceof FileNotFoundException)) {
        loggingService.error(FILENAME+" exception: "+ex);
        ex.printStackTrace();
      }
    }
  }

}


