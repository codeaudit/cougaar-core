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
public class Operator implements java.io.Serializable {
    public static final int MAXP = 10;
    protected String op;
    protected int lp;
    protected int rp;
    protected int nOperands;

    protected Operator(String op, int nOps, int lp, int rp) {
        this.op = op;
        this.lp = lp;
        this.rp = rp;
        nOperands = nOps;
    }

    public int getLP() {
        return lp;
    }
    public int getRP() {
        return rp;
    }
    public int getOperandCount() {
        return nOperands;
    }
    public int hashCode() {
        return op.hashCode();
    }
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Operator) {
            Operator that = (Operator) o;
            return (this.op.equals(that.op) &&
                    this.nOperands == that.nOperands);
        }
        return false;
    }
    public String toString() {
        return op;
    }
}

















