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

  /** @see org.cougaar.core.component.BinderSupport#initializeChild()
   **/

public interface Plugin
  extends Component
{
  /**
   * Found by introspection
   **/
  void setBindingSite(BindingSite bs);

}
