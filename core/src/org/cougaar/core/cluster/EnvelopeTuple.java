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

import java.util.*;
import org.cougaar.util.UnaryPredicate;

/** EnvelopeTuple is a semi-private datastructure used as
 * the atomic member of an envelope.
 **/

public abstract class EnvelopeTuple implements java.io.Serializable {
  public abstract int getAction();
  abstract public Object getObject();
  public boolean isAdd() { return false; }
  public boolean isRemove() { return false; }
  public boolean isChange() { return false; }
  public boolean isBulk() { return false; }
  public boolean isEvent() { return false; }

  abstract boolean applyToSubscription(Subscription s, boolean isVisible);
}
