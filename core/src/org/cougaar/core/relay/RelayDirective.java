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
import org.cougaar.planning.ldm.plan.DirectiveImpl;
import java.io.Serializable;
import java.util.Set;
import java.util.Collection;
import org.cougaar.core.agent.ClusterIdentifier;

/**
 * Define Directives for Relay messages. Directives can add, change,
 * and remove a Relay.Target and send responses back to the
 * Relay.Source.
 **/
class RelayDirective extends DirectiveImpl {
  private UID uid;

  protected RelayDirective(UID uid) {
    this.uid = uid;
  }

  public UID getUID() {
    return uid;
  }

  static class Add extends RelayDirective {
    private Relay.Content content;

    public Add(UID uid, Relay.Content content) {
      super(uid);
      this.content = content;
    }
    public Relay.Content getContent() {
      return content;
    }
  }

  static class Change extends RelayDirective {
    private Relay.Content content;

    public Change(UID uid, Relay.Content content, Collection changes) {
      super(uid);
      this.content = content;
    }
    public Relay.Content getContent() {
      return content;
    }
  }

  static class Remove extends RelayDirective {
    public Remove(UID uid) {
      super(uid);
    }
  }

  static class Response extends RelayDirective {
    private Relay.Response response;
    public Response(UID uid, Relay.Response response) {
      super(uid);
      this.response = response;
    }
    public Relay.Response getResponse() {
      return response;
    }
  }
}
