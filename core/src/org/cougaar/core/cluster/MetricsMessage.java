/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.society.MessageAddress;

public class MetricsMessage extends org.cougaar.core.society.Message {
  public long timestamp;
  public int directivesIn, directivesOut, notificationsIn, notificationsOut;
  public int assetCount, planelementCount, taskCount, workflowCount;

  public MetricsMessage(MessageAddress from, MessageAddress to,
                        long time,
                        int din, int dout,
                        int nin, int nout,
                        int as, int pes, int ts, int ws) {
    super(from, to);
    timestamp = time;
    directivesIn = din;
    directivesOut = dout;
    notificationsIn = nin;
    notificationsOut = nout;
    assetCount = as;
    planelementCount = pes;
    taskCount = ts;
    workflowCount = ws;
  }

}
                        
