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
import org.cougaar.util.PropertyParser;

/** Blackboard objects which implement this interface
 * will have the appropriate methods invoked when
 * publishAdd(), publishChange() or publishRemove().
 * Typically, these methods are used to maintain
 * some object state, check for well-formedness and/or
 * emit warnings about various problems.
 **/
public interface ActiveSubscriptionObject {
  /** default value for property deferCommit **/
  boolean DEFAULT_DEFER_COMMIT = false;

  /** Property for controlling deferred commit of ActiveSubscriptionObject methods **/
  String DEFER_COMMIT_PROPERTY = ActiveSubscriptionObject.class.getName()+".deferCommit";

  /** When deferCommit is true, ActiveSubscriptionObject methods will be invoked with commit=false at 
   * publish time, and commit=true at LP time.  When deferCommit is false, both invocations
   * will happen at publishTime and certain checks will be disabled (as uninteresting).
   * The default value is defined by #DEFAULT_DEFER_COMMIT
   * @property org.cougaar.core.blackboard.ActiveSubscriptionObject.deferCommit 
   * When set to true, causes ActiveSubscriptionObject side effects to occur
   * at LP invocation time rather than immediately during publishAdd
   * @note deferCommit implies that the ActiveSubscriptionObject cannot veto publishes!
   **/
  boolean deferCommit = PropertyParser.getBoolean(DEFER_COMMIT_PROPERTY, DEFAULT_DEFER_COMMIT);

  /** called by Subscriber.publishAdd().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void addingToBlackboard(Subscriber subscriber, boolean commit);
  /** called by Subscriber.publishChange().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void changingInBlackboard(Subscriber subscriber, boolean commit);
  /** called by Subscriber.publishRemove().  
   * @throws BlackboardException if the object cannot be committed.
   **/
  void removingFromBlackboard(Subscriber subscriber, boolean commit);
}
