/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
  public static interface Directory {
    String getPath();
  }
}







