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

package org.cougaar.core.agent;

import java.io.Serializable;
import java.util.Date;

/** a snapshot of collected cluster metrics.
 * Various metrics come from different sources and may have 
 * very different horizons of validity:
 *  Cluster MessageTransport (CMT) - only available if Metrics 
 *    activated at the cluster level at startup. If the service is
 *    not available, these values will be -1.
 *  Cluster LogPlan (CLP) - always available, but not transaction-controlled
 *    so that numbers may be out of sync with other CL-class entries.
 *  Cluster (C) - always available.  Interesting slowly-changing cluster-related
 *    information.
 *  VM - always available, but all clusters running in the same node will
 *    have the same values.
 **/

public class MetricsSnapshot implements Serializable {
  /** The name of the Cluster **/
  public String clusterName = null;
  /** the name of the Node **/
  public String nodeName = null;
  /** the system time of the snapshot in millis **/
  public long time = 0L;
  /** count of directives received by this cluster. (CMT) **/
  public int directivesIn = -1;
  /** count of directives send by this cluster. (CMT) **/
  public int directivesOut = -1;
  /** count of notifications received by this cluster (CMT) **/
  public int notificationsIn = -1;
  /** count of notifications sent by this cluster (CMT) **/
  public int notificationsOut = -1;
  /** Statistics from MessageStatistics.Statistics **/
  public double averageMessageQueueLength = -1;
  public long totalMessageBytes = -1;
  public long totalMessageCount = -1;

  /** number of assets in the cluster's logplan. (CLP) **/
  public int assets = 0;
  /** number of planelements in the cluster's logplan. (CLP) **/
  public int planelements = 0;
  /** number of tasks in the cluster's logplan. (CLP) **/
  public int tasks = 0;
  /** number of workflows in the cluster's logplan. (CLP) **/
  public int workflows = 0;

  /** total number of plugins (C) **/
  public int pluginCount = 0;
  /** portion of pluginCount which share a single thread of execution (C) **/
  public int thinPluginCount = 0;
  /** portion of pluginCount which are prototypeProviders (C) **/
  public int prototypeProviderCount = 0;
  /** portion of pluginCount which are propertyProviders (C) **/
  public int propertyProviderCount = 0;
  /** number of cached asset prototypes (C) **/
  public int cachedPrototypeCount = 0;

  /** number of millis of idle time (VM). 
   * The default accuracy is approximately 5 seconds.
   **/
  public long idleTime = 0L;
  /** Runtime.freeMemory() at snapshot time (VM). **/
  public long freeMemory = 0L;
  /** Runtime.totalMemory() at snapshot time (VM). **/
  public long totalMemory = 0L;
  /** number of active Threads in the main COUGAAR threadgroup **/
  public int threadCount = 0;

  public String toString() {
    return "<MetricsSnapshot at "+(new Date(time))+">";
  }
  
  public String describe() {
    return nodeName+"/"+clusterName+"@"+time+":\n"+
      "\tdirectivesIn="+directivesIn+"\n"+
      "\tdirectivesOut="+directivesOut+"\n"+
      "\tnotificationsIn="+notificationsIn+"\n"+
      "\tnotificationsOut="+notificationsOut+"\n"+
      "\taverageMessageQueueLength="+averageMessageQueueLength+"\n"+
      "\ttotalMessageBytes="+totalMessageBytes+"\n"+
      "\ttotalMessageCount="+totalMessageCount+"\n"+
      "\tassets="+assets+"\n"+
      "\tplanelements="+planelements+"\n"+
      "\ttasks="+tasks+"\n"+
      "\tworkflows="+workflows+"\n"+
      "\tpluginCount="+pluginCount+"\n"+
      "\tthinPluginCount="+thinPluginCount+"\n"+
      "\tprototypeProviderCount="+prototypeProviderCount+"\n"+
      "\tpropertyProviderCount="+propertyProviderCount+"\n"+
      "\tcachedPrototypeCount="+cachedPrototypeCount+"\n"+
      "\tidleTime="+idleTime+"\n"+
      "\tfreeMemory="+freeMemory+"\n"+
      "\ttotalMemory="+totalMemory+"\n"+
      "\tthreadCount="+threadCount+"\n";
  }

  /**
   * Create an incremental metrics snapshot from 2 cumulative snapshots.<br>
   * Note that both such snapshots should have been created with the
   * boolean argument <em>false</em> to give reasonable results.<br>
   * Many fields will return values of <code>-1</code> if either original
   * was undefined, or the <code>latest</code> was less than the <code>base</code>.<br>
   * The following entries are not diffed, but taken from the base:<br>
   * <ul><li><code>clusterName</code></li>
   * <li><code>nodeName</code></li>
   * <li><code>pluginCount</code></li>
   * <li><code>thinPluginCount</code></li>
   * <li><code>prototypeProviderCount</code></li>
   * <li><code>propertyProviderCount</code></li></ul><br>
   * The time listed is that of the <code>latest</code> snapshot.<br>
   *
   * @param base a <code>MetricsSnapshot</code> with cumulative results to compare against
   * @param latest a <code>MetricsSnapshot</code> with recent cumulative results
   * @return a <code>MetricsSnapshot</code> containing the diffs (where reasonable) of the latest from the base
   */
  public static MetricsSnapshot calculateIncremental(MetricsSnapshot base, MetricsSnapshot latest) {
    MetricsSnapshot answer = new MetricsSnapshot();
    // some elements it doesnt make sense to diff
    answer.clusterName = base.clusterName;
    answer.nodeName = base.nodeName;
    answer.time = latest.time;

    // these make sense to diff. However, if either value is undefined,
    // or the newer is less than the older, then the answer is undefined
    answer.directivesIn = MetricsSnapshot.calcDiff(base.directivesIn, latest.directivesIn);
    answer.directivesOut = MetricsSnapshot.calcDiff(base.directivesOut, latest.directivesOut);
    answer.notificationsIn = MetricsSnapshot.calcDiff(base.notificationsIn, latest.notificationsIn);
    answer.notificationsOut = MetricsSnapshot.calcDiff(base.notificationsOut, latest.notificationsOut);
    answer.averageMessageQueueLength = MetricsSnapshot.calcDiff(base.averageMessageQueueLength, latest.averageMessageQueueLength);
    answer.totalMessageBytes = MetricsSnapshot.calcDiff(base.totalMessageBytes, latest.totalMessageBytes);
    answer.totalMessageCount = MetricsSnapshot.calcDiff(base.totalMessageCount, latest.totalMessageCount);
    
    // these next are not initialized to -1, but the calcDiff
    // will at least handle the new being less than the old
    answer.assets = MetricsSnapshot.calcDiff(base.assets, latest.assets);
    answer.planelements = MetricsSnapshot.calcDiff(base.planelements, latest.planelements);
    answer.tasks = MetricsSnapshot.calcDiff(base.tasks, latest.tasks);
    answer.workflows = MetricsSnapshot.calcDiff(base.workflows, latest.workflows);

    // some more elements that shouldn't change, so don't diff
    answer.pluginCount = base.pluginCount;
    answer.thinPluginCount = base.thinPluginCount;
    answer.prototypeProviderCount = base.prototypeProviderCount;
    answer.propertyProviderCount = base.propertyProviderCount;

    // this one might actually change
    answer.cachedPrototypeCount = MetricsSnapshot.calcDiff(base.cachedPrototypeCount, latest.cachedPrototypeCount);

    // and a last few, where negative diffs are OK
    answer.idleTime = latest.idleTime - base.idleTime;
    answer.freeMemory = latest.freeMemory - base.freeMemory;
    answer.totalMemory = latest.totalMemory - base.totalMemory;
    answer.threadCount = latest.threadCount - base.threadCount;

    return answer;
  } // end of static calculateIncremental()

  // if either value is -1 or latest < base, return an error: -1
  private static int calcDiff(int base, int latest) {
    if (latest == -1 || base == -1) {
      return -1;
    } else if (latest < base) {
      return -1;
    } else {
      return latest - base;
    }
  }
  
  // if either value is -1 or latest < base, return an error: -1
  private static long calcDiff(long base, long latest) {
    if (latest == -1 || base == -1) {
      return -1;
    } else if (latest < base) {
      return -1;
    } else {
      return latest - base;
    }
  }
  
  // if either value is -1 or latest < base, return an error: -1
  private static double calcDiff(double base, double latest) {
    if (latest == -1 || base == -1) {
      return -1;
    } else if (latest < base) {
      return -1;
    } else {
      return latest - base;
    }
  }
}
