package org.cougaar.core.adaptivity;

/**
 * Defines boolean operator constants.
 */
public class BooleanOperator extends Operator {
    private BooleanOperator(String op) {
        super(op);
    }
    public static final BooleanOperator AND = new BooleanOperator("&");
    public static final BooleanOperator OR = new BooleanOperator("|");
    public static final BooleanOperator NOT = new BooleanOperator("!");
    public static final BooleanOperator TRUE = new BooleanOperator("TRUE");
    public static final BooleanOperator FALSE = new BooleanOperator("FALSE");
}

















