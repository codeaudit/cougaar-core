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

import java.awt.Font;

import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * Extends DefaultMetalTheme to specify fonts appropriate for the demo
 * UI displays.
 *
 * @see javax.swing.plaf.metal.DefaultMetalTheme
 */
public class DemoMetalTheme extends DefaultMetalTheme {
  
  private final FontUIResource demoControlFont = 
    new FontUIResource("Dialog", Font.BOLD, 18); 

  private final FontUIResource demoSystemFont =  
    new FontUIResource("Dialog", Font.PLAIN, 18); 

  private final FontUIResource demoUserFont =  
    new FontUIResource("Dialog", Font.PLAIN, 18); 

  private final FontUIResource demoSmallFont = 
    new FontUIResource("Dialog", Font.PLAIN, 14); 

  public FontUIResource getControlTextFont() { return demoControlFont;} 
  public FontUIResource getSystemTextFont() { return demoSystemFont;} 
  public FontUIResource getUserTextFont() { return demoUserFont;} 
  public FontUIResource getMenuTextFont() { return demoControlFont;} 
  public FontUIResource getWindowTitleFont() { return demoControlFont;} 
  public FontUIResource getSubTextFont() { return demoSmallFont;} 
} 

