package org.cougaar.core.component;

import org.cougaar.core.society.*;
import java.util.*;

/** The binding site for talking to the Cougaar MessageTransport
 **/

public interface MessageTransportBindingSite 
  extends BindingSite
{
  void send(Message m) throws Exception;
  //...
}
