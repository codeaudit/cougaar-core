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
  /** number of active Threads in the main ALP threadgroup **/
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
}
