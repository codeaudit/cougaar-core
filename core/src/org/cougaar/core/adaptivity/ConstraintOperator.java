package org.cougaar.core.adaptivity;

/** 
 * Defines constants for operators used for describing and interpreting
 * relations between policy components.
 */
public class ConstraintOperator extends Operator {
    private ConstraintOperator(String op) {
        super(op);
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
