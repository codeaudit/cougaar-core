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

/* Constant names for Auxiliary Query types which are used to return
 * extra information within an AllocationResult that are not necessarily
 * related to a preference
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AuxiliaryQueryType.java,v 1.3 2001-04-05 19:27:14 mthome Exp $
 */
public interface AuxiliaryQueryType {
  
  static final int PORT_NAME = 0;
    
  static final int FAILURE_REASON = 1;
  
  static final int UNIT_SOURCED = 2;
  
  static final int POE_DATE = 3;
  
  static final int POD_DATE = 4;
  
  static final int READINESS = 5;
  
  static final int OVERTIME = 6;
  
  static final int PLANES_AVAILABLE = 7;
  

  static final int LAST_AQTYPE = 7;
  static final int AQTYPE_COUNT = LAST_AQTYPE+1;
  
  static final int UNDEFINED = -1;
}

