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

import org.cougaar.domain.planning.ldm.plan.ScoringFunction;

/** Description of an allocation Preference for a task in the same currency
 * as an allocationresult.  
 *
 * Implementations are immutable.
 *
 * @author ALPINE <alpine-software@bbn.com>
 **/
 
public interface Preference extends AspectType, Cloneable 
{
   
  /** @return int  The AspectType that this preference represents
   * @see org.cougaar.domain.planning.ldm.plan.AspectType
   */
   int getAspectType();
   
  /** @return ScoringFunction
   * @see org.cougaar.domain.planning.ldm.plan.ScoringFunction
   */
  ScoringFunction getScoringFunction();
   
  /** A Weighting of this preference from 0.0-1.0, 1.0 being high and
   * 0.0 being low.
   */
  float getWeight();
}
