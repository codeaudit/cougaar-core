package org.cougaar.core.society;

import org.cougaar.core.component.Service;

public interface NodeIdentificationService extends Service {
  public NodeIdentifier getNodeIdentifier();
}
