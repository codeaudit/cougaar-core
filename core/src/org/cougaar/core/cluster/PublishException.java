/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

public class PublishException extends IllegalArgumentException {
    public PublishStack priorStack;
    private String specialMessage = null;
    public PublishException(String msg) {
        super(msg);
        this.priorStack = null;
    }
    public PublishException(String msg, PublishStack priorStack) {
        super(msg);
        this.priorStack = priorStack;
    }
    public String toString() {
        if (specialMessage != null) return specialMessage;
        return super.toString();
    }
    public synchronized void printStackTrace(String message) {
        specialMessage = message;
        super.printStackTrace();
        specialMessage = null;
    }
    public synchronized void printStackTrace() { 
        super.printStackTrace();
    }

    public synchronized void printStackTrace(java.io.PrintStream s) { 
        super.printStackTrace(s);
    }

    public synchronized void printStackTrace(java.io.PrintWriter s) { 
        super.printStackTrace(s);
    }
}
