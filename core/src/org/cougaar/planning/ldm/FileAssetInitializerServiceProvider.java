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

package org.cougaar.planning.ldm;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.planning.plugin.asset.AssetDataFileReader;
import org.cougaar.planning.plugin.asset.AssetDataReader;
import org.cougaar.planning.service.AssetInitializerService;

class FileAssetInitializerServiceProvider implements ServiceProvider {

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != AssetInitializerService.class) {
      throw new IllegalArgumentException(
          getClass() + " does not furnish " + serviceClass);
    }
    return new AssetInitializerServiceImpl();
  }
  
  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class AssetInitializerServiceImpl implements AssetInitializerService {
    public String getAgentPrototype(String agentName) {
      throw new UnsupportedOperationException();
    }
    public String[] getAgentPropertyGroupNames(String agentName) {
      throw new UnsupportedOperationException();
    }
    public Object[][] getAgentProperties(String agentName, String pgName) {
      throw new UnsupportedOperationException();
    }
    public String[][] getAgentRelationships(String agentName) {
      throw new UnsupportedOperationException();
    }
    public AssetDataReader getAssetDataReader() {
      return new AssetDataFileReader();
    }
    public Object[] translateAttributeValue(String type, String key) {
      return new Object[] {type, key};
    }
  }
}
