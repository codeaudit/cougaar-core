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

package org.cougaar.core.topology;

/**
 * Package-private name server directory and attribute 
 * constants.
 * <p>
 * These constants are subject to change without notice.
 * All clients should use the topology services and 
 * <i>not</i> poke around the name server directories!
 */
final class TopologyNamingConstants {

  // Name server directory:
  public static final String TOPOLOGY_DIR = "Topology";

  // attributes:
  public static final String ENCLAVE_ATTR = "Enclave";
  public static final String SITE_ATTR = "Site";
  public static final String HOST_ATTR = "Host";
  public static final String NODE_ATTR = "Node";
  public static final String AGENT_ATTR = "Agent";
  public static final String INCARNATION_ATTR = "Incarnation";
  public static final String MOVE_ID_ATTR = "MoveId";
  public static final String TYPE_ATTR = "Type";
  public static final String STATUS_ATTR = "Status";

  // matches TopologyReaderService types
  public static final String[] TYPE_TO_ATTRIBUTE_NAME =
    new String[] {
      AGENT_ATTR,
      NODE_ATTR,
      HOST_ATTR,
      SITE_ATTR,
      ENCLAVE_ATTR,
    };

}
