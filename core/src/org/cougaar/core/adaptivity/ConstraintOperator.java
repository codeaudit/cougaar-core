/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;


/** 
 * Defines constants for operators used for describing and interpreting
 * relations between policy components.
 */
public class ConstraintOperator extends Operator {
    private ConstraintOperator(String op) {
        super(op, 2, 4, 4);
    }

    /* equality and inequalities */
    public static final ConstraintOperator GREATERTHAN = new ConstraintOperator(">");
    public static final ConstraintOperator GREATERTHANOREQUAL = new ConstraintOperator(">=");
    public static final ConstraintOperator LESSTHAN = new ConstraintOperator("<");
    public static final ConstraintOperator LESSTHANOREQUAL = new ConstraintOperator("<=");
    public static final ConstraintOperator EQUAL = new ConstraintOperator("==");
    public static final ConstraintOperator ASSIGN = new ConstraintOperator("=");
    public static final ConstraintOperator NOTEQUAL = new ConstraintOperator("!=");
    
    /** set operators */
    
    public static final ConstraintOperator IN = new ConstraintOperator("IN");
    public static final ConstraintOperator NOTIN = new ConstraintOperator("NOT IN");
    /* What else ?*/
}
