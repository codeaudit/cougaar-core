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

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import javax.swing.*;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;

public class AlertCreatorPlugin extends org.cougaar.core.plugin.SimplePlugin 
{

  // This plugin doesn't subscribe, but needs to set up a UI
  // for the creation of alerts
  public void setupSubscriptions()
  {
    //    System.out.println("AlertCreatorPlugin.setupSubscriptions...");

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	generateAlert();
      }
    };
    createGUI("Create Alert", "Alert", listener);
  }

  private static int counter = 1;

  private static String []paramAction = 
  {"infomessage.html", "cat", "dog", "parrot"};

  private static String []paramDescription =
  {"View Info Messages", "A Cat", "A Dog", "A Parrot"}; 
  
  private void generateAlert()
  {
    openTransaction();
    NewAlert alert = theLDMF.newAlert();
    alert.setAlertText("AlertText-" + counter);

    int numParams = (counter % 4);

    AlertParameter []params = new AlertParameter[numParams];
    for (int i = 0; i < numParams; i++) {
      NewAlertParameter param = theLDMF.newAlertParameter();
      param.setParameter(paramAction[i]);  
      param.setDescription(paramDescription[i]);
      params[i] = param;
    }
    alert.setAlertParameters(params);

    alert.setAcknowledged(false);

    alert.setSeverity(counter);

    alert.setOperatorResponseRequired((numParams > 0));

    System.out.println("Publishing new alert " + alert.getAlertText());

    counter++;
    
    publishAdd(alert);
    closeTransactionDontReset();
  }

  public void execute()
  {
    //    System.out.println("AlertCreatorPlugin.execute...");
  }

  /**
   * Create a simple free-floating GUI button with a label
   */
  private static void createGUI(String button_label, String frame_label, 
				ActionListener listener) 
  {
    JFrame frame = new JFrame(frame_label);
    frame.getContentPane().setLayout(new FlowLayout());
    JPanel panel = new JPanel();
    // Create the button
    JButton button = new JButton(button_label);

    // Register a listener for the button
    button.addActionListener(listener);
    panel.add(button);
    frame.getContentPane().add("Center", panel);
    frame.pack();
    frame.setVisible(true);
  }



}
