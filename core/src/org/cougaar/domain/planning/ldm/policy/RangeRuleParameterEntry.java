/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.policy;

import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;

/** Simple entry for RangeRuleParameters : 
    holds int min/max range and a value 
**/
public class RangeRuleParameterEntry implements SelfPrinter, java.io.Serializable {
  
  private Object my_value;
  private int my_min;
  private int my_max;

  public RangeRuleParameterEntry(Object value, int min, int max)  {
    my_value = value; 
    my_min = min; 
    my_max = max;
  }

  public RangeRuleParameterEntry() {
  }

  
  public int getMin() {
    return my_min;
  }
  public int getRangeMin() { 
    return getMin();
  }
  public void setMin(int min) {
    my_min = min;
  }
  
  public Object getValue() { return my_value; }
  public void setValue(Object value) {
    my_value = value;
  }

  public void setMax(int max) {
    my_max = max;
  }
  public int getMax() {
    return my_max;
  }
  public int getRangeMax() { 
    return getMax();
  }
  
  public String toString() { 
    return "[" + my_value + "/" + my_min + "-" + 
      my_max + "]"; 
  }
  public void printContent(AsciiPrinter pr) {
    pr.print(my_min, "Min");
    pr.print(my_max, "Max");
    pr.print(my_value, "Value");
  }

  
}






