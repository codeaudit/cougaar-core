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

  int CONSOLE = 1;
  int STREAM  = 2;
  int FILE    = 3;

  int getLoggingLevel(String node);
  void setLoggingLevel(String node, int level);
  //void setLoggingLevel(String node, int level, boolean recursiveSet);

  Enumeration getAllLoggingNodes();

  Enumeration getOutputTypes(String node);
  void addOutputType(String node, int outputType, Object outputDevice);
  void addOutputType(String node, int outputType);
  //void removeOutputType(String node, int outputType);
  //void setOutputType(String node, int outputType, boolean recursiveSet);

}




