/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.measure;

import java.io.Serializable;

/** Base for all Scalar measures classes.
 **/

public abstract class Scalar extends AbstractMeasure {
  public abstract double getValue(int unit);
}
