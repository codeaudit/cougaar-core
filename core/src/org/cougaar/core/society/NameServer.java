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

package org.cougaar.core.society;

import java.util.*;

/** Abstraction of NameServer for ALP.
 * It is expected that there be multiple implementations.
 * Keys must be Strings.  Many of the Map methods are
 * stubbed.
 **/

public interface NameServer extends Map {
  
  String getDirSeparator();


  /** not implemented **/
  void clear();
  /** key must be a String **/
  boolean containsKey(Object key);
  /** not implemented **/
  boolean containsValue(Object value);
  /** only returns the entries at the root **/
  Set entrySet();
  /** like entrySet, except returns entries in an specified directory **/
  Set entrySet(Object directory);
  /** Look up an object in the NameService directory. 
   * name must be a string
   **/
  Object get(Object name);
  /** is the root (and therefor the whole tree) empty? **/
  boolean isEmpty();
  /** is the specified directory empty? **/
  boolean isEmpty(Object directory);
  Set keySet();
  Set keySet(Object directory);
  /** add an object to the directory **/
  Object put(Object name, Object o);
  void putAll(Map t);
  /** remove an object (and name) from the directory **/
  Object remove(Object name);
  int size();
  int size(Object directory);
  Collection values();
  Collection values(Object directory);
  interface Directory {
    String getPath();
  }
}







