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

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * This fixes bugs in JOptionPane.showOptionDialog and
 * JOptionPane.showInputDialog that sizes the dialog too small when
 * using larger demo fonts. It fixes the workaround for bug#4135218 by
 * dynamically returning a preferred size based on the current
 * preferred size instead of statically computing a preferred size.
 * The static technique doesn't track UI changes.
 **/

public class OptionPane extends JOptionPane {
    public OptionPane(Object message, int messageType, int optionType,
                      Icon icon, Object[] options, Object initialValue)
    {
        super(message, messageType, optionType, icon, options, initialValue);
    }

    private Dimension prevSize;
    private Dimension prevResult;

    public Dimension getPreferredSize() {
        Dimension sz = super.getPreferredSize();
        if (!sz.equals(prevSize)) {
            prevSize = sz;
            prevResult = new Dimension(sz.width + 5, sz.height + 2);
        }
        return prevResult;
    }

    public static int showOptionDialog(Component parentComponent,
                                       Object message,
                                       String title,
                                       int optionType,
                                       int messageType,
                                       Icon icon,
                                       Object[] options,
                                       Object initialValue)
    {
        JOptionPane pane = new OptionPane(message, messageType,
                                          optionType, icon,
                                          options, initialValue);
        pane.setInitialValue(initialValue);

        JDialog dialog = pane.createDialog(parentComponent, title);

        pane.selectInitialValue();

        dialog.show();

        Object selectedValue = pane.getValue();

        if (selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if(options == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    public static Object showInputDialog(Component parentComponent,
                                         Object message,
                                         String title,
                                         int messageType,
                                         Icon icon,
                                         Object[] selectionValues,
                                         Object initialSelectionValue)
    {
        JOptionPane pane = new OptionPane(message,
                                          messageType,
                                          JOptionPane.OK_CANCEL_OPTION,
                                          icon,
                                          null,
                                          null);

        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);

        JDialog dialog = pane.createDialog(parentComponent, title);

        pane.selectInitialValue();
        dialog.pack();              // This makes the difference!
        dialog.show();

        Object value = pane.getInputValue();

        if (value == JOptionPane.UNINITIALIZED_VALUE)
            return null;
        return value;
    }
}
