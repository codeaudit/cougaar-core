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


/**
 * We need to redefine the RSS constants here, since rss.jar isn't
 * accessible.
 */
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

    String CPU_LOAD_AVG_1_SEC_AVG = "CPULoadAvg1SecAvg";
    String CPU_LOAD_AVG_10_SEC_AVG = "CPULoadAvg10SecAvg";
    String CPU_LOAD_AVG_100_SEC_AVG = "CPULoadAvg100SecAvg";
    String CPU_LOAD_AVG_1000_SEC_AVG = "CPULoadAvg1000SecAvg";

    String CPU_LOAD_JIPS_1_SEC_AVG = "CPULoadJips1SecAvg";
    String CPU_LOAD_JIPS_10_SEC_AVG = "CPULoadJips10SecAvg";
    String CPU_LOAD_JIPS_100_SEC_AVG = "CPULoadJips100SecAvg";
    String CPU_LOAD_JIPS_1000_SEC_AVG = "CPULoadJips1000SecAvg";

    String MSG_IN_1_SEC_AVG = "MsgIn1SecAvg";
    String MSG_IN_10_SEC_AVG = "MsgIn10SecAvg";
    String MSG_IN_100_SEC_AVG = "MsgIn100SecAvg";
    String MSG_IN_1000_SEC_AVG = "MsgIn1000SecAvg";

    String MSG_OUT_1_SEC_AVG = "MsgOut1SecAvg";
    String MSG_OUT_10_SEC_AVG = "MsgOut10SecAvg";
    String MSG_OUT_100_SEC_AVG = "MsgOut100SecAvg";
    String MSG_OUT_1000_SEC_AVG = "MsgOut1000SecAvg";

    String BYTES_IN_1_SEC_AVG = "BytesIn1SecAvg";
    String BYTES_IN_10_SEC_AVG = "BytesIn10SecAvg";
    String BYTES_IN_100_SEC_AVG = "BytesIn100SecAvg";
    String BYTES_IN_1000_SEC_AVG = "BytesIn1000SecAvg";

    String BYTES_OUT_1_SEC_AVG = "BytesOut1SecAvg";
    String BYTES_OUT_10_SEC_AVG = "BytesOut10SecAvg";
    String BYTES_OUT_100_SEC_AVG = "BytesOut100SecAvg";
    String BYTES_OUT_1000_SEC_AVG = "BytesOut1000SecAvg";


    String MSG_FROM_1_SEC_AVG = "MsgFrom1SecAvg";
    String MSG_FROM_10_SEC_AVG = "MsgFrom10SecAvg";
    String MSG_FROM_100_SEC_AVG = "MsgFrom100SecAvg";
    String MSG_FROM_1000_SEC_AVG = "MsgFrom1000SecAvg";

    String MSG_TO_1_SEC_AVG = "MsgTo1SecAvg";
    String MSG_TO_10_SEC_AVG = "MsgTo10SecAvg";
    String MSG_TO_100_SEC_AVG = "MsgTo100SecAvg";
    String MSG_TO_1000_SEC_AVG = "MsgTo1000SecAvg";

    String BYTES_FROM_1_SEC_AVG = "BytesFrom1SecAvg";
    String BYTES_FROM_10_SEC_AVG = "BytesFrom10SecAvg";
    String BYTES_FROM_100_SEC_AVG = "BytesFrom100SecAvg";
    String BYTES_FROM_1000_SEC_AVG = "BytesFrom1000SecAvg";

    String BYTES_TO_1_SEC_AVG = "BytesTo1SecAvg";
    String BYTES_TO_10_SEC_AVG = "BytesTo10SecAvg";
    String BYTES_TO_100_SEC_AVG = "BytesTo100SecAvg";
    String BYTES_TO_1000_SEC_AVG = "BytesTo1000SecAvg";

    String PERSIST_SIZE_LAST = "PersistSizeLast";

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

