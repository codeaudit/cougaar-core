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

/**
 *  Role is a representation of a potential state of an asset.
 */

package org.cougaar.planning.ldm.plan;
import java.io.*;
import java.beans.*;
import java.util.*;

public final class Role implements Serializable, BeanInfo {
  // role cache - needs to be declared before the statics below
  private static HashMap roles = new HashMap(29);
  private static HashMap converseRoles = new HashMap(20);
  private static Object lock = new Object();

  private static final String DefaultConverse = "ConverseOf";

  public static final Role ASSIGNED = Role.getRole("Assigned");
  public static final Role AVAILABLE = Role.getRole("Available");
  public static final Role BOGUS = Role.getRole("Bogus");


  /** String holder for logical name.
   * No guarantee the string name is unique -- within the Cluster, much less
   * within the society of Clusters.
   **/
  private String name;
  private String nameConverse;

  /** getRole - finds the specified role. Uses role cache
   *  
   * @param vs String name of the role
   * @ return Role which has the specified name. Returns null if no such role exists.
   */
  public static Role getRole(String vs) {
    vs = vs.intern();
    synchronized (lock) {
      Role role = (Role) roles.get(vs);
      
      if (role == null) {
        create(vs, DefaultConverse + vs);
        role = (Role) roles.get(vs);
      }
      return role;
    }
  }

  /** create - creates specified role and its converse. 
   * @throws java.lang.IllegalArgumentException if role or converse already exist.
   * @param roleName String name of the role
   * @param converseName String name of the converse role
   */
  public static void create(String root, RelationshipType relationshipType) {
    String roleName = root + relationshipType.getFirstSuffix();
    String converseRoleName = root + relationshipType.getSecondSuffix();;

    create(roleName, converseRoleName);
  }

  public static void create(String roleName, String converseRoleName) {
    Role role = (Role)roles.get(roleName);
    Role converse = (Role)roles.get(converseRoleName);

    if (((role != null) && 
         (!role.nameConverse.equals(converseRoleName))) ||
        ((converse != null) && 
        (!converse.nameConverse.equals(roleName)))) {
      if (role != null) {
        System.err.println("Role.create() role: " + role.name + 
                           " already exists, converse: " + 
                           role.nameConverse);
      }
      if (converse != null) {
        System.err.println("Role.create() role: " + converse.name + 
                           " already exists, converse: " + 
                           converse.nameConverse);
      }

      throw new IllegalArgumentException("Role.create: unable to create role/converse pair - " +
                                         roleName + "/" + 
                                         converseRoleName);
    }

    synchronized (lock) {
      roleName = roleName.intern();
      role = new Role(roleName); // calls cacheRole
      role.nameConverse = converseRoleName;

      converseRoleName = converseRoleName.intern();
      
      if (roleName != converseRoleName) {
        converse = new Role(converseRoleName); // calls cacheRole
        converse.nameConverse = roleName;
        converseRoles.put(roleName, converse);
      }

      converseRoles.put(converseRoleName, role);
    }
  }

    
    
  /** Answer with the String representation of the name. */
  public String getName() { return name; }

  /** @return the converse role. */
  public Role getConverse() {
    return (Role) converseRoles.get(name);
  }

  /** Implementation of the comparison */
  public boolean equals(Object obj) {
    if (obj instanceof Role) {
      return getName().equals(((Role) obj).getName());
    }
    return false;
  }

  /** @deprecated Only to be used by Beans Introspection. */
  public Role() {
    name = "Bogus";
  }

  public String toString() {
    return name;
  }

  private transient int _hc = 0;
  public int hashCode() {
    if (_hc == 0) _hc = name.hashCode()+42;
    return _hc;
  }



  /** Should only be called through Role.getRole */
  private Role( String string ) {
    if ( string == null ) {
      throw new IllegalArgumentException ("Null valued strings are not allowed");
    }
    name = string.intern();
    cacheRole(this);
  }

  private static void cacheRole(Role v) {
    String vs = v.toString();
    synchronized (lock) {
      roles.put(vs, v);
    }
  }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    //if (name != null) name = name.intern();
  }


  // replace with an interned variation
  private Object readResolve() {
    if (roles.get(name) == null) {
      create(name, nameConverse);
    }
    return getRole(name);
  }

  // beaninfo
  private static PropertyDescriptor properties[];

  static {
    try {
      properties = new PropertyDescriptor[1];
      properties[0] = new PropertyDescriptor("name", Role.class, "getName", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }

  public BeanDescriptor getBeanDescriptor() { return null; }
  public int getDefaultPropertyIndex() { return -1; }
  public EventSetDescriptor[] getEventSetDescriptors() { return null; }
  public int getDefaultEventIndex() { return -1; }
  public MethodDescriptor[] getMethodDescriptors() { return null; }
  public BeanInfo[] getAdditionalBeanInfo() { return null; }
  public java.awt.Image getIcon(int iconKind) { return null; }


  public static void main(String []args) {
    System.out.println("Available: " + getRole("Available") + 
                       " converse: " + AVAILABLE.getConverse());

    System.out.println("Assigned: " + getRole("Assigned") + 
                       " converse: " + ASSIGNED.getConverse());

    System.out.println("Bogus: " + getRole("Bogus") + 
                       " converse: " + BOGUS.getConverse());

    create("Available", AVAILABLE.getConverse().getName());
    create("Available", "Assigned");
  }
} 






