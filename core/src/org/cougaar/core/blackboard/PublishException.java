/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.blackboard;


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
