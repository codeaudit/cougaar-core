package org.cougaar.core.society;

import org.cougaar.core.component.*;

public class NodeIdentificationServiceProvider implements ServiceProvider {
  private NodeIdentifier nodeID;

  public NodeIdentificationServiceProvider(NodeIdentifier nodeID) {
    this.nodeID = nodeID;
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (NodeIdentificationService.class.isAssignableFrom(serviceClass)) {
      return new NodeIdentificationServiceProxy();
    } else {
      return null;
    }
  }
  
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

  private final class NodeIdentificationServiceProxy implements NodeIdentificationService {
    public NodeIdentifier getNodeIdentifier() {
      return nodeID;
    } 
  } 
}
