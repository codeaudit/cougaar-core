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

package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import java.io.Serializable;

/** A marker class describing a single change to an object (usually
 * a Publishable object).  Subclasses describe specific types of changes. <p>
 *
 * hashCode and equals methods should compute their return values based
 * on the type and not any specific (old) value which may be stored in the
 * object.  This allows use of set operations on ChangeReport instances to
 * filter out redundant reports. <p>
 *
 * Implementations should
 * be as compact as possible and must be Serializable so that they are
 * transferrable between distributed clusters and persistable. <p>
 *
 * @see org.cougaar.planning.ldm.plan.Publishable
 **/

public interface ChangeReport 
  extends Serializable
{
}
