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

/** Simple entry for RangeRuleParameters : 
    holds int min/max range and a value 
**/
public class RangeRuleParameterEntry implements java.io.Serializable {

    public RangeRuleParameterEntry
	(String value, int range_min, int range_max) 
    {
	my_value = value; 
	my_range_min = range_min; 
	my_range_max = range_max;
    }

    public String getValue() { return my_value; }
    public int getRangeMin() { return my_range_min; }    
    public int getRangeMax() { return my_range_max; }    

    public String toString() { 
	return "[" + my_value + "/" + my_range_min + "-" + 
	    my_range_max + "]"; 
    }

    private String my_value;
    private int my_range_min;
    private int my_range_max;
}
