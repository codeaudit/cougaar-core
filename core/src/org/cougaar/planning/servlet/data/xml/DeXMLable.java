/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.planning.servlet.data.xml;

import org.xml.sax.Attributes;

/**
 * Classes conforming to this interface can DEserialize themselves
 * from a slightly restricted XML format.<P>
 * The XML format used, does not allow character data between or
 * following nested tags. For example: <P>
 * <PRE>
 * <A>OK
 *   <B>OK</B>
 *   NOT OK
 *   <C>OK</C>
 *   NOT OK
 * </A>
 * </PRE>
 * <BR><P>
 * 
 * @author Benjamin Lubin; last modified by: $Author: ahelsing $
 *
 * @since 1/24/01
 **/
public interface DeXMLable{

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException;

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being deXMLized
   **/
  boolean closeTag(String name)
    throws UnexpectedXMLException;

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException;
}

