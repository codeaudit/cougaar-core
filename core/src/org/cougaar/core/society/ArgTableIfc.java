/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

/**
*	This interface publishes the static keys for all known arguments for running a Node or SANode.
**/
public interface ArgTableIfc {
  /** which config are we running? */
  final String CONFIG_KEY = "config";
  /** config server if defined **/
  final String CS_KEY = "cs";
  /** name service **/
  final String NS_KEY = "ns";

 	/** Publish the -c keys so we do not hard code all over the place. **/
	public final String CLEAR_KEY = "clear";
	/** Publish the -d keys so we do not hard code all over the place. **/
	public final String DBNAME_KEY = "dnname";
	/** Publish the -f keys so we do not hard code all over the place. **/
	public final String FILE_KEY = "file";
	/** Publish the -n keys so we do not hard code all over the place. **/
	public final String NAME_KEY = "name";
	/** Publish the -p keys so we do not hard code all over the place. **/
	public final String PORT_KEY = "port";
	/** Publish the -p keys so we do not hard code all over the place. **/
	public final String REGISTRY_KEY = "registry";
	public final String LOCAL_KEY = "local";
	/** Publish the -t keys so we do not hard code all over the place. **/
	public final String TEST_KEY = "test";
	/** Publish the -w keys so we do not hard code all over the place. **/
	public final String WAIT_KEY = "wait";


        /** Publish the -s keys so we do not hard code all over the place. **/
        public final String SIGNED_PLUGIN_JARS = "plugin_jars";
}
			
