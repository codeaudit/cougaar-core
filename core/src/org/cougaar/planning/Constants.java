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

package org.cougaar.planning;

import org.cougaar.planning.ldm.plan.AspectType;

public interface Constants {

  interface Verb {
    // ALPINE defined verb types
    // Keep in alphabetical order
    String REPORT = "Report";

    org.cougaar.planning.ldm.plan.Verb Report= org.cougaar.planning.ldm.plan.Verb.getVerb(REPORT);
  }

  interface Preposition {
    // ALPINE defined prepositions
    String WITH        = "With"; 	// typically used for the OPlan object
    String TO          = "To"; 	// typically used for a destination geoloc
    String FROM        = "From"; 	// typically used for an origin geoloc
    String FOR         = "For"; 	// typically used for the originating organization
    String OFTYPE      = "OfType"; 	// typically used with abstract assets
    String USING       = "Using"; 	// typically used for ???
    String AS          = "As"; 	// used with Roles for RFS/RFD task
  }
}







