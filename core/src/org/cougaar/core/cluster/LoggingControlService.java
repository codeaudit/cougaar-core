/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.component.*;
import java.util.Enumeration;

/** a Service for controlling logging
 **/

public interface LoggingControlService extends Service {

  public static final int CONSOLE = 1;
  public static final int STREAM  = 2;
  public static final int FILE    = 3;

  public int getLoggingLevel(String node);
  public void setLoggingLevel(String node, int level);
  //public void setLoggingLevel(String node, int level, boolean recursiveSet);

  public Enumeration getOutputTypes(String node);
  public void addOutputType(String node, int outputType);
  //public void removeOutputType(String node, int outputType);
  //public void setOutputType(String node, int outputType, boolean recursiveSet);

}




