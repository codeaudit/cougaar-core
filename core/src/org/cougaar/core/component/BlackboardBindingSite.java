package org.cougaar.core.component;

import org.cougaar.core.cluster.Subscription;
import org.cougaar.util.UnaryPredicate;
import java.util.*;

/** The binding site for talking to the Cougaar Blackboard
 **/

public interface BlackboardBindingSite 
  extends BindingSite
{
  Subscription subscribe(UnaryPredicate predicate);
  void unsubscribe(Subscription subscription);
  //...
}

