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

package org.cougaar.core.qos.metrics;

// We need to redefine the RSS constants here, since rss.jar isn't
// accessible.

public interface Constants
{
    /**
     * The character used to separate fields of a data key. */
    String KEY_SEPR = "_";

    /**
     * The character used to seperate fields of a data-lookup paths.
     */
    String PATH_SEPR = ":";


    /**
     * The category of thread-service measurements, and some specific
     * ones
     */
    String THREAD_SENSOR = "COUGAAR_THREAD";

    String ONE_SEC_LOAD_AVG = "OneSecondLoadAvg";
    String MSG_IN_1_SEC_AVG = "MsgIn1SecAvg";
    String MSG_IN_10_SEC_AVG = "MsgIn10SecAvg";
    String MSG_IN_100_SEC_AVG = "MsgIn100SecAvg";
    String MSG_IN_1000_SEC_AVG = "MsgIn1000SecAvg";


    // Credibility Spectrum: tries to unify many different notions of
    // credibility into a common metric. The Credibility "Calculus" is
    // still undefined, but here is the general notion for credibility
    // values. 
    //
    // The dimensions of credibility include 
    //   Aggregation: Time period over which the observation were made
    //   Staleness: How out-of-date is the data
    //   Source: How was the data collected
    //   Trust or Collector Authority: Can the collector be trusted
    //   Sensitivity: Does a key component of the data have low credibility
    /**
     * No source for data. There was an error or nobody was looking for
     * this data*/
    double NO_CREDIBILITY = 0.0;
    /**
     * Compile Time Default was the source for data */
    double DEFAULT_CREDIBILITY = 0.1;
    /**
     * System Level configuration file was the source for data */
    double SYS_DEFAULT_CREDIBILITY = 0.2;
    /**
     * User Level configuration file was the source for data */
    double USER_DEFAULT_CREDIBILITY = 0.3;
    /**
     * System Level Base-Line measurements file was the source
     * for data. This data is aggregated over Days */
    double SYS_BASE_CREDIBILITY = 0.4;
    /**
     * User Level Base-Line measurements file was the source for
     * data. This data is aggregated over Days */
    double USER_BASE_CREDIBILITY = 0.5;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Hours and is not stale*/
    double HOURLY_MEAS_CREDIBILITY = 0.6;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Minutes and is not stale*/
    double MINUTE_MEAS_CREDIBILITY = 0.7;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Seconds and is not stale*/
    double SECOND_MEAS_CREDIBILITY = 0.8;
    /**
     * A Multiple Active measurments were a Consistant source for data. 
     * This data is aggregated over Seconds and is not stale*/
    double CONFIRMED_MEAS_CREDIBILITY = 0.9;
    /**
     * A higher-level system has declared this datarmation to be true. 
     * Mainly used by debuggers and gods */
    double ORACLE_CREDIBILITY = 1.0;
}

