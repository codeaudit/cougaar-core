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

package org.cougaar.core.examples;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.LayoutManager;
import java.awt.BorderLayout;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.node.NodeTrustPolicy;
import org.cougaar.core.node.PolicyMulticastMessage;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.service.DomainService;

public class PanicButtonPlugin
  extends ComponentPlugin implements MessageTransportClient
{
  /** frame for 1-button UI **/
  private JFrame frame;    
  JLabel panicLabel;
  private JComboBox categoryCB;
  private ButtonGroup trustlevelBG;
  private JRadioButton trustNoOne, trustFew, trustMedium, trustEveryone;
  protected JButton panicButton;
  
  private MessageTransportService messageTransService = null;
  private DomainService domainService = null;
    
  public PanicButtonPlugin() {}

  protected void setupSubscriptions() {
    createGUI();
    // setup and register message transport service
    messageTransService = (MessageTransportService)
      getServiceBroker().getService(this, MessageTransportService.class, 
                                    new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          if (MessageTransportService.class.equals(re.getService())) {
            messageTransService = null;
          }
        }
      });    
    messageTransService.registerClient(this);

    // setup domain service
    domainService = (DomainService)
      getServiceBroker().getService(this, DomainService.class, 
                                    new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          if (DomainService.class.equals(re.getService())) {
            domainService = null;
          }
        }
      });

  }

  private void createGUI() {
    frame = new JFrame("PanicButtonPlugin");
        
    JPanel panel = new JPanel(new BorderLayout());
    // Create the button
    JPanel buttonPanel = new JPanel();
    panicButton = new JButton("Create and Send New NodeTrustPolicy");
    buttonPanel.add(panicButton);

    // Create the JComboBox
    JPanel trustPanel = new JPanel(new GridBagLayout());
    JLabel categoryLabel = new JLabel("Choose a Trust Category:");
    categoryCB = new JComboBox();
    categoryCB.addItem("Society");
    //categoryCB.addItem("Host");
    //categoryCB.addItem("Subnet");

    // Create JRadioButtons and Group
    JLabel trustlevelLabel = new JLabel("Choose a Trust Level:");
    trustlevelBG = new ButtonGroup();
    trustNoOne = new JRadioButton("0 - Compromised");
    trustNoOne.setSelected(true);
    trustFew = new JRadioButton("2 - Suspect");
    trustMedium = new JRadioButton("5 - Normal");
    trustEveryone = new JRadioButton("10 - Carefree");
    trustlevelBG.add(trustNoOne);
    trustlevelBG.add(trustFew);
    trustlevelBG.add(trustMedium);
    trustlevelBG.add(trustEveryone);

    // Register a listener for the check box
    PanicButtonListener myPanicListener = new PanicButtonListener();
    panicButton.addActionListener(myPanicListener);
    panicButton.setEnabled(true);

    trustPanel.add(categoryLabel,
                   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 0, 5, 5), 0, 0));
    trustPanel.add(categoryCB,
                   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 0, 5, 5), 0, 0));
    trustPanel.add(trustlevelLabel,
                   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 0, 5, 5), 0, 0));
    trustPanel.add(trustNoOne,
                   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 20, 5, 5), 0, 0));
    trustPanel.add(trustFew,
                   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 20, 5, 5), 0, 0));
    trustPanel.add(trustMedium,
                   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 20, 5, 5), 0, 0));
    trustPanel.add(trustEveryone,
                   new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                          GridBagConstraints.WEST,
                                          GridBagConstraints.HORIZONTAL,
                                          new Insets(10, 20, 5, 5), 0, 0));
    panel.add(buttonPanel, BorderLayout.SOUTH);
    panel.add(trustPanel, BorderLayout.CENTER);
    frame.setContentPane(panel);
    frame.pack();
    frame.setVisible(true);
  }
    
  /** An ActionListener that listens to the GLS buttons. */
  class PanicButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      int trustlevel = 33;
      String category = (String) categoryCB.getSelectedItem();
      if (trustNoOne.isSelected()) {
        trustlevel = 0;
      } else if (trustFew.isSelected()) {
        trustlevel = 2;
      } else if (trustMedium.isSelected()) {
        trustlevel = 5;
      } else if (trustEveryone.isSelected()) {
        trustlevel = 10;
      }
        
      //System.out.println("catgorycb: "+category+
      //                   " trustlevel: "+trustlevel);
      sendNodeTrustPolicy(category, trustlevel);
    }
  }

  /** 
   * Do nothing
   */
  public void execute() {}

  public void sendNodeTrustPolicy(String category, int trustlevel) {
    // for now assume that pushing the button means to create a
    // society wide trust policy of '0' (trust no one level)
    NodeTrustPolicy trustpolicy = 
      (NodeTrustPolicy)domainService.getFactory().newPolicy(NodeTrustPolicy.class.getName());
    trustpolicy.setTrustCategory(category);
    trustpolicy.setTrustLevel(trustlevel);
    //create a message to contain the trust policy
    MulticastMessageAddress dest = 
      MulticastMessageAddress.getMulticastMessageAddress(org.cougaar.core.node.NodePolicyWatcher.class);
    PolicyMulticastMessage policymsg = 
      new PolicyMulticastMessage(getMessageAddress(), dest, trustpolicy);
    messageTransService.sendMessage(policymsg);
    System.out.println("\n PanicButtonPlugin just sent msg: "+policymsg);
  }

    
  //MessageTransportClient stuff
  public void receiveMessage(Message message) {
    // I don't want any message for now.
    System.err.println("\n"+this+": Received unhandled Message: "+message);
  }

  public MessageAddress getMessageAddress() {
    MessageAddress myma = MessageAddress.getMessageAddress("PanicButtonPlugin");
    return myma;
  }
    
} // end of PanicButtonPlugin.java
