/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.util.*;
import org.cougaar.util.*;
import java.util.*;

/**
 * A useful predicate class and generator for selecting
 * predicates by type.  Assumes that it will only be
 * called on Preferences (or it will get a ClassCastException).
 **/
 
public final class PreferencePredicate implements UnaryPredicate
{
  private final int aspect;

  private PreferencePredicate(int a) {
    aspect=a;
  }

  public boolean execute(Object o) {
    return (aspect == ((Preference)o).getAspectType());
  }

  private static final UnaryPredicate predVector[];
  private static final HashMap predTable = new HashMap(11);
  static {
    int l = AspectType._ASPECT_COUNT;
    predVector = new UnaryPredicate[l];
    for (int i = 0; i<l; i++) {
      predVector[i] = new PreferencePredicate(i);
    }
  }

  /** @return a unary predicate which returns true IFF the object
   * preference's aspect type is the same as the argument.
   **/
  public static UnaryPredicate get(int aspectType) {
    if (aspectType<0) throw new IllegalArgumentException();

    if (aspectType<=AspectType._LAST_ASPECT) {
      // handle the common ones from a pre-initialized vector
      return predVector[aspectType];
    } else {
      // hash on the aspectType for the rest.
      Integer k = new Integer(aspectType);
      UnaryPredicate p;
      if ((p = (UnaryPredicate) predTable.get(k)) != null) return p;
      synchronized (predTable) {
        //if ((p = predTable.get(k)) != null) return p;
        p = new PreferencePredicate(aspectType);
        predTable.put(k,p);
        return p;
      }
    }
  }
}
