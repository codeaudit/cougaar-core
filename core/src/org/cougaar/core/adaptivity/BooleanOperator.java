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
 * Defines boolean operator constants.
 */
public class BooleanOperator extends Operator {
    private BooleanOperator(String op, int nOps, int lp, int rp) {
        super(op, nOps, lp, rp);
    }
    public static final BooleanOperator AND = new BooleanOperator("&", 2, 5, 5);
    public static final BooleanOperator OR = new BooleanOperator("|", 2, 6, 6);
    public static final BooleanOperator NOT = new BooleanOperator("!", 1, 7, 4);
    public static final BooleanOperator TRUE = new BooleanOperator("TRUE", 0, 0, 0);
    public static final BooleanOperator FALSE = new BooleanOperator("FALSE", 0, 0, 0);
}

















