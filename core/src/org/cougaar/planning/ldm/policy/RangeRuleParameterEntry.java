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


package org.cougaar.planning.ldm.policy;

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






