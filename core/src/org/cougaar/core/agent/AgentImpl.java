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

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModel;

/**
 * AgentImpl is the base class for all agents.
 * <p>
 * An agent starts with a single "bootstrap" component that uses
 * the AgentBootstrapService to specify the subsequent agent
 * components.
 */
public class AgentImpl extends Agent {

  // this agent's address
  private MessageAddress localAgent;

  /** Alias for getMessageAddress, required by Agent superclass */
  public MessageAddress getAgentIdentifier() {
    return localAgent;
  }

  /**
   * Expects the parameter to specify the MessageAddress,
   * either through a single String or the first element of
   * a List.
   */
  public void setParameter(Object o) {
    MessageAddress cid = null;
    if (o instanceof MessageAddress) {
      cid = (MessageAddress) o;
    } else if (o instanceof String) {
      cid = MessageAddress.getMessageAddress((String) o);
    } else if (o instanceof List) {
      List l = (List) o;
      if (!l.isEmpty()) {
        Object o1 = l.get(0);
        if (o1 instanceof MessageAddress) {
          cid = (MessageAddress) o1;
        } else if (o1 instanceof String) {
          cid = MessageAddress.getMessageAddress((String) o1);
        }
      }
    }
    if (cid == null) {
      throw new IllegalArgumentException(
          "Invalid agent parameter: "+o);
    }
    this.localAgent = cid;
  }

  // disable super load sequence
  protected void loadHighPriorityComponents() {}
  protected void loadInternalPriorityComponents() {}
  protected void loadBinderPriorityComponents() {}
  protected void loadComponentPriorityComponents() {}
  protected void loadLowPriorityComponents() {}

  protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }
  protected ComponentDescriptions findExternalComponentDescriptions() {
    return null;
  }
 
  public Object getState() {
    return null;
  }
  public void setState(Object o) {
    return;
  }

  public void load() {
    // can't call super.load()!
    transitState("load()", UNLOADED, LOADED);

    add_agent_state_model_service();
    add_agent_containment_service();
    add_agent_component_model_service();

    List l = new LinkedList();

    ServiceProvider sp = add_agent_component_list_service(l);

    this.add_agent_identification_service(localAgent);

    // start with a bootstrap component, which must use our
    // AgentBootstrapService to add more components that we
    // will load
    l.add(getBootstrapDescription());

    while (!l.isEmpty()) {
      Object o = l.get(0);
      l.remove(0);
      if (o != null) {
        // add component, may change our list!
        add(o);
      }
    }

    // done loading
    revoke_agent_component_list_service(sp);
  }

  private ComponentDescription getBootstrapDescription() {
    String classname = getBootstrapClass();
    return
      new ComponentDescription(
          classname,
          "Node.AgentManager.Agent.Component",
          classname,
          null,  //codebase
          null,  //params
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH);
  }
  private String getBootstrapClass() {
    // FIXME add a system property
    return "org.cougaar.core.agent.Bootstrap";
  }

  private ServiceProvider add_agent_state_model_service() {
    final GenericStateModel agentModel = this;
    Class clazz = AgentStateModelService.class;
    Service service =
      new AgentStateModelService() {
        // forward all agent state transitions
        public void initialize()   { agentModel.initialize(); }
        public void load()         { agentModel.load(); }
        public void start()        { agentModel.start(); }
        public void suspend()      { agentModel.suspend(); }
        public void resume()       { agentModel.resume(); }
        public void stop()         { agentModel.stop(); }
        public void halt()         { agentModel.halt(); }
        public void unload()       { agentModel.unload(); }
        public int getModelState() { return agentModel.getModelState(); }
      };
    return add_service(clazz, service);
  }

  private ServiceProvider add_agent_containment_service() {
    final Collection agent = this;
    Class clazz = AgentContainmentService.class;
    Service service =
      new AgentContainmentService() {
        // forward all operations
        public boolean add(ComponentDescription desc) {
          return agent.add(desc);
        }
        public boolean remove(ComponentDescription desc) {
          return agent.remove(desc);
        }
        public boolean contains(ComponentDescription desc) {
          return agent.contains(desc);
        }
      };
    return add_service(clazz, service);
  }

  private ServiceProvider add_agent_component_model_service() {
    final AgentImpl agent = this;
    Class clazz = AgentComponentModelService.class;
    Service service =
      new AgentComponentModelService() {
        public ComponentDescriptions getComponentDescriptions() {
          return agent.captureState();
        }
      };
    return add_service(clazz, service);
  }

  private ServiceProvider add_agent_component_list_service(
      final List l) {
    final AgentImpl agent = this;
    Class clazz = AgentBootstrapService.class;
    Service service =
      new AgentBootstrapService() {
        public void overrideComponentList(List newlist) {
          l.clear();
          if (newlist != null) {
            l.addAll(newlist);
          }
        }
      };
    return add_service(clazz, service);
  }

  private void revoke_agent_component_list_service(
      ServiceProvider sp) {
    Class clazz = AgentBootstrapService.class;
    revoke_service(clazz, sp);
  }

  private void add_agent_identification_service(
      final MessageAddress addr) {
    Class clazz = AgentIdentificationService.class;
    Service service =
      new AgentIdentificationService() {
        public MessageAddress getMessageAddress() {
          return addr;
        }
        public String getName() {
          return addr.getAddress();
        }
      };
    add_service(clazz, service);
  }

  private ServiceProvider add_service(
      Class clazz, Service service) {
    ServiceBroker csb = getChildServiceBroker();
    ServiceProvider sp = new SimpleServiceProvider(clazz, service);
    csb.addService(clazz, sp);
    return sp;
  }
  private void revoke_service(
      Class clazz, ServiceProvider sp) {
    if (sp != null) {
      ServiceBroker csb = getChildServiceBroker();
      csb.revokeService(clazz, sp);
    }
  }
  private static final class SimpleServiceProvider
    implements ServiceProvider {
      private final Class clazz;
      private final Service service;
      public SimpleServiceProvider(
          Class clazz, Service service) {
        this.clazz = clazz;
        this.service = service;
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (clazz.isAssignableFrom(serviceClass)) {
          return service;
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
