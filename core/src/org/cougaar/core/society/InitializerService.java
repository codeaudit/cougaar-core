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

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.domain.planning.plugin.AssetDataReader;

public interface InitializerService extends Service {
    ComponentDescription[] getAgentDescriptions(String nodeName)
        throws InitializerServiceException;
    ComponentDescription[] getPluginDescriptions(String agentName)
        throws InitializerServiceException;
    String getAgentPrototype(String agentName)
        throws InitializerServiceException;
    String[] getAgentPropertyGroupNames(String agentName)
        throws InitializerServiceException;
    Object[][] getAgentProperties(String agentName, String pgName)
        throws InitializerServiceException;
    String[][] getAgentRelationships(String agentName)
        throws InitializerServiceException;
    AssetDataReader getAssetDataReader();
    Object[] translateAttributeValue(String type, String key)
        throws InitializerServiceException;
}
