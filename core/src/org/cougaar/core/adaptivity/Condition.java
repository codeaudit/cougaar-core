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

/** 
 * A Condition holds a value that the {@link AdaptivityEngine} or
 * {@link PolicyManager PolicyManager} uses for selecting
 * {@link Play Play}s or
 * {@link OperatingModePolicy OperatingModePolicy}s.
 * Conditions include the measurements made by sensors, external
 * conditions distributed throughout portions of the society and
 * inputs from higher level adaptivity engines. This interface does
 * not define a setValue method because establishing the current value
 * is the responsibility of the owner of the Condition.
 **/
public interface Condition {

    /**
     * Gets the (distinct) name of this Condition. The names of all
     * the Condition on a particular blackboard must be distinct. This
     * can be achieved by establishing naming conventions such has
     * including the class name of the plugin or other component that
     * created the Condition in the name. Where the same component
     * class may be instantiated multiple times, the multiple
     * instances may already have some sor of name that can be used in
     * addition to the class name. It is the responsibility of the
     * component designer to insure that Condition names are not
     * ambiguous.
     * @return the name of this Condition
     **/
    String getName();

    /**
     * Get the list of allowed value ranges for this OperatingMode.
     * Attempts to set the value outside these ranges will fail.
     * @return a list of allowed value ranges
     **/
    OMCRangeList getAllowedValues();

    /**
     * Get the current value of this OperatingMode. This value must
     * never be outside the allowed value ranges.
     * @return the current value of this OperatingMode
     **/
    Comparable getValue();
}
