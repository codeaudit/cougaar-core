/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.asset;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.NewTimeSpan;

public interface NewTimePhasedPropertyGroup extends NewPropertyGroup, NewTimeSpan, TimePhasedPropertyGroup {

  public void setTimeSpan(TimeSpan timeSpan);
}
