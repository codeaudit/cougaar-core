/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
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

package org.cougaar.core.mobility.ldm;

import java.io.Serializable;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Request that an agent be moved.
 * <p>
 * Use the MobilityFactory, provided by the "mobility" domain,
 * to create a new move request.
 * <p>
 * The ticket specifies the move details: which agent to
 * move, which node it should move to, etc.
 * <p>
 * This subsumes and replaces the prior MoveAgent APIs, since 
 * this API removes the capability for direct node contact
 * and simplifies status checking.  The existing MoveAgent
 * APIs still work, but are now subclasses of AgentRemove.
 */
public interface AgentRemove extends AgentControl {
  
  /**
   * Status codes:
   */
  int NONE = 1;
  int REMOVED = 2;
  int ALREADY_REMOVED = 3;
  int FAILURE = 4;
  
  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Get the optional UID of the object that "owns" this 
   * move request, which is used by the infrastructure
   * to mark redirected move requests.
   */
  UID getOwnerUID();

  /**
   * Get the agent that created this move request.
   */
  MessageAddress getSource();

  /**
   * Get the agent that should perform the move.
   * <p>
   * Typically this is either the mobile agent itself or
   * the node containing the mobile agent.  This must
   * agree with the ticket.
   * <p>
   * Note that the behavior is different depending upon
   * the target.  If the node is specified then the 
   * request is not passed through the agent.
   */
  MessageAddress getTarget();

  /**
   * The addresses specified in the ticket are not 
   * null.
   */
  RemoveTicket getRemoveTicket();

  /**
   * Get the move status code, which is one of the above
   * "*_STATUS" constants.
   * <p>
   * Initially the status is "NO_STATUS".
   */
  int getStatusCode();

  /**
   * Get a string representation of the status.
   */
  String getStatusCodeAsString();

  /**
   * If (getStatusCode() == FAILED_STATUS), this is
   * the failure exception.
   */
  Throwable getFailureStackTrace();

  /**
   * For infrastructure use only!  Set the status.
   */
  void setStatus(int statusCode, Throwable stack);
  
   /**
   * Immutable class that represents the dynamic status 
   * of the move request.
   */
  final class Status implements Serializable {

    /**
     * Status codes:
     */
    public static final int NONE = 100;
    public static final int CREATED = 200;
    public static final int ALREADY_EXISTS = 300;
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
      case CREATED: return "Created ("+CREATED+")";
      case FAILURE: return "Failure ("+FAILURE+")";
      case NONE: return "None ("+NONE+")";
      case ALREADY_EXISTS: return "Already Exists ("+ALREADY_EXISTS+")";
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
