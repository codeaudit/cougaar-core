/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning;

import org.cougaar.domain.planning.ldm.plan.AspectType;

public interface Constants {

  public static interface Verb {
    // ALPINE defined verb types
    // Keep in alphabetical order
    public static final String REPORT = "Report";

    public static final org.cougaar.domain.planning.ldm.plan.Verb Report= org.cougaar.domain.planning.ldm.plan.Verb.getVerb(REPORT);
  }

  public static interface Preposition {
    // ALPINE defined prepositions
    public static final String WITH        = "With"; 	// typically used for the OPlan object
    public static final String TO          = "To"; 	// typically used for a destination geoloc
    public static final String FROM        = "From"; 	// typically used for an origin geoloc
    public static final String FOR         = "For"; 	// typically used for the originating organization
    public static final String OFTYPE      = "OfType"; 	// typically used with abstract assets
    public static final String USING       = "Using"; 	// typically used for ???
    public static final String AS          = "As"; 	// used with Roles for RFS/RFD task
  }
}







