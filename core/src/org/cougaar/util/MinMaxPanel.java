/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
;
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
