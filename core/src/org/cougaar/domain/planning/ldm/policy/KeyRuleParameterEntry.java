/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.policy;

import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;

/** Simple entry for KeyRuleParameters : holds a key and a value **/
public class KeyRuleParameterEntry implements java.io.Serializable, SelfPrinter {
  private String my_value;
  private String my_key;

  public KeyRuleParameterEntry(String key, String value) {
    my_key = key;
    my_value = value; 
  }
  
  public KeyRuleParameterEntry() {
  }

  public String getKey() { 
    return my_key; 
  }
 
  public void setKey(String key) {
    my_key = key;
  }

  public String getValue() { 
    return my_value; 
  }
 
  public void setValue(String value) {
    my_value = value;
  }
  
  public String toString() { 
    return "[" + my_value + "/" + my_key + "]"; 
  }
  
  public void printContent(AsciiPrinter pr) {
    pr.print(my_key, "Key");
    pr.print(my_value, "Value");
  }

  
}




