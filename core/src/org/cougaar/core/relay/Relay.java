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

package org.cougaar.core.relay;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;
import java.io.Serializable;
import java.util.Set;
import java.util.Collection;
import org.cougaar.core.agent.ClusterIdentifier;

public interface Relay extends UniqueObject {
  interface Source extends Relay {
    /**
     * Get all the addresses of the target agents to which this
     * Relay should be sent.
     **/
    Set getTargets();

    /**
     * Get an object representing the value of this Relay
     * suitable for transmission.
     **/
    Content getContent();

    /**
     * Get an object representing a response from the target with the
     * specified address.
     **/
    Response getResponse(ClusterIdentifier targetAddress);

    /**
     * Set the response that was sent from a target. For LP use only.
     **/
    void setResponse(ClusterIdentifier targetAddress, Relay.Response resp);
  }

  interface Target extends Relay {
    /**
     * Get the ClusterIdentifier of the Agent holding the Source copy of
     * this Relay
     **/
    ClusterIdentifier getSource();

    /**
     * Get the current Response for this target. Null indicates that
     * this target has no response.
     **/
    Response getResponse();

    /**
     * Update with new content.
     * @return true if the update changed the Relay. The LP should
     * publishChange the Relay.
     **/
    boolean updateContent(Content newContent, Token token);
  }

  interface Content extends Serializable {
    /**
     * Create an object to be published to the target's blackboard as
     * described by this Content. Often this will be the Content
     * itself for those implementations whose getContent() method
     * returns itself. Other implementations may create an instance of
     * a different class that is just sufficient to implement the
     * Relay.Target interface.
     * @param uid the UID of the target instance. In some cases this
     * may be redundant with information in this Content, but simple
     * Content implementations need not carry the UID since it is also
     * passed in the Directive.
     **/
    Relay.Target create(UID uid, ClusterIdentifier source, Token token);
  }

  interface Response extends Serializable {
    // Response is arbitrary
  }

  class Token {
    Token() {}                  // Only package access allowed.
  }
}
