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

package org.cougaar.core.naming;

import org.cougaar.core.service.*;

import javax.naming.NameParser;
import javax.naming.Name;
import javax.naming.CompoundName;
import javax.naming.NamingException;
import java.util.Properties;


/**
 * Implementation of javax.naming.NameParser for 
 * Cougaar Naming Service. Specfies left to right parsing,
 * use of NS.DirSeparator as the CompoundName separator, and
 * recognizes case distinctions in the text.
 */

public class NamingParser implements NameParser {

  private static final Properties syntax = new Properties();
  static {
    syntax.put("jndi.syntax.direction", "left_to_right");
    syntax.put("jndi.syntax.separator", NS.DirSeparator);
    syntax.put("jndi.syntax.ignorecase", "false");
  }

  /**
   * Parses a name into its components.
   *
   * @param name The non-null string name to parse.
   * @return A non-null parsed form of the name using the naming convention
   * of this parser.
   * @exception InvalidNameException If name does not conform to
   * 	syntax defined for the namespace.
   * @exception NamingException If a naming exception was encountered.
   */
  public Name parse(String name) throws NamingException {
    return new CompoundName(name, syntax);
  }
}













