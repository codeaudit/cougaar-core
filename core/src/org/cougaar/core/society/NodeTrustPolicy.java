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

import org.cougaar.domain.planning.ldm.policy.*;
import java.util.Enumeration;

/**
 * A Node level trust policy for MessageTransport
 *
 * @author   ALPINE <alpine-software@bbn.com>
 *
 */

public class NodeTrustPolicy extends Policy {

  public static final int TRUST_NO_ONE = 0;
  public static final int TRUST_EVERYONE = 10;
  public static final String HOST = "Host";
  public static final String SOCIETY = "Society";
  public static final String SUBNET = "Subnet";
  private final String TRUST_CATEGORY = "Trust Category";
  private final String TRUST_LEVEL = "Trust Level";
  private final String TRAFFIC_NODES = "Traffic Nodes";
  
  public NodeTrustPolicy() {
    super("NodeTrustPolicy");
  }

  public String getTrustCategory() {
    StringRuleParameter param = (StringRuleParameter)Lookup(TRUST_CATEGORY);
    return (String) param.getValue();
  }

  public int getTrustLevel() {
    IntegerRuleParameter param = (IntegerRuleParameter)Lookup(TRUST_LEVEL);
    return param.intValue();
  }

  public String[] getNodes() {
    EnumerationRuleParameter param = (EnumerationRuleParameter)Lookup(TRAFFIC_NODES);
    if (param == null) {
      return null;
    } else {
      return param.getEnumeration();
    }
  }

  public void setTrustCategory(String category) {
    //check categor value
    if (! category.equals(SOCIETY) || category.equals(HOST) || 
        category.equals(SUBNET)) {
      throw new IllegalArgumentException("\n"+this+" category parameter "+
                                         "must be either NodeTrustPolicy.SOCIETY "+
                                         ", NodeTrustPolicy.HOST or " +
                                         "NodeTrustPolicy.SUBNET");
    }
    StringRuleParameter srp = new StringRuleParameter(TRUST_CATEGORY);
    try {
      srp.setValue(category);
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(srp);
  }

  public void setTrustLevel(int trust_level) {
    try {
      IntegerRuleParameter irp = 
        new IntegerRuleParameter(TRUST_LEVEL, 0, 10, trust_level);
    Add(irp);
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public void setNodes(String []nodes) {
   if (nodes != null && nodes.length != 0) {
      EnumerationRuleParameter erp  = new EnumerationRuleParameter(TRAFFIC_NODES, nodes);
      Add(erp);
    }
  }

}




