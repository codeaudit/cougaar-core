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

public class PropertyNameValue {
  public String name;
  public Object value;

  public PropertyNameValue(String name, Object value) {
    this.name = name;
    this.value = value;
  }

}
