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

package org.cougaar.core.plugin;

import java.io.Serializable;

/** 
 * An Infrastructure-opaque type used for tagging various LDM objects 
 * with Plugin-specific information.  The infrastructure will never copy
 * or transmit Annotations between clusters, but will save them when 
 * persisting (hence the extends Serializable).
 *
 * Annotations may be attached to appropriate plan objects at object
 * construction time.  They should never be modified (or even examined)
 * by any entity other than the one that created the plan object.
 * 
 * This structure is intentionally opaque to the infrastructure.
 *
 * @see org.cougaar.planning.ldm.plan.Task
 * @see org.cougaar.planning.ldm.plan.Workflow
 * @see org.cougaar.planning.ldm.plan.PlanElement
 **/
public interface Annotation 
  extends Serializable
{
}
