/*
 * <copyright>
 *  Copyright 2001 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.lps;

import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.plan.NewDeletion;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.util.UnaryPredicate;

import java.util.*;

/** Watch the LogPlan and complain when obvious errors and 
 * other suspicious patterns are detected.  
 * <p>
 * This particular
 * implementation is only concerned with vetting Envelopes.
 * It does not attempt to detect any in-state Whiteboard
 * or Message problems.
 * <p>
 * The specific tests that are performed are described by the
 * execute method.
 * <p>
 * Properties:
 * org.cougaar.domain.planning.ldm.lps.ComplainingLP.level may
 * be set to an integer. Valid values are 0 (silent), 1 (report 
 * only definite errors), and 2 (report anything suspicious).
 * The default value is 2.
 **/


public class ComplainingLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider
{
  private final static String levelPROP = "org.cougaar.domain.planning.ldm.lps.ComplainingLP.level";
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
        complain("change of non-existent object "+obj);

      /*
        // cannot do these because LPs are applied after subscription updates.
      if ((! thereP) && o.isRemove() && level >= levelWARN)
        complain("redundant remove of object "+obj);

      if ((thereP) && o.isAdd() && level >= levelWARN)
        complain("redundant add of object "+obj);
      */
      if (thereP && found != obj && level >= levelWARN)
        complain("action="+o.getAction()+" on "+obj+" which is not == "+found);

    }
  }
  private void complain(String complaint) {
    System.err.println("Warning: "+cid+" ComplainingLP observed "+complaint);
  }
}
