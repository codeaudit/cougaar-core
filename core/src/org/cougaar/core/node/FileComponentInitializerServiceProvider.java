/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.io.InputStream;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.ConfigFinder;

/**
 * Provides service to initialize components from INI files.
 **/
public class FileComponentInitializerServiceProvider implements ServiceProvider {

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != ComponentInitializerService.class) {
      throw new IllegalArgumentException(
          getClass() + " does not furnish " + serviceClass);
    }
    return new ComponentInitializerServiceImpl();
  }
  
  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class ComponentInitializerServiceImpl implements ComponentInitializerService {
    /**
     * Get the descriptions of components with the named parent having
     * an insertion point below the given container insertion point.
     **/
    public ComponentDescription[] getComponentDescriptions(
        String parentName, String containerInsertionPoint) throws InitializerException {
      try {
        String filename = parentName;
        if (! parentName.endsWith(".ini")) {
          filename = parentName + ".ini";
        }
        InputStream in = ConfigFinder.getInstance().open(filename);
        try {
          return INIParser.parse(in, containerInsertionPoint);
        } finally {
          in.close();
        }
      } catch (Exception e) {
        throw new InitializerException(
            "getComponentDescriptions("+parentName+", "+containerInsertionPoint+")",
            e);
      }
    }
  }
}
