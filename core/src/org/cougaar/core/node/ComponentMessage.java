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

package org.cougaar.core.node;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/**
 * Message to a <code>Container</code> that a <code>Component</code> be 
 * added/removed/etc.
 * <p>
 * All Component messages require a <code>ComponentDescription</code> to
 * uniquely identify the Component (via the ".equals(..)" and ".hashCode()"
 * methods).
 */
public class ComponentMessage 
extends Message {

  /** the operations */
  public static final int ADD     = 0;
  public static final int REMOVE  = 1;
  public static final int SUSPEND = 2;
  public static final int RESUME  = 3;
  public static final int RELOAD  = 4;
  private static final int MAX_OP  = RELOAD; // last entry

  /** one of the above constants, for example <tt>ADD</tt> */
  private int operation;

  /** @see #getComponentDescription() */
  private ComponentDescription desc;

  /** @see #getState() */
  private Object state;

  public ComponentMessage(
      MessageAddress aSource, 
      MessageAddress aTarget,
      int operation,
      ComponentDescription desc,
      Object state) {
    super(aSource, aTarget);
    setOperation(operation);
    setComponentDescription(desc);
    setState(state);
  }

  /**
   * Get the specified action, such as ADD.
   *
   * @see ComponentDescription
   */
  public int getOperation() {
    return operation;
  }

  /**
   * @see #getComponentDescription()
   */
  public void setOperation(int operation) {
    if ((operation < 0) ||
        (operation > MAX_OP)) {
      throw new IllegalArgumentException(
          "Invalid ComponentMessage \"operation\": "+operation);
    }
    this.operation = operation;
  }

  /**
   * Get the component description for the Component.
   *
   * @see ComponentDescription
   */
  public ComponentDescription getComponentDescription() {
    return desc;
  }

  /**
   * @see #getComponentDescription()
   */
  public void setComponentDescription(ComponentDescription desc) {
    this.desc = desc;
  }

  /**
   * Get the state for the Component to ADD or RELOAD.
   * <p>
   * The state must be a <code>java.io.Serializable</code> Object.
   * <p>
   * Together the description and state form a <code>StateTuple</code>.
   */
  public Object getState() {
    return state;
  }

  /**
   * @see #getState()
   */
  public void setState(Object state) {
    this.state = state;
  }

  public String toString() {
    String sOp;
    switch (operation) {
      case ADD:     sOp = "Add";     break;
      case REMOVE:  sOp = "Remove";  break;
      case SUSPEND: sOp = "Suspend"; break;
      case RESUME:  sOp = "Resume";  break;
      case RELOAD:  sOp = "Reload" ; break;
      default:      sOp = "Unknown"; break;
    }
    return 
      sOp+
      " ComponentMessage "+
      super.toString()+
      " ComponentDescription: "+desc+
      " State: "+
      ((state != null) ? 
       state.getClass().getName() : 
       "null");
  }
}
