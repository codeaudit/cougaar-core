/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.planning.ldm.plan;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.util.UID;


/** NewAssetRescind Interface
 * Provides setter methods for object creation 
 * @author  ALPINE <alpine-software@bbn.com>
 *
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
