/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;
import java.util.*;
/** 
 * A knob and its value 
 */
import org.cougaar.planning.ldm.policy.RuleParameter;

public class OperatingModeImpl extends OMSMBase implements OperatingMode {
  private List listeners = new ArrayList(1);

  public OperatingModeImpl(String name, OMSMValueList allowedValues) {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }
  public OperatingModeImpl(String name, OMSMValueList allowedValues, Comparable value) {
    super(name, allowedValues, value);
  }
    
  public void addOperatingModeListener(OperatingModeListener l) {
    listeners.add(l);
  }

  public void removeOperatingModeListener(OperatingModeListener l) {
    listeners.remove(l);
  }

  public void setValue(Comparable newValue) {
    super.setValue(newValue);
  }

  protected void fireListeners(Comparable oldValue) {
    if (listeners == null) return; // No listeners yet
    for (Iterator j = listeners.iterator(); j.hasNext(); ) {
      OperatingModeListener l = (OperatingModeListener) j.next();
      try {
        l.operatingModeChanged(this, oldValue);
      } catch (Exception e) {
//          logger.error("Exception in listener", e);
      }
    }
  }
}
