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
package org.cougaar.core.component;

import java.net.URL;
import java.io.Serializable;

/**
 * An immutable description of a loadable component (for example, 
 * a plugin, servlet, etc).
 * <p>
 * We may want several levels of description and protection, 
 * starting here and ending up at an uninitialized instance.  
 * This could be done either as a sequence of classes or
 * as a single class with instantiation state (e.g. Description,
 * Classloaded, Instantiated, Loaded (into a component), Active).
 * <p>
 * The Description is interpreted and evaluated to varying degrees
 * as it is passed through the hierarchy until the insertion point
 * is found.  In particular, the description will be evaluated for
 * trust attributes at each level before instantiation or pass-through.
 * <p>
 **/
public final class ComponentDescription implements Serializable {

  private final String name;
  private final String insertionPoint;
  private final String classname;
  private final URL codebase;
  private final Object parameter;
  private final Object certificate;
  private final Object lease;
  private final Object policy;

  /**
   * A ComponentDescription must have a non-null name, 
   * insertionPoint, and classname.
   *
   * @throws IllegalArgumentException if name is null,
   *         insertionPoint is null, or classname is null.
   */
  public ComponentDescription(String name,
                              String insertionPoint,
                              String classname,
                              URL codebase,
                              Object parameter,
                              Object certificate,
                              Object lease,
                              Object policy) {
    this.name = name;
    this.insertionPoint = insertionPoint;
    this.classname = classname;
    this.codebase = codebase;
    this.parameter = parameter;
    this.certificate = certificate;
    this.lease = lease;
    this.policy = policy;

    // clone the object parameters to ensure immutability?

    // validate -- also see "readObject(..)"
    if (name == null) {
      throw new IllegalArgumentException("Null name");
    } else if (insertionPoint == null) {
      throw new IllegalArgumentException("Null insertionPoint");
    } else if (classname == null) {
      throw new IllegalArgumentException("Null classname");
    }
  }

  /** 
   * The name of a particular component, used
   * both as the displayable identifier of 
   * the Component and to disambiguate between 
   * multiple similar components which might otherwise
   * appear equal. <p>
   * It would be consistent to treat the name as a UID/OID for
   * a specific component.
   **/
  public String getName() { return name; }

  /**
   * The point in the component hierarchy where the component
   * should be inserted.  It is used by the component hierarchy to
   * determine the container component the plugin should be
   * added.  This point is interpreted individually by each
   * (parent) component as it propagates through the container
   * hierarchy - it may be interpreted any number of times along
   * the way to the final insertion point. <p>
   * example: a plugin would have an insertion point of
   * "Node.AgentManager.Agent.PluginManager.Plugin"
   * <p>
   * Note that data formats (including presentation methods) may
   * abbreviate full insertion paths as relative to some parent
   * in the context.  For instance, agent.ini files may refer to
   * plugins as ".PluginManager.Plugin".
   */
  public String getInsertionPoint() { return insertionPoint; }

  /**
   * The name of the class to instantiate, relative to the
   * codebase url.  The class will not be loaded or instantiated
   * until the putative parent component has been found and has
   * had the opportunity to verify the plugin's identity and 
   * authorization.
   **/
  public String getClassname() { return classname; }

  /**
   * Where the code for classname should be loaded from.
   * Will be evaulated for trust before any classes are loaded
   * from this location.
   **/
  public URL getCodebase() { return codebase; }

  /**
   * A parameter supplied to the constructor of classname,
   * often some sort of structured object (xml document, etc).
   * <p>
   * This is defined as just an Object, but we will likely 
   * have to impose additional restrictions (e.g. Serializable) 
   * for safety reasons.
   **/
  public Object getParameter() { return parameter; }

  /**
   * Assurance that the Plugin is trustworth enough to instantiate.
   * The type is specified as Object until we decide what really
   * should be here.
   **/
  public Object getCertificate() { return certificate; }

  /**
   * Lease information - how long should the plugin live in the agent?
   * We need some input on what this should look like.
   * It is possible that this could be merged with Policy.
   **/
  public Object getLeaseRequested() { return lease; }

  /**
   * High-level Policy information.  Allows plugin policy/techspec 
   * to contribute to the component hookup process before it is 
   * actually instantiated.  Perhaps this is overkill, and instance-level
   * policy is sufficient.
   **/
  public Object getPolicy() { return policy; }

  /**
   * Equality tests <i>all</i> publicly visibile fields.
   * <p>
   * In the future this may be modified to ignore the
   * certificate, lease, and/or policy.
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ComponentDescription)) {
      return false;
    }
    ComponentDescription cd = (ComponentDescription) o;
    // simplistic equality test:
    if (eq(name, cd.name) &&
        eq(insertionPoint, cd.insertionPoint) &&
        eq(classname, cd.classname) &&
        eq(codebase, cd.codebase) &&
        eq(parameter, cd.parameter) &&
        eq(certificate, cd.certificate) &&
        eq(lease, cd.lease) &&
        eq(policy, cd.policy)) {
      return true;
    }
    return false;
  }

  private final static boolean eq(Object a, Object b) {
    return ((a == null) ? (b == null) : (a.equals(b)));
  }

  public int hashCode() {
    return 
      (name.hashCode() ^ 
       insertionPoint.hashCode() ^ 
       classname.hashCode());
  }

  private void readObject(java.io.ObjectInputStream ois) 
  throws java.io.IOException, ClassNotFoundException {
    ois.defaultReadObject();
    // validate -- see constructor
    if (name == null) {
      throw new java.io.InvalidObjectException("Null name");
    } else if (insertionPoint == null) {
      throw new java.io.InvalidObjectException("Null insertionPoint");
    } else if (classname == null) {
      throw new java.io.InvalidObjectException("Null classname");
    }
  }

  public String toString() {
    return "<ComponentDescription "+classname+
      ((parameter==null)?"":(" "+parameter))+
      ">";
  }

  private static final long serialVersionUID = 1673609926074089996L;
}
