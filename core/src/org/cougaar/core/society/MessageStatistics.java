/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

/**
 * Abstract MessageStatistics layer for Society interaction.
 * Used for Scalability testing.
 **/

public interface MessageStatistics {
  public static final int[] BIN_SIZES = {
    100,
    200,
    500,
    1000,
    2000,
    5000,
    10000,
    20000,
    50000,
    100000
  };
  public static final int NBINS = BIN_SIZES.length;
  public class Statistics {
    public double averageMessageQueueLength;
    public long totalMessageBytes;
    public long totalMessageCount;
    public long[] histogram = new long[NBINS];
    public Statistics(double amql, long tmb, long tmc, long[] h) {
      averageMessageQueueLength = amql;
      totalMessageBytes = tmb;
      totalMessageCount = tmc;
      if (h != null) {
        System.arraycopy(h, 0, histogram, 0, NBINS);
      }
    }
  }
      
  Statistics getMessageStatistics(boolean reset);
}
