/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
