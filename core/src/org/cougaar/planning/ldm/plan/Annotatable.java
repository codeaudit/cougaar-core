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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.plugin.Annotation;

/** Annotatable marks a Plan Object which supports attachment
 * of a plugin annotation.
 * @see org.cougaar.core.plugin.Annotation
 **/

public interface Annotatable 
{
  /**
   * Get the plugin annotation (if any) attached to the Annotatable
   * object.  
   *
   * Only the creating plugin of an Annotatable object
   * should use this accessor.
   *
   * @see org.cougaar.core.plugin.Annotation
   * @return the Annotation or null.
   **/
  Annotation getAnnotation();

  /**
   * Set the plugin annotation attached to the Annotatable 
   * Object.
   *
   * Only the creating plugin of an Annotatable object
   * should use this accessor.
   *
   * PlugIns are encouraged but not required to only
   * set the annotation one.
   *
   * @see org.cougaar.core.plugin.Annotation
   **/
  void setAnnotation(Annotation pluginAnnotation);
}

