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

package org.cougaar.core.node;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Map;
import org.cougaar.core.component.Service;

/**
 * Database initializer service API.
 */
public interface DBInitializerService extends Service {

  Map createSubstitutions();

  String getNonNullString(ResultSet rs, int ix, String query)
    throws SQLException;

  String getQuery(String queryName, Map substitutions);

  Connection getConnection() throws SQLException;

  ResultSet executeQuery(Statement stmt, String query) throws SQLException;

  /**
   * Translate the value of a "query" attribute type. The "key"
   * should be one or more query substitutions. Each substitution is
   * an equals separated key and value. Multiple substitutions are
   * separated by semi-colon. Backslash can quote a character. The
   * query may be in a different database. If so, then the dbp
   * should contain properties named by concatenating the query
   * name with .database, .username, .password describing the
   * database to connect to.
   * @param type is the "data type" of the attribute value and
   * names a query that should be done to obtain the actual
   * value. 
   * @return a two-element array of attribute type and value.
   **/
  Object[] translateAttributeValue(
      String type, String key) throws SQLException;

}
