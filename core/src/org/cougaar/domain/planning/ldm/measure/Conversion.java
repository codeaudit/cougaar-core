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

/** Abstraction of a conversion function from one unit of measure to 
 * another compatible unit of measure.
 *  All subclasses of Conversion will implement the method convert,
 * which encapsulates any information and function needed to convert
 * between the two units of measure.
 *
 *  Only deals in doubles for now.
 **/

public interface Conversion {
  /** @return the result of converting from to a new unit of measure */
  double convert(double from);
}