/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;

/** 
 * Logging service for assessment events.
 * <p>
 * Events are intended for external profiling and monitoring
 * applications and often follow a strict application-defined
 * syntax.  In contrast, LoggingService logs are primarily for
 * human-readable debugging.
 * <p>
 * EventService clients should always check "isEventEnabled()"
 * before logging an event, for the same reasons as noted
 * in the LoggingService.
 * <p>
 * Events are currently equivalent to using the logging service
 * with the "EVENT.<i>classname</i>" log category and INFO
 * log level.
 * <p>
 * For example, if component "org.foo.Bar" emits an event, it
 * will be logged as category "EVENT.org.foo.Bar" and level
 * INFO.
 *
 * @see LoggingService
 */
public interface EventService extends Service {

  boolean isEventEnabled();

  void event(String s);

  void event(String s, Throwable t);

}
