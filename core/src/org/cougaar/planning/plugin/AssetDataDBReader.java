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
package org.cougaar.planning.plugin;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Arrays;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.InitializerService;
import org.cougaar.planning.ldm.asset.NewPropertyGroup;

/**
 * Populates an "Asset" from the config database.
 **/
public class AssetDataDBReader implements AssetDataReader {
  private AssetDataCallback cb;
  private String clusterId;
  InitializerService initializerService;

  public AssetDataDBReader(InitializerService is) {
    initializerService = is;
  }

  /**
   * 
   */
  public void readAsset(String cId, AssetDataCallback cb) {
    this.cb = cb;
    try {
      clusterId = cId;
      cb.createMyLocalAsset(initializerService.getAgentPrototype(cId));
      String[] pgNames = initializerService.getAgentPropertyGroupNames(cId);
      for (int i = 0; i < pgNames.length; i++) {
        String pgName = pgNames[i];
        cb.createPropertyGroup(pgName);
        cb.addPropertyToAsset();
        Object[][] props = initializerService.getAgentProperties(cId, pgName);
        for (int j = 0; j < props.length; j++) {
          Object[] prop = props[j];
          String attributeName = (String) prop[0];
          String attributeType = (String) prop[1];
          Object attributeValue = prop[2];
          if (attributeType.startsWith("query")) {
            String v = ((String) attributeValue).trim();
            Object[] r = initializerService.translateAttributeValue(attributeType, v);
            attributeType = (String) r[0];
            attributeValue = r[1];
          }
          if (attributeType.equals("FixedLocation")) {
            String v = ((String) attributeValue).trim();
            if (v.startsWith("(")) v = v.substring(1);
            if (v.endsWith(")")) v = v.substring(0, v.length() - 1);
            int ix = v.indexOf(',');
            String latStr = v.substring(0, ix);
            String lonStr = v.substring(ix + 1);
            cb.setLocationSchedule(latStr.trim(), lonStr.trim());
          } else {
            if (attributeValue.getClass().isArray()) {
              String[] rv = (String[]) attributeValue;
              Object[] pv = new Object[rv.length];
              for (int k = 0; k < rv.length; k++) {
                pv[k] = cb.parseExpr(attributeType, rv[k]);
              }
              Object[] args = {Arrays.asList(pv)};
              cb.callSetter("set" + attributeName, "Collection", args);
            } else {
              Object[] args = {cb.parseExpr(attributeType, (String) attributeValue)};
              cb.callSetter("set" + attributeName, cb.getType(attributeType), args);
            }
          }
        }
      }
      String[][] relationships = initializerService.getAgentRelationships(cId);
      for (int i = 0; i < relationships.length; i++) {
        String[] r = relationships[i];
        long start = cb.getDefaultStartTime();
        long end = cb.getDefaultEndTime();
        try {
          start = cb.parseDate(r[4]);
        } catch (java.text.ParseException pe) {
          System.err.println("Unable to parse: "
                             + r[4]
                             + ". Start time defaulting to "
                             + start);
        }
        try {
          if (r[5] != null) {
            end = cb.parseDate(r[5]);
          }
        } catch (java.text.ParseException pe) {
          System.err.println("Unable to parse: "
                             + r[5]
                             + ". End time defaulted to "
                             + end);
        }
        cb.addRelationship(r[2],     // Type id
                           r[1],     // Item id
                           r[3],     // Other cluster
                           r[0],     // Role
                           start,    // Start time
                           end);     // End time
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }
}
