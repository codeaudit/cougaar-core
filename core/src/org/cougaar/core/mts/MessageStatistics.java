/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.mts;


/**
 * Abstract MessageStatistics layer for Society interaction.
 * Used for Scalability testing.
 **/

public interface MessageStatistics {
  int[] BIN_SIZES = {
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

  int NBINS = BIN_SIZES.length;

  class Statistics {
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
