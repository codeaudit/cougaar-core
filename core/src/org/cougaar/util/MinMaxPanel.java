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

package org.cougaar.util;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public abstract class MinMaxPanel extends JPanel {
  private JCheckBox enable = new JCheckBox();
  private JProgressBar progress = new JProgressBar() {
      public Dimension getPreferredSize() {
        return new Dimension(100, super.getPreferredSize().height);
      }
    };

  private Spinner minSpinner = new Spinner(0, 1000, 1000);
  private Spinner maxSpinner = new Spinner(0, 1000, 1000);

  private ActionListener minListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      int min = minSpinner.getValue();
      int max = maxSpinner.getValue();
      if (min > max) {
        maxSpinner.setValue(min);
      }
      newMin(min);
    }
  };

  private ActionListener maxListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      int min = minSpinner.getValue();
      int max = maxSpinner.getValue();
      if (min > max) {
        minSpinner.setValue(max);
      }
      newMax(max);
    }
  };

  private ActionListener enableListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      newEnable(enable.isSelected());
    }
  };

  public MinMaxPanel() {
    super(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = gbc.HORIZONTAL;
    gbc.weightx = 1.0;
    add(enable, gbc);
    gbc.fill = gbc.NONE;
    gbc.weightx = 0.0;
    add(minSpinner, gbc);
    add(maxSpinner, gbc);
    add(progress, gbc);
    minSpinner.addActionListener(minListener);
    maxSpinner.addActionListener(maxListener);
    enable.addActionListener(enableListener);
    progress.setStringPainted(true);
    progress.setBorder(BorderFactory.createLoweredBevelBorder());
  }

  public void setColumns(int cols) {
    minSpinner.setColumns(cols);
    maxSpinner.setColumns(cols);
  }

  public void setMin(int min) {
    minSpinner.setValue(min);
  }

  public void setMax(int max) {
    maxSpinner.setValue(max);
  }

  public void setProgressMax(int max) {
    progress.setMaximum(max);
  }

  public void setProgressValue(float completed) {
    int max = progress.getMaximum();
    progress.setValue((int) (max * completed));
    progress.setString(((int) (max * (1.0f - completed))) + "");
  }

  public void setEnabled(boolean b) {
    enable.setSelected(b);
  }

  public boolean isEnabled() {
    return enable.isSelected();
  }

  public void setText(String labelText) {
    enable.setText(labelText);
  }

  protected abstract void newMin(int min);
  protected abstract void newMax(int max);
  protected abstract void newEnable(boolean enable);
}
