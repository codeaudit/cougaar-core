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

