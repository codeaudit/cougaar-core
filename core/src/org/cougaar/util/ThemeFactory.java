/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
  static private final Color myALPRed = new Color(255, 75, 75);
  static private final Color myALPYellow = new Color(250, 250, 25);
  static private final Color myALPGreen = new Color(50, 205, 50);

  static public final String ALP_RED_KEY = "ALPRed";
  static public final String ALP_YELLOW_KEY = "ALPYellow";
  static public final String ALP_GREEN_KEY = "ALPGreen";

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
   * @see javax.swing.UIManager.setLookAndFeel
   */
  static public void establishMetalTheme() {
    try {
      MetalLookAndFeel.setCurrentTheme(myCurrentMetalTheme);

      UIManager.setLookAndFeel(new MetalLookAndFeel());

      UIDefaults defaults = UIManager.getDefaults();
      defaults.put(ALP_RED_KEY, getALPRed());
      defaults.put(ALP_YELLOW_KEY, getALPYellow());
      defaults.put(ALP_GREEN_KEY, getALPGreen());

      // BOZO for sun - List font is not surfaced in MetalLookAndFeel
      defaults.put("List.font", myCurrentMetalTheme.getUserTextFont());
    } catch (UnsupportedLookAndFeelException ulafe) {
      System.out.println("ThemeFactory.establishMetalTheme() - exception " +
                         ulafe);
      ulafe.printStackTrace();
    }
  }

  /**
   * getALPRed - returns the canonical ALP red
   *
   * @return Color - ALP red.
   */
  static public Color getALPRed() {
    return myALPRed;
  }

  /**
   * getALPYellow - returns the canonical ALP yellow
   *
   * @return Color - ALP yellow
   */
  static public Color getALPYellow() {
    return myALPYellow;
  }

  /**
   * getALPGreen - returns the canonical ALP green
   *
   * @return Color - ALP green.
   */
  static public Color getALPGreen() {
    return myALPGreen;
  }

}






