/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.core.society.UID;


/** NewAssetRescind Interface
 * Provides setter methods for object creation 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewAssetRescind.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface NewAssetRescind extends AssetRescind, NewDirective {
  /**
   * Sets the asset to be rescinded
   * @param asset - The Asset to be rescinded.
   **/
  void setAsset(Asset anAsset);

  /**
   * Sets the asset from which the asset will be rescinded
   * @param rescindee Asset
   **/
  void setRescindee(Asset rescindee);

  /**
   * Sets the schedule for which the asset will be rescinded
   * @param rescindSchedule Schedule
   **/
  void setSchedule(Schedule rescindSchedule);
}
