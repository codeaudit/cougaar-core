/*
 * <copyright>
 *  Copyright 1997-2003 Mobile Intelligence Corp
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
package org.cougaar.core.service.community;

import javax.naming.directory.Attributes;

/**
 * Interface defining entities that are associated with a community.  An
 * entity is typically an Agent or a Community.
 */
public interface Entity {

  /**
   * Set entity name.
   * @param name  Entity name
   */
  public void setName(String name);

  /**
   * Get entity name.
   * @return Entity name
   */
  public String getName();

  /**
   * Set entity attributes.
   * @param attrs Entity attributes
   */
  public void setAttributes(Attributes attrs);

  /**
   * Get entity attributes.
   * @return Entity attributes
   */
  public Attributes getAttributes();

  /**
   * Returns an XML representation of Entity.
   */
  public String toXml();

  /**
   * Returns an XML representation of Entity.
   * @param indent Blank string used to pad beginning of entry to control
   *               indentation formatting
   */
  public String toXml(String indent);

  /**
   * Creates a string representation of an Attribute set.
   */
  public String attrsToString();
}