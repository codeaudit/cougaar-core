/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.MessageTransportWatcher;

/**
 * A MessageTransportServiceProvider is a provider class that PluginManager calls
 * when a client requests a MessageTransportServer.
 */
public class MessageTransportServiceProvider implements ServiceProvider {

  private final MessageTransportServer mts;

  public MessageTransportServiceProvider(ClusterImpl agent) {
    this.mts = agent.getMessageTransportServer();
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return new MessageTransportServer() {
      public void sendMessage(Message m) {
        // should verify message contents
        mts.sendMessage(m);
      }
      public void registerClient(MessageTransportClient client) {
        // for now plugins are not allowed to register
        throw new UnsupportedOperationException();
      }
      public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
        // for now plugins not allowed to watch traffic
        throw new UnsupportedOperationException();
      }
      public String getIdentifier() {
        return mts.getIdentifier();
      }
      public boolean addressKnown(MessageAddress a) {
        return mts.addressKnown(a);
      }
    };
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
    // sharing cluster's MessageTransport, so do nothing
  }
}
