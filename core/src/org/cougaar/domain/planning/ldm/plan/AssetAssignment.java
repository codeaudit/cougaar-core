/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.util.Enumeration;
import org.cougaar.domain.planning.ldm.asset.Asset;

/** AssetAssignment Interface identifies those method signatures associated 
 *  with AssetAssignment Directives.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AssetAssignment.java,v 1.2 2001-04-05 19:27:13 mthome Exp $
 **/

public interface AssetAssignment extends Directive {
  public static final byte UPDATE = 0;
  public static final byte NEW = 1;
  public static final byte REPEAT = 2;
  /**
   * Answer with the Asset to be assigned by this Directive.
   * @return org.cougaar.domain.planning.ldm.asset.Asset
   **/
  Asset getAsset();

  /**
    * Returns the schedule or time frame for this asset to be assigned.
    * @return Schedule associated with this asset.
    **/
  Schedule getSchedule();

  /**
   * Answer with the Asset to receive the assigned Asset
   * @return org.cougaar.domain.planning.ldm.asset.Asset
   **/
  Asset getAssignee();

  /**
   * @return true IFF this is an update message.
   **/
  boolean isUpdate();

  /**
   * @return true IF this may be a repeat message.
   **/
  boolean isRepeat();
}




