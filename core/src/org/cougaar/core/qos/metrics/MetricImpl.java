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

package org.cougaar.core.qos.metrics;

public class MetricImpl implements Metric
{
    private Object rawValue;
    private double credibility;
    private String units;
    private String provenance;

    public MetricImpl(double raw, 
		      double credibility, 
		      String units, 
		      String provenance) 
    {
	this(new Double(raw), credibility, units, provenance);
    }

    public MetricImpl(Object raw, 
		      double credibility, 
		      String units, 
		      String provenance) 
    {
	this.rawValue = raw;
	this.credibility = credibility;
	this.units = units;
	this.provenance = provenance;
    }

    public String toString() {
	return "<" +rawValue+ ":" +credibility+ ">";
    }
    
    public String stringValue() {
	return rawValue.toString();
    }

    public byte byteValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).byteValue());
	} else {
	    return 0;
	}
    }

    public short shortValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).shortValue());
	} else {
	    return 0;
	}
    }

    public int intValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).intValue());
	} else {
	    return 0;
	}
    }

    public long longValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).longValue());
	} else {
	    return 0;
	}
    }

    public float floatValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).floatValue());
	} else {
	    return 0;
	}
    }

    public double doubleValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).doubleValue());
	} else {
	    return 0;
	}
    }

    public char charValue() {
	if (rawValue instanceof String)
	    return ((String) rawValue).charAt(0);
	else if (rawValue instanceof Character) 
	    return (((Character) rawValue).charValue());
	else
	    return '?';
    }

    public boolean booleanValue() {
	if (rawValue instanceof Boolean) 
	    return (((Boolean) rawValue).booleanValue());
	else
	    return false;
    }

    public Object getRawValue() {
	return rawValue;
    }

    public double getCredibility() {
	return credibility;
    }

    public String getUnits() {
	return units;
    }

    public String getProvenance() {
	return provenance;
    }

}
