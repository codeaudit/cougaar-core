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

package org.cougaar.core.blackboard;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * An extension of IllegalArgumentException for capturing additional
 * information about an apparently erroneous publish operation. It
 * records certain facts to assist the Blackboard in providing a more
 * detailed explanation of the error including the stack of a previous
 * operation with which the current publish operation seems to be in
 * conflict.
 **/
public class PublishException extends IllegalArgumentException {
    public PublishStack priorStack;
    public boolean priorStackUnavailable;
    private String specialMessage = null;
    public PublishException(String msg) {
        super(msg);
        this.priorStack = null;
    }
    public PublishException(String msg, PublishStack priorStack, boolean priorStackUnavailable) {
        super(msg);
        this.priorStack = priorStack;
        this.priorStackUnavailable = priorStackUnavailable;
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
