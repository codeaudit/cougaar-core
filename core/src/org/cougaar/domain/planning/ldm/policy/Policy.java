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

package org.cougaar.domain.planning.ldm.policy;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.cougaar.core.util.XMLizable;
import org.cougaar.domain.planning.ldm.plan.Transferable;
import org.cougaar.core.util.XMLize;
import org.cougaar.domain.planning.ldm.plan.Context;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.NotActiveException;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;

import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.society.OwnedUniqueObject;
import org.cougaar.core.society.UID;

/** Policy implementation
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/


/**
 * Policy is a class that contains RuleParameters
 **/
public class Policy
  extends OwnedUniqueObject
  implements java.io.Serializable, XMLizable,
  Transferable, UniqueObject
{ 
  protected Hashtable my_parameters;
  protected String name;

  /**
   * Default constructor. Creates a PolicyImpl with an empty Hashtable and 
   * no name.
   **/ 
  public Policy() {
    my_parameters = new Hashtable(11);
  }

  /**
   * Creates a new policy with the name policyName
   * @param policyName the name of the policy
   **/
  public Policy(String policyName) {
    this();
    name = policyName;
  }

  /**
   * Adds a new RuleParameter to the set stored in the Policy. This
   * should be the sole means for putting parameters into the policy
   * and can be overridden if desired to take special action such as
   * caching certain parameters for quicker access.
   * @param rule_parameter the new param to be added to the Policy
   **/
  public void Add(RuleParameter rule_parameter) 
  {
    my_parameters.put(rule_parameter.getName(), rule_parameter);
  }

  public void Remove(String name) {
    my_parameters.remove(name);
  }

  /**
   * Lookup and return parameter by name
   * Return null if no such parameter found.
   */
  public RuleParameter Lookup(String name) 
  {
    return (RuleParameter)my_parameters.get(name);
  }

  /**
   * Returns the policy name
   * @return the name of the Policy
   **/
  public String getName()
  {
    return name;
  }

  /**
   * Sets the name of the Policy
   * @param policyName the name of the policy
   **/ 
  public void setName(String policyName) {
    name = policyName;
  }

  /**
   * Replace a parameter with replacement_param
   * @param replacement_param a RuleParameter to replace a parameter
   * in the Policy. replacement_param must have the same getName() value
   * as an existing parameter in the Policy.
   * @return false if there is no matching param in the Policy, true 
   * otherwise
   **/
  public boolean Replace(RuleParameter replacement_param) 
  {
    if (my_parameters.get(replacement_param.getName()) != null) {
      Add(replacement_param);
      return true;
    }
    return false;
  }

  /** Replaces existing set of RuleParameters with new set 
   * @param params the new rule parameters
   **/
  public void setRuleParameters(RuleParameter[] params) {
    my_parameters = new Hashtable(11);
    for (int i=0; i<params.length; i++) {
      Add(params[i]);
    }
  }

  /**
   * @return this policy's rule parameters
   **/
  public RuleParameter[] getRuleParameters() {
    java.util.Enumeration e = my_parameters.elements();
    int i=0;
    RuleParameter[] rps = new RuleParameter[my_parameters.size()];
    while(e.hasMoreElements())
      rps[i++] = (RuleParameter)e.nextElement();
    return rps;
  }

  public Object clone() {

    Policy np = null;
    try {
      np = (Policy) getClass().newInstance();
    } catch ( InstantiationException ie ) {
	 ie.printStackTrace();
    } catch ( IllegalAccessException iae ) {
	 iae.printStackTrace();
    }
    if (np == null)
      return null;

    np.setName(this.getName());
    Enumeration e = my_parameters.elements();
    while(e.hasMoreElements()){
      RuleParameter rp = (RuleParameter)e.nextElement();
      np.Add((RuleParameter)rp.clone());
    }

    // same or different UID?
    np.setUID(this.getUID());
    np.setOwner(this.getOwner());

    // clone context?
    np.setContext(this.getContext());
    return np;
  }

  public boolean same(Transferable other) {
    // name is good enough
    if (other instanceof Policy)
      if (this.getName().equals(((Policy)other).getName()))
	return true;
    return false;
  }

  public void setAll(Transferable other) {
    if (!(other instanceof Policy))
      throw new IllegalArgumentException("Parameter not Policy");

    Policy oPolicy = (Policy) other;
    setUID(oPolicy.getUID());
    setOwner(oPolicy.getOwner());
    setRuleParameters(oPolicy.getRuleParameters());
    pcs.firePropertyChange(new PropertyChangeEvent(this, "RuleParameters", 
						   null, my_parameters));
  }

  private Context myContext = null;

  /**
   * Set the problem Context of this policy.
   * @see org.cougaar.domain.planning.ldm.plan.Context
   **/
  void setContext(Context context) {
    myContext = context;
  }
  /**
   * Get the problem Context (if any) for this policy.
   * @see org.cougaar.domain.planning.ldm.plan.Context
   **/
  Context getContext() {
    return myContext;
  }


  /**
   * Save the Policy object in some format (XML?) in some file
   **/
  public void save() {

    // stub
  }

  /**
   * Restore a Policy object
   **/ 
  public void load() {

    // stub
  }

  // 
  // XMLizable method for UI, other clients
  //
  public Element getXML(Document doc) {
    return XMLize.getPlanObjectXML(this,doc);
  }

  //dummy PropertyChangeSupport for the Jess Interpreter.
  protected transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
    pcs.removePropertyChangeListener(pcl);
  }

  private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException, NotActiveException {
    is.defaultReadObject();
    pcs = new PropertyChangeSupport(this);
  }
}

