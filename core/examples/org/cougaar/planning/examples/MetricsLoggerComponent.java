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

package org.cougaar.planning.examples;

import java.util.Collection;
import java.util.Iterator;
import java.util.TimerTask;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageStatistics;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardMetricsService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.service.NodeMetricsService;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Notification;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * Component that periodically logs sample metrics.
 * <p>
 * This component can be added to any or all agents.  Load as:<pre>
 *   plugin = org.cougaar.core.examples.MetricsLoggerComponent(csv=true, delay=30000, interval=1000)
 * </pre>  See below for more parameter options.
 * <p>
 * MTS message byte and count requires this aspect:<pre>
 *   -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.StatisticsAspect
 * </pre><p>
 * CPULoad detection requires this node-agent plugin:<pre>
 *   plugin = org.cougaar.core.thread.AgentSensorPlugin
 * </pre><p>
 * Here's a sample log4j configuration file that logs to a file
 * named "metrics.csv":<pre>
 *   log4j.rootCategory=WARN, stdout
 *   log4j.appender.stdout=org.apache.log4j.ConsoleAppender
 *   log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
 *   log4j.appender.stdout.layout.ConversionPattern=%d{ABSOLUTE} %-5p - %c{1} - %m%n
 *   log4j.appender.metrics=org.apache.log4j.RollingFileAppender
 *   log4j.appender.metrics.File=metrics.csv
 *   log4j.appender.metrics.MaxFileSize=5024KB
 *   log4j.appender.metrics.MaxBackupIndex=1
 *   log4j.appender.metrics.layout=org.apache.log4j.PatternLayout
 *   log4j.appender.metrics.layout.ConversionPattern=%m%n
 *   log4j.additivity.org.cougaar.core.examples.MetricsLoggerComponent=false
 *   log4j.category.org.cougaar.core.examples.MetricsLoggerComponent=DEBUG,metrics
 * </pre>  Save the above log4j configuration in a file named:<pre>
 *   $COUGAAR_INSTALL_PATH/configs/common/log.props
 * </pre> and enable with this system property: <pre>
 *   -Dorg.cougaar.core.logging.config.filename=log.props
 * </pre>
 */
public class MetricsLoggerComponent
extends GenericStateModelAdapter
implements Component
{

  /**
   * Default component parameters are:<pre>
   *   csv=false       <i>log human-readable output, not CSV</i>
   *   delay=30000     <i>wait thirty seconds before starting</i>
   *   interval=1000   <i>once starting, log once per second</i>
   * </pre>
   */
  private boolean useCSV = true;
  private long delay = 10000;
  private long interval = 1000;

  private ServiceBroker serviceBroker;
  private LoggingService rawLogger = LoggingService.NULL;
  private LoggingService logger = rawLogger; // "AGENT: "+logger
  private AgentIdentificationService agentIdService;
  private NodeIdentificationService nodeIdService;
  private ThreadService threadService;
  private PrototypeRegistryService protoRegistryService;
  private BlackboardMetricsService bbMetricsService;
  private MetricsService metricsService;
  private MessageStatisticsService messageStatsService;
  private MessageWatcherService messageWatcherService;
  private NodeMetricsService nodeMetricsService;

  private MessageAddress agentId;
  private MessageAddress nodeId;
  private String cpuPath;
  private String toMsgPath;
  private String fromMsgPath;
  protected MessageWatcher myMessageWatcher;

  // gory service load/unload is at the end.
  // 
  // here we show the interesting stuff up front:

  private void logAllMetrics() {
    if (logger.isInfoEnabled()) {
      if (useCSV) {
        logCSV();
      } else { 
        logVerbose();
      }
    }
  }

  private void logVerbose() {
    logProtoMetrics();
    logBlackboardMetrics();
    logNodeMetrics();
    logQosMetrics();
    logMessageStatsMetrics();
    logMessageWatcherMetrics();
  }

  private void logProtoMetrics() {
    if (protoRegistryService != null) {
      logger.info("Cached Prototype Count: " + protoRegistryService.getCachedPrototypeCount());
      logger.info("Property Provider Count: " + protoRegistryService.getPropertyProviderCount());
      logger.info("Prototype Provider Count: " + protoRegistryService.getPrototypeProviderCount());
    }
  }

  private void logBlackboardMetrics() {
    if (bbMetricsService != null) {
      logger.info("Asset Count: " + bbMetricsService.getBlackboardCount(Asset.class));
      logger.info("Plan Element Count: " + bbMetricsService.getBlackboardCount(PlanElement.class));
      logger.info("Task Count: " + bbMetricsService.getBlackboardCount(Task.class));
      logger.info("Total Blackboard Object Count: " + bbMetricsService.getBlackboardCount());
    }
  }

  private void logNodeMetrics() {
    if (nodeMetricsService != null) {
      logger.info("Active Thread Count: " + nodeMetricsService.getActiveThreadCount());
      logger.info("Free Memory: " + nodeMetricsService.getFreeMemory());
      logger.info("Total Memory: " + nodeMetricsService.getTotalMemory());
    }
  }


  private void logQosMetrics() {
    logger.info("CPU Load: "+getMetric(cpuPath, 0.0));
    logger.info("Sent To: "+getMetric(toMsgPath, 0.0));
    logger.info("Heard From: "+getMetric(fromMsgPath, 0.0));
  }

  private void logMessageStatsMetrics() {
    if (messageStatsService != null) {
      MessageStatistics.Statistics stats = messageStatsService.getMessageStatistics(false);
      logger.info("Message Queue: " + stats.averageMessageQueueLength);
      logger.info("Message Bytes: " + stats.totalMessageBytes);
      logger.info("Message Count: " + stats.totalMessageCount);
      long[] h = stats.histogram;
      int n = (h != null ? h.length : 0);
      n = Math.min(n, MessageStatistics.NBINS);
      for (int i = 0; i < n; i++) {
        int bin = MessageStatistics.BIN_SIZES[i];
        logger.info("Histogram["+bin+"]: "+h[i]);
      }
    }
  }

  private void logMessageWatcherMetrics() {
    if (myMessageWatcher != null) {
      logger.info("Directives In: " + myMessageWatcher.directivesIn);
      logger.info("Directives Out: " + myMessageWatcher.directivesOut);
      logger.info("Notifications In: " + myMessageWatcher.notificationsIn);
      logger.info("Notifications Out: " + myMessageWatcher.notificationsOut);
    }
  }

  private void logCSVHeader() {
    rawLogger.info(getCSVHeader());
  }

  private String getCSVHeader() {
    StringBuffer buf = new StringBuffer();
    buf.append(
        "#Agent"+
        ", Time_In_Millis"+
        ", Cached_Prototype_Count"+
        ", Property_Provider_Count"+
        ", Prototype_Provider_Count"+
        ", Asset_Count"+
        ", Plan_Element_Count"+
        ", Task_Count"+
        ", Total_Blackboard_Object_Count"+
        ", Active_Thread_Count"+
        ", Free_Memory"+
        ", Total_Memory"+
        ", CPU_Load"+
        ", Sent_To"+
        ", Heard_From"+
        ", Message_Queue"+
        ", Message_Bytes"+
        ", Message_Count");
    for (int i = 0; i < MessageStatistics.NBINS; i++) {
      int bin = MessageStatistics.BIN_SIZES[i];
      buf.append(", Histogram_").append(bin);
    }
    buf.append(
        ", Directives_In"+
        ", Directives_Out"+
        ", Notifications_In"+
        ", Notifications_Out");
    return buf.toString();
  }

  private void logCSV() {
    rawLogger.info(getCSV());
  }

  private String getCSV() {
    StringBuffer buf = new StringBuffer();
    buf.append(agentId.getAddress());
    buf.append(", ").append(System.currentTimeMillis());
    if (protoRegistryService != null) {
      buf.append(", ").append(protoRegistryService.getCachedPrototypeCount());
      buf.append(", ").append(protoRegistryService.getPropertyProviderCount());
      buf.append(", ").append(protoRegistryService.getPrototypeProviderCount());
    } else {
      buf.append(", 0, 0, 0");
    }
    if (bbMetricsService != null) {
      buf.append(", ").append(bbMetricsService.getBlackboardCount(Asset.class));
      buf.append(", ").append(bbMetricsService.getBlackboardCount(PlanElement.class));
      buf.append(", ").append(bbMetricsService.getBlackboardCount(Task.class));
      buf.append(", ").append(bbMetricsService.getBlackboardCount());
    } else {
      buf.append(", 0, 0, 0, 0");
    }
    if (nodeMetricsService != null) {
      buf.append(", ").append(nodeMetricsService.getActiveThreadCount());
      buf.append(", ").append(nodeMetricsService.getFreeMemory());
      buf.append(", ").append(nodeMetricsService.getTotalMemory());
    } else {
      buf.append(", 0, 0, 0");
    }
    buf.append(", ").append(getMetric(cpuPath, 0.0));
    buf.append(", ").append(getMetric(toMsgPath, 0.0));
    buf.append(", ").append(getMetric(fromMsgPath, 0.0));
    if (messageStatsService != null) {
      MessageStatistics.Statistics stats = 
        messageStatsService.getMessageStatistics(false);
      buf.append(", ").append(stats.averageMessageQueueLength);
      buf.append(", ").append(stats.totalMessageBytes);
      buf.append(", ").append(stats.totalMessageCount);
      long[] h = stats.histogram;
      for (int i = 0; i < MessageStatistics.NBINS; i++) {
        long hi = (i < h.length ? h[i] : 0l);
        buf.append(", ").append(hi);
      }
    } else {
      buf.append(", 0, 0, 0");
      for (int i = 0; i < MessageStatistics.NBINS; i++) {
        buf.append(", 0");
      }
    }
    if (myMessageWatcher != null) {
      buf.append(", ").append(myMessageWatcher.directivesIn);
      buf.append(", ").append(myMessageWatcher.directivesOut);
      buf.append(", ").append(myMessageWatcher.notificationsIn);
      buf.append(", ").append(myMessageWatcher.notificationsOut);
    } else {
      buf.append(", 0, 0, 0, 0");
    }
    return buf.toString();
  }

  private double getMetric(String s, double val) {
    Metric metric = metricsService.getValue(s);
    return (metric != null ? metric.doubleValue() : val);
  }

  // service load/unload:

  public void setParameter(Object o) {
    try {
      Collection c = (Collection) o;
      for (Iterator iter = c.iterator(); iter.hasNext(); ) {
        String s = (String) iter.next();
        int sep = s.indexOf("=");
        String n = s.substring(0, sep);
        String v = s.substring(sep+1);
        if (n.equals("csv") || n.equals("useCSV") || n.equals("cvs")) {
          useCSV = "true".equals(v);
        } else if (n.equals("delay")) {
          delay = Long.parseLong(v);
        } else if (n.equals("interval")) {
          interval = Long.parseLong(v);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Usage: \"csv=BOOLEAN, delay=MILLIS, interval=MILLIS\"",
          e);
    }
  }

  public void setBindingSite(BindingSite bs) {
    serviceBroker = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService s) {
    if (s != null) {
      rawLogger = s;
      logger = s;
    }
  }
  public void setAgentIdentificationService(AgentIdentificationService s) {
    agentIdService = s;
    if (agentIdService != null) {
      agentId = agentIdService.getMessageAddress();
    }
  }
  public void setNodeIdentificationService(NodeIdentificationService s) {
    nodeIdService = s;
    if (nodeIdService != null) {
      nodeId = nodeIdService.getMessageAddress();
    }
  }
  public void setThreadService(ThreadService s) {
    threadService = s;
  }
  public void setPrototypeRegistryService(PrototypeRegistryService s) {
    protoRegistryService = s;
  }
  public void setBlackboardMetricsService(BlackboardMetricsService s) {
    bbMetricsService = s;
  }
  public void setNodeMetricsService(NodeMetricsService s) {
    nodeMetricsService = s;
  }
  public void setMetricsService(MetricsService s) {
    metricsService = s;
  }
  public void setMessageStatisticsService(MessageStatisticsService s) {
    messageStatsService = s;
  }
  public void setMessageWatcherService(MessageWatcherService s) {
    messageWatcherService = s;
  }

  public void load() {
    super.load();
    if (agentId != null) {
      logger = LoggingServiceWithPrefix.add(
          rawLogger, agentId.getAddress()+": ");
    }
    if (metricsService != null && agentId != null && nodeId != null) {
      String agentName = agentId.getAddress();
      String nodeName = nodeId.getAddress();
      String agentPath =
        "Agent("+agentName+")"+Constants.PATH_SEPR;
      String destPath=
        "Node("+nodeName+")"+Constants.PATH_SEPR+
        "Destination("+agentName+")"+Constants.PATH_SEPR;
      cpuPath =
        agentPath+Constants.CPU_LOAD_AVG_1_SEC_AVG;
      toMsgPath =
        destPath+Constants.MSG_TO_10_SEC_AVG;
      fromMsgPath =
        destPath+Constants.MSG_FROM_10_SEC_AVG;
    }
    if (messageWatcherService != null && agentId != null) {
      myMessageWatcher = new MessageWatcher(agentId);
      messageWatcherService.addMessageTransportWatcher(myMessageWatcher);
    }
    if (logger.isWarnEnabled()) {
      boolean b = false;
      if (agentIdService == null) {
        logger.warn("Unable to obtain AgentIdentificationService");
        b = true;
      }
      if (nodeIdService == null) {
        logger.warn("Unable to obtain NodeIdentificationService");
        b = true;
      }
      if (threadService == null) {
        logger.warn("Unable to obtain ThreadService");
        b = true;
      }
      if (protoRegistryService == null) {
        logger.warn("Unable to obtain PrototypeRegistryService");
        b = true;
      }
      if (bbMetricsService == null) {
        logger.warn("Unable to obtain BlackboardMetricsService");
        b = true;
      }
      if (nodeMetricsService == null) {
        logger.warn("Unable to obtain NodeMetricsService");
        b = true;
      }
      if (metricsService == null) {
        logger.warn("Unable to obtain MetricsService");
        b = true;
      }
      if (messageStatsService == null) {
        logger.warn("Unable to obtain MessageStatisticsService");
        b = true;
      }
      if (messageWatcherService == null) {
        logger.warn("Unable to obtain MessageWatcherService");
        b = true;
      }
      if (!b && logger.isInfoEnabled()) {
        logger.info("Loaded all necessary services");
      }
    }

    if (useCSV && logger.isInfoEnabled()) {
      logCSVHeader();
    }

    if (threadService == null) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to obtain ThreadService");
      }
    } else {
      TimerTask poller = new TimerTask() {
        public void run() { 
          logAllMetrics();
        }
      };
      threadService.schedule(poller, delay, interval);
    }
  }

  public void unload() {
    ServiceBroker sb = serviceBroker;
    if (agentIdService != null) {
      sb.releaseService(this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (nodeIdService != null) {
      sb.releaseService(this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    if (threadService != null) {
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (protoRegistryService != null) {
      sb.releaseService(this, PrototypeRegistryService.class, protoRegistryService);
      protoRegistryService = null;
    }
    if (bbMetricsService != null) {
      sb.releaseService(this, BlackboardMetricsService.class, bbMetricsService);
      bbMetricsService = null;
    }
    if (nodeMetricsService != null) {
      sb.releaseService(this, NodeMetricsService.class, nodeMetricsService);
      nodeMetricsService = null;
    }
    if (metricsService != null) {
      sb.releaseService(this, MetricsService.class, metricsService);
      metricsService = null;
    }
    if (messageStatsService != null) {
      sb.releaseService(this, MessageStatisticsService.class, messageStatsService);
      messageStatsService = null;
    }
    if (messageWatcherService != null) {
      if (myMessageWatcher != null) {
        messageWatcherService.removeMessageTransportWatcher(myMessageWatcher);
        myMessageWatcher = null;
      }
      sb.releaseService(this, MessageWatcherService.class, messageWatcherService);
      messageWatcherService = null;
    }
    if (rawLogger != null && rawLogger != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, rawLogger);
      rawLogger = LoggingService.NULL;
      logger = rawLogger;
    }
    super.unload();
  }

  private static class MessageWatcher implements MessageTransportWatcher {
    private final MessageAddress me;
    public int directivesIn = 0;
    public int directivesOut = 0;
    public int notificationsIn = 0;
    public int notificationsOut = 0;
    public MessageWatcher(MessageAddress me) {
      this.me = me;
    }
    public void messageSent(Message m) {
      if (m.getOriginator().equals(me)) {
        if (m instanceof DirectiveMessage) {
          Directive[] directives = ((DirectiveMessage)m).getDirectives();
          for (int i = 0; i < directives.length; i++) {
            if (directives[i] instanceof Notification)
              notificationsOut++;
            else
              directivesOut++;
          }
        }
      }
    }
    public void messageReceived(Message m) {
      if (m.getTarget().equals(me)) {
        if (m instanceof DirectiveMessage) {
          Directive[] directives = ((DirectiveMessage)m).getDirectives();
          for (int i = 0; i < directives.length; i++) {
            if (directives[i] instanceof Notification)
              notificationsIn++;
            else
              directivesIn++;
          }
        }
      }
    }
  }
}
