/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.examples;

import javax.swing.*;
import java.awt.event.*;
import java.awt.LayoutManager;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.domain.planning.ldm.PrototypeRegistryService;

import org.cougaar.core.blackboard.BlackboardMetricsService;

import org.cougaar.core.mts.MessageStatisticsService;
import org.cougaar.core.mts.MessageWatcherService;
import org.cougaar.core.mts.MessageTransportWatcher;

import org.cougaar.core.society.NodeMetricsService;
import org.cougaar.core.society.MessageStatistics.Statistics;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.Message;

import org.cougaar.domain.planning.ldm.plan.Notification;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.cluster.DirectiveMessage;

import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.domain.mlm.plugin.UICoordinator;
//import org.cougaar.core.cluster.ClusterIdentifier;

public class MetricsComponentPlugIn
    extends ComponentPlugin
{
    /** frame for 1-button UI **/
    private JFrame frame;    
    JLabel metricsLabel;
    protected JButton metricsButton;

    private PrototypeRegistryService protoRegistryService = null;
    private int cachedProtoCount = 0;
    private int propProviderCount = 0;
    private int protoProviderCount = 0;
    private BlackboardMetricsService bbMetricsService = null;
    private int assetCount = 0;
    private int planElementCount = 0;
    private int taskCount = 0;
    private int totalBlackboardCount = 0;
    private MessageStatisticsService messageStatsService = null;
    private MessageWatcherService  messageWatchService = null;
    private NodeMetricsService  nodeMetricsService = null;

    public MetricsComponentPlugIn() {}

    /** 
     * Do nothing
     */
    protected void setupSubscriptions() {
    createGUI();
    }

    private void createGUI() {
        frame = new JFrame("MetricsComponentPlugIn");
        JPanel panel = new JPanel((LayoutManager) null);
        // Create the button
        metricsButton = new JButton("Get Metrics");
        metricsLabel = new JLabel("Press to Retrieve Metrics.");
        
        // Register a listener for the check box
        MetricsButtonListener myMetricsListener = new MetricsButtonListener();
        metricsButton.addActionListener(myMetricsListener);
        metricsButton.setEnabled(true);
        UICoordinator.layoutButtonAndLabel(panel, metricsButton, metricsLabel);
        frame.setContentPane(panel);
        frame.pack();
        UICoordinator.setBounds(frame);
        frame.setVisible(true);
    }
 
    /** An ActionListener that listens to the GLS buttons. */
    class MetricsButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            getAllMetrics();
        }
    }

    /** 
     * Do nothing
     */
    public void execute() {}

    public void getAllMetrics() {
        protoRegistryService = (PrototypeRegistryService)
            getServiceBroker().getService(this, PrototypeRegistryService.class, 
                                          new ServiceRevokedListener() {
                                                  public void serviceRevoked(ServiceRevokedEvent re) {
                                                      if (PrototypeRegistryService.class.equals(re.getService()))
                                                          protoRegistryService  = null;
                                                  }
                                              }); 

        cachedProtoCount = protoRegistryService.getCachedPrototypeCount();
        propProviderCount = protoRegistryService.getPropertyProviderCount();
        protoProviderCount = protoRegistryService.getPrototypeProviderCount();
        System.out.println("\n");
        System.out.println("Cached Prototype Count: " + cachedProtoCount);
        System.out.println("Property Provider Count: " + propProviderCount);
        System.out.println("Prototype Provider Count: " + protoProviderCount);


        bbMetricsService = (BlackboardMetricsService)
            getServiceBroker().getService(this, BlackboardMetricsService.class, 
                                          new ServiceRevokedListener() {
                                                  public void serviceRevoked(ServiceRevokedEvent re) {
                                                      if (BlackboardMetricsService.class.equals(re.getService())) {
                                                          bbMetricsService = null;
                                                      }
                                                 }
                                              }); 
 
        assetCount = bbMetricsService.getAssetCount();
        planElementCount = bbMetricsService.getPlanElementCount();
        taskCount = bbMetricsService.getTaskCount();
        totalBlackboardCount = bbMetricsService.getBlackboardObjectCount();
        System.out.println("Asset Count: " + assetCount);
        System.out.println("Plan Element Count: " + planElementCount);
        System.out.println("Task Count: " + taskCount);
        System.out.println("Total Blackboard Object Count: " + totalBlackboardCount);

        nodeMetricsService = (NodeMetricsService)
            getServiceBroker().getService(this, NodeMetricsService.class, 
                                          new ServiceRevokedListener() {
                                                  public void serviceRevoked(ServiceRevokedEvent re) {
                                                      if (NodeMetricsService.class.equals(re.getService())) {
                                                           nodeMetricsService = null;
                                                      }
                                                 }
                                              }); 

        System.out.println("Active Thread Count: " + nodeMetricsService.getActiveThreadCount());
        System.out.println("Free Memory: " + nodeMetricsService.getFreeMemory());
        System.out.println("Total Memory: " + nodeMetricsService.getTotalMemory());


        messageStatsService = (MessageStatisticsService)
            getServiceBroker().getService(this, MessageStatisticsService.class, 
                                          new ServiceRevokedListener() {
                                                  public void serviceRevoked(ServiceRevokedEvent re) {
                                                      if (MessageStatisticsService.class.equals(re.getService())) {
                                                          messageStatsService = null;
                                                      }
                                                  }
                                              }); 
        //System.out.println("Just got messageStatsService, Value: " + messageStatsService);

        System.out.println("Message Queue: " + messageStatsService.getMessageStatistics(false).averageMessageQueueLength);
        System.out.println("Message Bytes: " + messageStatsService.getMessageStatistics(false).totalMessageBytes);
        System.out.println("Message Count: " + messageStatsService.getMessageStatistics(false).totalMessageCount);
        System.out.println("Histogram:     " + messageStatsService.getMessageStatistics(false).histogram); 
        
        messageWatchService = (MessageWatcherService)
            getServiceBroker().getService(this,MessageWatcherService.class, 
                                          new ServiceRevokedListener() {
                                                  public void serviceRevoked(ServiceRevokedEvent re) {
                                                      if (MessageWatcherService.class.equals(re.getService()))
                                                          messageWatchService = null;
                                                  }
                                              });
        //System.out.println("Just got messageWatchService, Value: " + messageWatchService);

        messageWatchService.addMessageTransportWatcher(_messageWatcher = new MessageWatcher());
        System.out.println("Directives In: " + _messageWatcher.getDirectivesIn());
        System.out.println("Directives Out: " + _messageWatcher.getDirectivesOut());
        System.out.println("Notifications In: " + _messageWatcher.getNotificationsIn());
        System.out.println("Notifications Out: " + _messageWatcher.getNotificationsOut());
       
    }  //close method getAllMetrics()
    
    protected MessageWatcher _messageWatcher = null;


    class MessageWatcher implements MessageTransportWatcher {

        MessageAddress me;
        private int directivesIn = 0;
        private int directivesOut = 0;
        private int notificationsIn = 0;
        private int notificationsOut = 0;
        
        public MessageWatcher() {
            //            me = getClusterIdentifier();
            me = getBindingSite().getAgentIdentifier();
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
        } // close messageSent

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
        } // close messageReceived

        public int getDirectivesIn() {
            return directivesIn;
        }
        public int getDirectivesOut() {
            return directivesOut;
        }
        public int getNotificationsIn() {
            return notificationsIn;
        }
        public int getNotificationsOut() {
            return notificationsOut;
        }
    }   // end of MessageWatcher
    
} // end of MetricsComponentPlugIn.java
