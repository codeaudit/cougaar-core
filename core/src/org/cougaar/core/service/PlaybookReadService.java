package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.adaptivity.Play;

/**  
 * Part of the PlaybookService responsible for sending the
 * list of current plays
 */

public interface PlaybookReadService extends Service {
  interface Listener extends NotPersistable {
  }

  /**    
   * Called by AdaptivityEngine.
   * @return the active set of plays 
   */
  Play[] getCurrentPlays();

  void addListener(Listener l);
  void removeListener(Listener l);
}


