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

/** Simple entry for KeyRuleParameters : holds a key and a value **/
public class KeyRuleParameterEntry implements java.io.Serializable {

    public KeyRuleParameterEntry
	(String key, String value)
    {
	my_key = key;
	my_value = value; 
    }

    public String getValue() { return my_value; }
    public String getKey() { return my_key; }

    public String toString() { 
	return "[" + my_value + "/" + my_key + "]"; 
    }

    private String my_value;
    private String my_key;
}
