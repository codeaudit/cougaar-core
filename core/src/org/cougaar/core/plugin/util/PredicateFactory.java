/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin.util;

import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.util.UnaryPredicate;


import org.cougaar.core.plugin.util.NewExpandableTasksPredicate;
import org.cougaar.core.plugin.util.NewAllocationsPredicate;
import org.cougaar.core.plugin.util.NewAllocatableWFPredicate;

import org.cougaar.core.plugin.util.ExpandableTasksPredicate;
import org.cougaar.core.plugin.util.AllocatableWFPredicate;
import org.cougaar.core.plugin.util.AllocationsPredicate;

public class PredicateFactory {
    
  public static NewExpandableTasksPredicate newExpandableTasksPredicate() {
    ExpandableTasksPredicate et = new ExpandableTasksPredicate();
    return ( ( NewExpandableTasksPredicate ) et );
  }

  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewExpandableTasksPredicate newExpandableTasksPredicate( String ve, RootFactory ldmf ) {
    Verb newVerb = new Verb( ve );
    ExpandableTasksPredicate et = new ExpandableTasksPredicate( newVerb );
    return ( ( NewExpandableTasksPredicate ) et );
  }	

  public static NewAllocatableWFPredicate newAllocatableWFPredicate() {
    AllocatableWFPredicate atp = new AllocatableWFPredicate();
    return ( ( NewAllocatableWFPredicate ) atp );
  }

  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewAllocatableWFPredicate newAllocatableWFPredicate( String ve, RootFactory ldmf ) {
    Verb newVerb = new Verb( ve );
    AllocatableWFPredicate et = new AllocatableWFPredicate( newVerb );
    return ( ( NewAllocatableWFPredicate ) et );
  }	


  public static NewAllocationsPredicate newAllocationsPredicate() {
    AllocationsPredicate ap = new AllocationsPredicate();
    return ( ( NewAllocationsPredicate ) ap );
  }
    
  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewAllocationsPredicate newAllocationsPredicate( String ve, RootFactory ldmf ) {
    Verb newVerb = new Verb( ve );
    AllocationsPredicate ap = new AllocationsPredicate( newVerb );
    return ( ( NewAllocationsPredicate ) ap );
  }	
}
