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

package org.cougaar.core.mobility.ldm;

import java.io.Serializable;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A request that an agent be moved from its current node 
 * to a different node.
 * <p>
 * This has been replaced by the "AgentControl" API, but 
 * there's no rush to switch to the new API.
 *
 * @see AgentControl
 */
public interface MoveAgent extends UniqueObject {

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Address of the agent that requested the move.
   */
  MessageAddress getSource();

  /**
   * Get the move ticket.
   * <p>
   * If the ticket's "getMobileAgent()" is null then
   * the requesting agent will be moved (a "rover" agent).
   * 
   * @see Ticket
   */
  Ticket getTicket();

  // maybe add "abort" here

  /**
   * Get the current status to the request.
   */
  Status getStatus();

  /**
   * For infrastructure use only!.
   * Allows the infrastructure to set the status.
   * Only valid if (local-agent == req.agentToMove)
   */
  void setStatus(Status status);


  /**
   * Immutable class that represents the dynamic status 
   * of the move request.
   */
  final class Status implements Serializable {

    /**
     * Status codes:
     */
    public static final int OKAY = 200;
    public static final int FAILURE = 500;
    // add more status-codes here

    private final int code;
    private final String message;
    private final Throwable throwable;

    public Status(int code, String message) {
      this(code, message, null);
    }

    public Status(int code, String message, Throwable throwable) {
      this.code = code;
      this.message = message;
      this.throwable = throwable;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public Throwable getThrowable() {
      return throwable;
    }

    public String getCodeAsString() {
      switch (code) {
        case OKAY: return "Okay ("+OKAY+")";
        case FAILURE: return "Failure ("+FAILURE+")";
        default: return "Unknown ("+code+")";
      }
    }

    public int hashCode() {
      int hc = code;
      if (message != null) hc += message.hashCode();
      return hc;
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Status)) {
        return false;
      } else {
        Status s = (Status) o;
        return 
          ((code == s.code) &&
           ((message == null) ? 
            (s.message == null) :
            (message.equals(s.message))) &&
           ((throwable == null) ? 
            (s.throwable == null) :
            (throwable.equals(s.throwable))));
      }
    }
    public String toString() {
      return 
        (getCodeAsString()+" "+
         message+
         ((throwable != null) ? (throwable.getMessage()) : ""));
    }
  }

}
