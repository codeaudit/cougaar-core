/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

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

