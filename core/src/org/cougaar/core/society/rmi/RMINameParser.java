/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society.rmi;

import javax.naming.NameParser;
import javax.naming.Name;
import javax.naming.CompoundName;
import javax.naming.NamingException;
import java.util.Properties;


public class RMINameParser implements NameParser {

  private static final Properties syntax = new Properties();
  static {
    syntax.put("jndi.syntax.direction", "left_to_right");
    syntax.put("jndi.syntax.separator", NS.DirSeparator);
    syntax.put("jndi.syntax.ignorecase", "false");
  }
  public Name parse(String name) throws NamingException {
    return new CompoundName(name, syntax);
  }
}

