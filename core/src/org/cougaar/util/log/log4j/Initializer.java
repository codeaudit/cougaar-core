/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util.log.log4j;

import java.util.Map;

import org.apache.log4j.BasicConfigurator;

/** 
 * Package-private log4j class to initialize the logging
 * configuration.
 */
class Initializer {

  /**
   * Configure the factory, which sets the initial
   * logging configuration (levels, destinations, etc).
   */
  public static void configure(Map env) {
    BasicConfigurator.configure();
  }
}
