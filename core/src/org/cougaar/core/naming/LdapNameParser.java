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

package org.cougaar.core.naming;

import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * NameParser that converts "cougaar" names (eg /Agents/foo) into X500 names
 * (eg "dc=foo, dc=Agents").
 */
public class LdapNameParser implements javax.naming.NameParser {

    private static Logger log;
    
    public LdapNameParser() {
        if (log == null)
            log = Logging.getLogger(this.getClass().getName());
    }

    public boolean equals(java.lang.Object obj) {
        boolean retValue;
        
        retValue = super.equals(obj);
        return retValue;
    }
    
    public int hashCode() {
        int retValue;
        
        retValue = super.hashCode();
        return retValue;
    }
    
  private static final Properties syntax = new Properties();
  private static final String separator = org.cougaar.core.naming.NS.DirSeparator;
  static {
    syntax.put("jndi.syntax.direction", "right_to_left");
    syntax.put("jndi.syntax.separator", ",");
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
    String x500name = x500ify(name);
    x500name = convertToDN(name);
    if (log.isDebugEnabled()) log.debug("Parsed : "+name+" into "+x500name);
    return new CompoundName(x500name, syntax);
  }
  
  static String x500ify(String name) {
      StringBuffer ret = new StringBuffer();
      StringTokenizer toker = new StringTokenizer(name, separator);
      while (toker.hasMoreTokens()) {
          String next = toker.nextToken();
          ret.append("/dc=");
          ret.append(next);
      }
      return ret.toString();
  }
  
  static String convertToDN(String name) {
      StringBuffer ret = new StringBuffer();
      // reverse the order for DN
      StringTokenizer toker = new StringTokenizer(name, separator);
      Stack stack = new Stack();
      while (toker.hasMoreTokens()) {
          stack.push(toker.nextToken());
      }
      while (!stack.empty()) {
          String next = (String)stack.pop();
          ret.append("dc=");
          ret.append(next);
          if (!stack.empty())
              ret.append(",");
      }
      return ret.toString();
  }

}
