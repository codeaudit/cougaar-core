package org.cougaar.core.adaptivity;
import java.util.*;
/** 
 * A knob and its value 
 */
import org.cougaar.planning.ldm.policy.RuleParameter;

public class OperatingMode extends OMSMBase {
  private List listeners = new ArrayList(1);

  public OperatingMode(String name, OMSMValueList allowedValues) {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }
  public OperatingMode(String name, OMSMValueList allowedValues, Comparable value) {
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
