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

import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.ConfigFinder;
import java.text.ParseException;

public interface AssetDataCallback {
    ConfigFinder getConfigFinder();
    void createMyLocalAsset(String assetClassName);
    boolean hasMyLocalAsset();
  void createPropertyGroup(String propertyName) throws Exception;
    Object parseExpr(String dataType, String value);
    long parseDate(String dateString) throws ParseException;
    String getType(String type);
    void callSetter(String setterName, String type, Object[] arguments);
    void setLocationSchedule(String latStr, String lonStr);
    long getDefaultStartTime();
    long getDefaultEndTime();
  void addPropertyToAsset();
    void addRelationship(String typeId, String itemId,
                         String otherClusterId, String roleName,
                         long start, long end);
}
