/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.relay;

import org.cougaar.core.blackboard.DirectiveImpl;
import org.cougaar.core.util.UID;

/**
 * Define Directives for Relay messages. Directives can add, change,
 * and remove a Relay.Target and send responses back to the
 * Relay.Source.
 **/
public abstract class RelayDirective extends DirectiveImpl {
  protected final UID uid;

  public RelayDirective(UID uid) {
    this.uid = uid;
  }

  public UID getUID() {
    return uid;
  }

  public static class Add extends RelayDirective {
    private Object content;
    private Relay.TargetFactory tf;

    public Add(UID uid, Object content, Relay.TargetFactory tf) {
      super(uid);
      this.content = content;
      this.tf = tf;
    }
    public Object getContent() {
      return content;
    }
    public Relay.TargetFactory getTargetFactory() {
      return tf;
    }
    public String toString() {
      return "(add uid="+uid+" content="+content+")";
    }
  }

  public static class Change extends RelayDirective {
    private Object content;
    private Relay.TargetFactory tf;

    public Change(UID uid, Object content, Relay.TargetFactory tf) {
      super(uid);
      this.content = content;
      this.tf = tf;
    }
    public Object getContent() {
      return content;
    }
    public Relay.TargetFactory getTargetFactory() {
      return tf;
    }
    public String toString() {
      return "(change uid="+uid+" content="+content+")";
    }
  }

  public static class Remove extends RelayDirective {
    public Remove(UID uid) {
      super(uid);
    }
    public String toString() {
      return "(remove uid="+uid+")";
    }
  }

  public static class Response extends RelayDirective {
    private Object response;
    public Response(UID uid, Object response) {
      super(uid);
      this.response = response;
    }
    public Object getResponse() {
      return response;
    }
    public String toString() {
      return "(response uid="+uid+" response="+response+")";
    }
  }
}
