/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.util;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import javax.swing.*;

import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;

public class AlertCreatorPlugIn extends org.cougaar.core.plugin.SimplePlugIn 
{

  // This plugin doesn't subscribe, but needs to set up a UI
  // for the creation of alerts
  public void setupSubscriptions()
  {
    //    System.out.println("AlertCreatorPlugIn.setupSubscriptions...");

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
    closeTransaction(false);
  }

  public void execute()
  {
    //    System.out.println("AlertCreatorPlugIn.execute...");
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
