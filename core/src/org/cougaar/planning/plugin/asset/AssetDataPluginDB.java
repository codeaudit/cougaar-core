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

package org.cougaar.planning.plugin.asset;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Arrays;
import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.service.AssetInitializerService;

/**
 * Populates an "Asset" from the config database.
 **/
public class AssetDataPluginDB extends AssetDataPluginBase {
  private String clusterId;
  AssetInitializerService assetInitService;

  public void setAssetInitializerService(AssetInitializerService assetInitService) {
    this.assetInitService = assetInitService;
  }

  /**
   * 
   */
  protected void readAsset(String cId) {
    try {
      clusterId = cId;
      createMyLocalAsset(assetInitService.getAgentPrototype(cId));
      String[] pgNames = assetInitService.getAgentPropertyGroupNames(cId);
      for (int i = 0; i < pgNames.length; i++) {
        String pgName = pgNames[i];
        createPropertyGroup(pgName);
        addPropertyToAsset();
        Object[][] props = assetInitService.getAgentProperties(cId, pgName);
        for (int j = 0; j < props.length; j++) {
          Object[] prop = props[j];
          String attributeName = (String) prop[0];
          String attributeType = (String) prop[1];
          if (attributeType.equals("FixedLocation")) {
            String v = ((String) prop[2]).trim();
            if (v.startsWith("(")) v = v.substring(1);
            if (v.endsWith(")")) v = v.substring(0, v.length() - 1);
            int ix = v.indexOf(',');
            String latStr = v.substring(0, ix);
            String lonStr = v.substring(ix + 1);
            setLocationSchedule(latStr.trim(), lonStr.trim());
          } else {
            if (prop[2].getClass().isArray()) {
              String[] rv = (String[]) prop[2];
              Object[] pv = new Object[rv.length];
              for (int k = 0; k < rv.length; k++) {
                pv[k] = parseExpr(attributeType, rv[k]);
              }
              Object[] args = {Arrays.asList(pv)};
              callSetter("set" + attributeName, "Collection", args);
            } else {
              Object[] args = {parseExpr(attributeType, (String) prop[2])};
              callSetter("set" + attributeName, getType(attributeType), args);
            }
          }
        }
      }
      String[][] relationships = assetInitService.getAgentRelationships(cId);
      for (int i = 0; i < relationships.length; i++) {
        String[] r = relationships[i];
        long start, end;
        if (r[4] == null) {
          start = getDefaultStartTime();
        } else {
          try {
            start = myDateFormat.parse(r[4]).getTime();
          } catch (java.text.ParseException pe) {
            start = getDefaultStartTime();
            System.out.println("Unable to parse: " + r[4] + 
                               ". Start time defaulting to " + 
                               start);
          }
        }
        if (r[5] == null) {
          end = getDefaultEndTime();
        } else {
          try {
            end = myDateFormat.parse(r[5]).getTime();
          } catch (java.text.ParseException pe) {
            end = getDefaultEndTime();
            System.out.println("Unable to parse: " + r[5] + 
                               ". End time defaulting to " + 
                               end);
          }
        }
        addRelationship(r[2],     // Type id
                        r[1],     // Item id
                        r[3],     // Other cluster
                        r[0],     // Role
                        start,    // Start time
                        end);      // End time
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }
}
