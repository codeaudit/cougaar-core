package org.cougaar.core.adaptivity;

/**
 * Defines boolean operator constants.
 */
public class Operator {
    protected String op;
    protected Operator(String op) {
        this.op = op;
    }
    public int hashCode() {
        return op.hashCode();
    }
    public boolean equals(Object o) {
        return this == o;
    }
    public String toString() {
        return op;
    }
}

















