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




