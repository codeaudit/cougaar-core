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

/* Constant names for Auxiliary Query types which are used to return
 * extra information within an AllocationResult that are not necessarily
 * related to a preference
 */
public interface AuxiliaryQueryType {
  
  int PORT_NAME = 0;
    
  int FAILURE_REASON = 1;
  
  int UNIT_SOURCED = 2;
  
  int POE_DATE = 3;
  
  int POD_DATE = 4;
  
  int READINESS = 5;
  
  int OVERTIME = 6;
  
  int PLANES_AVAILABLE = 7;
  

  int LAST_AQTYPE = 7;
  int AQTYPE_COUNT = LAST_AQTYPE+1;
  
  int UNDEFINED = -1;
}

