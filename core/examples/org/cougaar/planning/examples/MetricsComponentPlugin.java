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

import javax.swing.*;
import java.awt.event.*;
import java.awt.LayoutManager;
import java.awt.BorderLayout;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.BlackboardMetricsService;

import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.mts.MessageTransportWatcher;

import org.cougaar.core.service.NodeMetricsService;
import org.cougaar.core.mts.MessageStatistics.Statistics;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.Message;

import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Notification;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.blackboard.Directive;

import org.cougaar.core.blackboard.DirectiveMessage;

import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

public class MetricsComponentPlugin
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

      public MetricsComponentPlugin() {}

      protected void setupSubscriptions() {
      createGUI();
      protoRegistryService = (PrototypeRegistryService)
          getServiceBroker().getService(this, PrototypeRegistryService.class, 
                                        new ServiceRevokedListener() {
                                                public void serviceRevoked(ServiceRevokedEvent re) {
                                                    if (PrototypeRegistryService.class.equals(re.getService()))
                                                        protoRegistryService  = null;
                                                }
                                            });
      bbMetricsService = (BlackboardMetricsService)
          getServiceBroker().getService(this, BlackboardMetricsService.class, 
                                        new ServiceRevokedListener() {
                                                public void serviceRevoked(ServiceRevokedEvent re) {
                                                    if (BlackboardMetricsService.class.equals(re.getService())) {
                                                        bbMetricsService = null;
                                                    }
                                                }
                                            });
      nodeMetricsService = (NodeMetricsService)
          getServiceBroker().getService(this, NodeMetricsService.class, 
                                        new ServiceRevokedListener() {
                                                public void serviceRevoked(ServiceRevokedEvent re) {
                                                    if (NodeMetricsService.class.equals(re.getService())) {
                                                        nodeMetricsService = null;
                                                    }
                                                }
                                            });    
      messageStatsService = (MessageStatisticsService)
          getServiceBroker().getService(this, MessageStatisticsService.class, 
                                        new ServiceRevokedListener() {
                                                public void serviceRevoked(ServiceRevokedEvent re) {
                                                    if (MessageStatisticsService.class.equals(re.getService())) {
                                                        messageStatsService = null;
                                                    }
                                                }
                                            });
      messageWatchService = (MessageWatcherService)
          getServiceBroker().getService(this,MessageWatcherService.class, 
                                        new ServiceRevokedListener() {
                                                public void serviceRevoked(ServiceRevokedEvent re) {
                                                    if (MessageWatcherService.class.equals(re.getService()))
                                                        messageWatchService = null;
                                                }
                                            });   
      messageWatchService.addMessageTransportWatcher(_messageWatcher = new MessageWatcher());
      }

    private void createGUI() {
        frame = new JFrame("MetricsComponentPlugin");
        //          JPanel panel = new JPanel((LayoutManager) null);
        
        JPanel panel = new JPanel(new BorderLayout());
        // Create the button
        metricsButton = new JButton("Get Metrics");
        metricsLabel = new JLabel("Press to Retrieve Metrics.");
        metricsLabel.setHorizontalAlignment(JLabel.RIGHT);

        // Register a listener for the check box
        MetricsButtonListener myMetricsListener = new MetricsButtonListener();
        metricsButton.addActionListener(myMetricsListener);
        metricsButton.setEnabled(true);
        
        panel.add(metricsButton, BorderLayout.WEST);
        panel.add(metricsLabel, BorderLayout.EAST);
        //          UICoordinator.layoutButtonAndLabel(panel, metricsButton, metricsLabel);
        frame.setContentPane(panel);
        frame.pack();
        //          UICoordinator.setBounds(frame);
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

          //get all PrototypeRegistryService metrics
          cachedProtoCount = protoRegistryService.getCachedPrototypeCount();
          propProviderCount = protoRegistryService.getPropertyProviderCount();
          protoProviderCount = protoRegistryService.getPrototypeProviderCount();
          System.out.println("\n");
          System.out.println("Cached Prototype Count: " + cachedProtoCount);
          System.out.println("Property Provider Count: " + propProviderCount);
          System.out.println("Prototype Provider Count: " + protoProviderCount);

          //get all BlackBoardMetricsServices metrics
          assetCount = bbMetricsService.getBlackboardCount(Asset.class);
          planElementCount = bbMetricsService.getBlackboardCount(PlanElement.class);
          taskCount = bbMetricsService.getBlackboardCount(Task.class);
          totalBlackboardCount = bbMetricsService.getBlackboardCount();
          System.out.println("Asset Count: " + assetCount);
          System.out.println("Plan Element Count: " + planElementCount);
          System.out.println("Task Count: " + taskCount);
          System.out.println("Total Blackboard Object Count: " + totalBlackboardCount);

          //get all NodeMetricsSerivices metrics
          System.out.println("Active Thread Count: " + nodeMetricsService.getActiveThreadCount());
          System.out.println("Free Memory: " + nodeMetricsService.getFreeMemory());
          System.out.println("Total Memory: " + nodeMetricsService.getTotalMemory());

          //get all MessageStatistics metrics
          if (messageStatsService != null) {
              System.out.println("Message Queue: " + messageStatsService.getMessageStatistics(false).averageMessageQueueLength);
              System.out.println("Message Bytes: " + messageStatsService.getMessageStatistics(false).totalMessageBytes);
              System.out.println("Message Count: " + messageStatsService.getMessageStatistics(false).totalMessageCount);
              System.out.println("Histogram:     " + messageStatsService.getMessageStatistics(false).histogram); 
          }
          else
              System.out.println("MessageStatisticsService not returned");

          //get all MessageWatcher metrics
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
              me = getMessageAddress();
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
    
} // end of MetricsComponentPlugin.java
