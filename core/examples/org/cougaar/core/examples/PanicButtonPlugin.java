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
import java.awt.event.*;
import java.awt.LayoutManager;
import java.awt.BorderLayout;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MulticastMessageAddress;
import org.cougaar.core.society.NodeTrustPolicy;
import org.cougaar.core.society.PolicyMulticastMessage;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

public class PanicButtonPlugin
  extends ComponentPlugin implements MessageTransportClient
{
  /** frame for 1-button UI **/
  private JFrame frame;    
  JLabel panicLabel;
  protected JButton panicButton;
  
  private MessageTransportService messageTransService = null;
    
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

  }

  private void createGUI() {
    frame = new JFrame("PanicButtonPlugin");
    //          JPanel panel = new JPanel((LayoutManager) null);
    
    JPanel panel = new JPanel(new BorderLayout());
    // Create the button
    panicButton = new JButton("Panic");
    panicLabel = new JLabel("Press to send Node Trust policies.");
    panicLabel.setHorizontalAlignment(JLabel.RIGHT);

    // Register a listener for the check box
    PanicButtonListener myPanicListener = new PanicButtonListener();
    panicButton.addActionListener(myPanicListener);
    panicButton.setEnabled(true);
        
    panel.add(panicButton, BorderLayout.WEST);
    panel.add(panicLabel, BorderLayout.EAST);
    frame.setContentPane(panel);
    frame.pack();
    frame.setVisible(true);
  }
    
  /** An ActionListener that listens to the GLS buttons. */
  class PanicButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      sendNodeTrustPolicy();
    }
  }

  /** 
   * Do nothing
   */
  public void execute() {}

  public void sendNodeTrustPolicy() {
    // for now assume that pushing the button means to create a
    // society wide trust policy of '0' (trust no one level)
    int trust = 0;
    NodeTrustPolicy trustpolicy = 
      new NodeTrustPolicy(NodeTrustPolicy.SOCIETY, 0, null);
    //create a message to contain the trust policy
 //    PolicyMulticastMessage policymsg = 
//       new PolicyMulticastMessage(getMessageAddress(), multicastaddress, trustpolicy);
//     messageTransService.sendMessage(policymsg);
  }

    
  //MessageTransportClient stuff
  public void receiveMessage(Message message) {
    // I don't want any message for now.
    System.err.println("\n"+this+": Received unhandled Message: "+message);
  }

  public MessageAddress getMessageAddress() {
    MessageAddress myma = new MessageAddress("PanicButtonPlugin");
    return myma;
  }
    
} // end of PanicButtonPlugIn.java
