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

import java.util.Enumeration;
import org.cougaar.planning.ldm.asset.Asset;

/** AssetAssignment Interface identifies those method signatures associated 
 *  with AssetAssignment Directives.
 **/

public interface AssetAssignment extends Directive {
  byte UPDATE = 0;
  byte NEW = 1;
  byte REPEAT = 2;

  /**
   * Answer with the Asset to be assigned by this Directive.
   * @return org.cougaar.planning.ldm.asset.Asset
   **/
  Asset getAsset();

  /**
    * Returns the schedule or time frame for this asset to be assigned.
    * @return Schedule associated with this asset.
    **/
  Schedule getSchedule();

  /**
   * Answer with the Asset to receive the assigned Asset
   * @return org.cougaar.planning.ldm.asset.Asset
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




