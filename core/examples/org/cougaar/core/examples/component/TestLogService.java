/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.examples.component;

import java.util.*;
import java.net.URL;
import org.cougaar.core.component.*;

/** Simple Test service for Component model demo.
 * Implements a simple logging service.
 **/
public interface TestLogService 
  extends Service
{
  void log(String message);
}

