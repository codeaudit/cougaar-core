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

package org.cougaar.planning.ldm.lps;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogPlanLogicProvider;
import org.cougaar.planning.ldm.plan.NewDeletion;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

import java.util.*;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** Watch the LogPlan and complain when obvious errors and 
 * other suspicious patterns are detected.  
 * <p>
 * This particular
 * implementation is only concerned with vetting Envelopes.
 * It does not attempt to detect any in-state Blackboard
 * or Message problems.
 * <p>
 * The specific tests that are performed are described by the
 * execute method.
 * <p>
 * Properties:
 * org.cougaar.planning.ldm.lps.ComplainingLP.level may
 * be set to an integer. Valid values are 0 (silent), 1 (report 
 * only definite errors), and 2 (report anything suspicious).
 * The default value is 2.
 **/


public class ComplainingLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider
{
  private static Logger logger = Logging.getLogger(ComplainingLP.class);

  private final static String levelPROP = "org.cougaar.planning.ldm.lps.ComplainingLP.level";

  private static int level = 2;

  private final static int levelQUIET = 0;
  private final static int levelERROR = 1;
  private final static int levelWARN = 2;
  
  static {
    level = (Integer.valueOf(System.getProperty(levelPROP, String.valueOf(level)))).intValue();
    if (level < levelQUIET) level = levelQUIET;
    if (level > levelWARN) level = levelWARN;
  }

  private ClusterIdentifier cid;

  public ComplainingLP(LogPlanServesLogicProvider logplan,
                       ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
    cid = cluster.getClusterIdentifier();
  }

  /**
   * Complain in any of the following cases:
   *  Unique Object Changed but not in logplan (error).
   *  Unique Object add/removed/changed which has the same UID as an existing object
   * but that is not identical (warning).
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    if (level <= 0) return;     // quiet.  better would be to refuse to plug-in.

    Object obj = o.getObject();
    if (obj instanceof UniqueObject) {
      UID objuid = ((UniqueObject)obj).getUID();
      Object found = logplan.findUniqueObject(objuid);
      boolean thereP = (found != null);
      if ((! thereP) && o.isChange() && level >= levelERROR)
        complain("change of non-existent object "+obj, obj);

      /*
        // cannot do these because LPs are applied after subscription updates.
      if ((! thereP) && o.isRemove() && level >= levelWARN)
        complain("redundant remove of object "+obj);

      if ((thereP) && o.isAdd() && level >= levelWARN)
        complain("redundant add of object "+obj);
      */
      if (thereP && found != obj && level >= levelWARN)
        complain("action="+o.getAction()+" on "+obj+" which is not == "+found, obj);

    }
  }
  private void complain(String complaint, Object obj) {
    logger.warn("Warning: "+cid+" ComplainingLP observed "+complaint);
    PublishHistory history = logplan.getHistory();
    if (history != null) history.dumpStacks(obj);
  }
}
