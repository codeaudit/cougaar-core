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
 * Defines constants for operators used for describing and interpreting
 * relations between policy components.
 */
public class ArithmeticOperator extends Operator {
    private ArithmeticOperator(String op, int nOps, int lp, int rp) {
        super(op, nOps, lp, rp);
    }

    /* Arithmetic ops */
    public static final ArithmeticOperator ADD = new ArithmeticOperator("+", 2, 2, 2);
    public static final ArithmeticOperator SUBTRACT = new ArithmeticOperator("-", 2, 2, 2);
    public static final ArithmeticOperator NEGATE = new ArithmeticOperator("-", 1, 3, 0);
    public static final ArithmeticOperator MULTIPLY = new ArithmeticOperator("*", 2, 1, 1);
    public static final ArithmeticOperator DIVIDE = new ArithmeticOperator("/", 2, 1, 1);
}
