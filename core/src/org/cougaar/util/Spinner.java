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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.Polygon;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * A control with arrows to cycle through a set of values and an entry
 * field for direct entry of the value.
 **/
public class Spinner extends JPanel {
  /**
   * A trivial Document that can hold numeric strings. Attempts to
   * insert non-numeric information are thwarted. Additional accessor
   * and mutator provide direct access to the numeric value.
   **/

  /** An icon for the down arrow button. **/
  static Icon downIcon;

  /** An icon for the up arrow button. **/
  static Icon upIcon;

  static RenderingHints antialiasingHint =
    new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

  static class ArrowIcon implements Icon {
    private static final int mid = 5;
    private boolean isUp = true;
    public ArrowIcon(boolean isUp) {
      this.isUp = isUp;
    }
    public int getIconHeight() {
      return mid + 1;
    }
    public int getIconWidth() {
      return mid*2+1;
    }
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2d = ((Graphics2D) g);
      Color base = g.getColor();
      g2d.addRenderingHints(antialiasingHint);
      g2d.setColor(Color.black);
      g2d.translate(x+0.5, y+0.5);
      if (isUp) {
        int[] xp = {0, mid, mid*2};
        int[] yp = {mid, 0, mid};
        g2d.fill(new Polygon(xp, yp, 3));
//          for (int i = 0; i <= mid; i++) {
//            g2d.drawLine(mid-i, i, mid+i, i);
//          }
      } else {
        int[] xp = {0, mid, mid*2};
        int[] yp = {0, mid, 0};
        g2d.fill(new Polygon(xp, yp, 3));
//          for (int i = 0; i <= mid; i++) {
//            g2d.drawLine(mid-i, mid-i, mid+i, mid-i);
//          }
      }
      g2d.translate(-x-0.5,-y-0.5);
      g.setColor(base);
    }
  }

  static Icon getUpIcon() {
    return new ArrowIcon(true);
  }

  static Icon getDownIcon() {
    return new ArrowIcon(false);
  }

  /**
   * A button displaying an arrow to adjust the value of the spinner.
   **/
  private class MyBasicArrowButton extends JComponent implements ActionListener, MouseListener {

    /** A timer to repeated increment the spinner while the mouse is pressed. **/
    private Timer repeatTimer = new Timer(60, this);

    /** The basic increment -- always plus or minus one. **/
    private int increment;

    /**
     * Indicates that the button is current pressed. Used to alter the
     * button's appearance.
     **/
    private boolean pressed = false;

    /**
     * The number of times and increment has been performed for this
     * press of the mouse. Used to accelerate the increment.
     **/
    private int nIncrements = 0;

    /** The correct icon for this button -- upIcon or downIcon **/
    private Icon icon;

    /**
     * Create a button to increment by the given amount.
     * @param increment the amount to increment. Should be +/- one.
     **/
    public MyBasicArrowButton(int increment) {
      this.increment = increment;
      repeatTimer.setInitialDelay(300);  // default InitialDelay?
      this.addMouseListener(MyBasicArrowButton.this);
      if (increment < 0) {
        icon = getDownIcon();
      } else {
        icon = getUpIcon();
      }
    }

    /**
     * Get the preferred size of this button. The width is built
     * in. The height is always half the preferred height of the entry
     * field.
     * @return the preferred size of this button.
     **/
    public Dimension getPreferredSize() {
      return new Dimension(icon.getIconWidth() + 6, entry.getPreferredSize().height / 2);
    }

    /**
     * Handle a mouse press event. Increment the spinner and start the timer.
     * @param e the mouse event is not used.
     **/
    public void mousePressed(MouseEvent e) {
      doIncrement(increment);
      nIncrements = 1;
      repeatTimer.start();
      pressed = true;
      this.repaint();
    }

    /**
     * Handle a mouse release event. The timer is stopped.
     * @param e the mouse event is not used.
     **/
    public void mouseReleased(MouseEvent e) {
      repeatTimer.stop();
      pressed = false;
      this.repaint();
    }

    /**
     * Handle a mouse clicked event. Does nothing.
     **/
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Handle a mouse entered event. Does nothing.
     **/
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Handle a mouse exited event. Does nothing.
     **/
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Handle the action event from the repeat timer. Increment the
     * spinner by an amount that increases as time goes on.
     **/
    public void actionPerformed(ActionEvent e) {
      if (nIncrements > 200) {
        doIncrement(increment * 100);
      } else if (nIncrements > 50) {
        doIncrement(increment * 10);
      } else {
        doIncrement(increment);
      }
      nIncrements += 1;
    }

    /**
     * Paint this button. The button is a 3D rectangle with an arrow
     * icon in it.
     **/
    public void paintComponent(Graphics g) {
      Dimension size = this.getSize();
      g.setColor(this.getBackground());
      g.fill3DRect(0, 0, size.width, size.height, !pressed);
      if (icon != null) {
        icon.paintIcon(this, g,
                       (size.width - icon.getIconWidth()) / 2,
                       (size.height - icon.getIconHeight()) / 2);
      }
    }
  }

  /**
   * Increment the spinner by a given amount. The new value is limited
   * by the range values.
   * @param by the amount by which to increment the spinner.
   **/
  private void doIncrement(int by) {
    setValue(Math.min(maxValue, Math.max(minValue, getValue() + by)));
  }

  /** The Document in the entry field. **/
  private NumericDocument document = new NumericDocument();

  /** An entry field to display the spinner value and allow direct input. **/
  private JTextField entry = new JTextField(document, "0", 6);

  /** The arrow button for increasing the spinner value. **/
  private MyBasicArrowButton plusButton = new MyBasicArrowButton(1);

  /** The arrow button for decreasing the spinner value. **/
  private MyBasicArrowButton minusButton = new MyBasicArrowButton(-1);

  /** The minimum value that is permitted in the spinner. **/
  private int minValue;

  /** The maximum value that is permitted in the spinner. **/
  private int maxValue;

  /**
   * Create a new Spinner that allows values in a given range and
   * having a specific initial value.
   * @param min the minimum value allowed.
   * @param max the maximum value allowed.
   * @param value the initial value.
   **/
  public Spinner(int min, int max, int value) {
    super(new GridBagLayout());
    entry.setHorizontalAlignment(entry.RIGHT);
    entry.addFocusListener(new FocusListener() {
      public void focusLost(FocusEvent e) {
        doIncrement(0);
      }
      public void focusGained(FocusEvent e) {
        entry.selectAll();
      }
    });
    setRange(min, max);
    setValue(value);
    setColumns(Math.max(columnsNeeded(min), columnsNeeded(max)));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    gbc.weightx = 1.0;
    gbc.fill = gbc.HORIZONTAL;
    add(entry, gbc);
    gbc.gridx = 1;
    gbc.gridheight = 1;
    gbc.fill = gbc.NONE;
    gbc.weightx = 0.0;
    add(plusButton, gbc);
    gbc.gridy = 1;
    add(minusButton, gbc);
  }

  /**
   * Add an ActionListener to be notified if the value of this Spinner
   * changes.
   * @param l the listener to add.
   **/
  public synchronized void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  /**
   * Remove an ActionListener.
   * @param l the listener to remove.
   **/
  public synchronized void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }

  /**
   * Send an ActionEvent to listeners.
   **/
  protected void fireActionPerformed() {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, entry.getText());
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==ActionListener.class) {
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }          
    }
  }

  /**
   * Get the current value of this Spinner.
   * @return the current value of this Spinner.
   **/
  public int getValue() {
    return document.getValue();
  }

  /**
   * Set the value of this spinner.
   * @param newValue the new value.
   * @exception IllegalArgumentException if the new value is out of range.
   **/
  public void setValue(int newValue) {
    if (newValue < minValue || newValue > maxValue) {
      throw new IllegalArgumentException("newValue out or range");
    }
    document.setValue(newValue);
    fireActionPerformed();
  }

  public void setColumns(int columns) {
    entry.setColumns(columns);
  }

  public int getColumns() {
    return entry.getColumns();
  }

  private int columnsNeeded(int val) {
    if (val == Integer.MIN_VALUE) return 11; // Can't negate this
    int needed = 0;
    if (val < 0) {
      needed = 1;
      val = -val;
    }
    while (val >= 1) {
      needed++;
      val /= 10;
    }
    return needed;
  }

  /**
   * Change the allowed range of values. The current value is forced
   * into the specified range.
   * @param newMin the new minimum value (inclusive)
   * @param newMax the new maximum value (inclusive)
   * @exception IllegalArgumentException if the newMin exceeds newMax
   **/
  public void setRange(int newMin, int newMax) {
    if (newMin > newMax) {
      throw new IllegalArgumentException("newMin > newMax");
    }
    minValue = newMin;
    maxValue = newMax;
    if (newMin > getValue()) {
      setValue(newMin);
    }
    if (newMax < getValue()) {
      setValue(newMax);
    }
  }
}

    
  
