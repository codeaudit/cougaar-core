package org.cougaar.core.adaptivity;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.PlaybookConstrainService;

/** 
 * TBD
 * A PolicyManager that handles OperatingModePolicies
 * Must handle processing of policies: expand, forward, deconflict
 */

public class OperatingModePolicyManager extends ComponentPlugin implements PolicyManager {

  private PlaybookConstrainService myPlaybookService;
  
  public void setPlaybookConstrainService(PlaybookConstrainService cps) {
    myPlaybookService = cps;
  }

  public void setupSubscriptions() {
  }
  
  public void execute() {

  }
}

