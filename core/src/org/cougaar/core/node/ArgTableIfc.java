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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

/**
*       This interface publishes the static keys for all known arguments for running a Node or SANode.
**/
public interface ArgTableIfc {
  /** which config are we running? */
  String CONFIG_KEY = "config";
  /** config server if defined **/
  String CS_KEY = "cs";
  /** name service **/
  String NS_KEY = "ns";

  /** Publish the -c keys so we do not hard code all over the place. **/
  String CLEAR_KEY = "clear";
  /** Publish the -d keys so we do not hard code all over the place. **/
  String DBNAME_KEY = "dbname";
  /** Publish the -f keys so we do not hard code all over the place. **/
  String FILE_KEY = "file";
  /** Publish the -n keys so we do not hard code all over the place. **/
  String NAME_KEY = "name";
  /** Publish the -p keys so we do not hard code all over the place. **/
  String PORT_KEY = "port";
  /** Publish the -p keys so we do not hard code all over the place. **/
  String REGISTRY_KEY = "registry";
  String LOCAL_KEY = "local";
  /** Publish the -t keys so we do not hard code all over the place. **/
  String TEST_KEY = "test";
  /** Publish the -w keys so we do not hard code all over the place. **/
  String WAIT_KEY = "wait";
  /** Publish the -s keys so we do not hard code all over the place. **/
  String SIGNED_PLUGIN_JARS = "plugin_jars";
  /** Publish the -X keys so we do not hard code all over the place. **/
  String EXPERIMENT_ID_KEY = "experiment_id";
}
                        
