/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import java.util.Vector;
import java.util.Enumeration;

import org.cougaar.domain.planning.ldm.plan.*;

/** An envelope sent by a PersistencePlugIn as the result of rehydration of a saved LogPlan.
 * The essential difference from a standard Envelope is that PersistenceEnvelope does
 * not update the delta lists of any subscription.
 **/

public class PersistenceEnvelope extends Envelope {
  protected boolean isVisible() { return false; }
}

