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

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

/**
 * ThemeFactory - Convenience class for establishing common look and feel
 * for the UI. Also defines a common set of colors for use in the UI.
 *
 * Can switch between font sizes by changing myCurrentMetalTheme and
 * recompiling. Top level demo UI - applet or main - should call
 * establishMetalTheme()
 *
 * @see javax.swing.plaf.metal.MetalTheme
 */
public class ThemeFactory {
  static private final Color myCougaarRed = new Color(255, 75, 75);
  static private final Color myCougaarYellow = new Color(250, 250, 25);
  static private final Color myCougaarGreen = new Color(50, 205, 50);

  static public final String COUGAAR_RED_KEY = "CougaarRed";
  static public final String COUGAAR_YELLOW_KEY = "CougaarYellow";
  static public final String COUGAAR_GREEN_KEY = "CougaarGreen";

  /**
   * Use for demo
   */
  static final MetalTheme myCurrentMetalTheme = new DemoMetalTheme();

  /**
   * Use for normal size fonts
   */
  //static final MetalTheme myCurrentMetalTheme = new DefaultMetalTheme();

  /**
   * getMetalTheme - returns current MetalTheme (determines default
   * colors and fonts.
   *
   * @return MetalTheme - current MetalTheme
   */
  static public MetalTheme getMetalTheme() {
    return myCurrentMetalTheme;
  }

  
  /**
   * establishMetalTheme - sets the look and feel. Involves setting
   * the current theme for MetalLookAndFeel and then installing 
   * MetalLookAndFeel as the UIManager default.
   *
   * @see javax.swing.plaf.metal.MetalLookAndFeel
   * @see javax.swing.UIManager#setLookAndFeel
   */
  static public void establishMetalTheme() {
    try {
      MetalLookAndFeel.setCurrentTheme(myCurrentMetalTheme);

      UIManager.setLookAndFeel(new MetalLookAndFeel());

      UIDefaults defaults = UIManager.getDefaults();
      defaults.put(COUGAAR_RED_KEY, getCougaarRed());
      defaults.put(COUGAAR_YELLOW_KEY, getCougaarYellow());
      defaults.put(COUGAAR_GREEN_KEY, getCougaarGreen());

      // BOZO for sun - List font is not surfaced in MetalLookAndFeel
      defaults.put("List.font", myCurrentMetalTheme.getUserTextFont());
    } catch (UnsupportedLookAndFeelException ulafe) {
      System.out.println("ThemeFactory.establishMetalTheme() - exception " +
                         ulafe);
      ulafe.printStackTrace();
    }
  }

  /**
   * getCougaarRed - returns the canonical COUGAAR red
   *
   * @return Color - COUGAAR red.
   */
  static public Color getCougaarRed() {
    return myCougaarRed;
  }

  /**
   * getCougaarYellow - returns the canonical COUGAAR yellow
   *
   * @return Color - COUGAAR yellow
   */
  static public Color getCougaarYellow() {
    return myCougaarYellow;
  }

  /**
   * getCougaarGreen - returns the canonical COUGAAR green
   *
   * @return Color - COUGAAR green.
   */
  static public Color getCougaarGreen() {
    return myCougaarGreen;
  }

}






