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

package org.cougaar.core.persist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.core.service.LoggingService;

/**
 * This persistence class saves plan objects in a database. It saves and
 * restores persistence deltas in RDB tables.
 *
 * We store the deltas for each cluster in a separate table named
 * after the cluster: delta_<clustername>. The table has three columns:
 * seqno  -- has an INTEGER delta sequence number
 * active -- has a CHAR indicating the kind of delta stored
 *           x -- full delta (all data)
 *           t -- incremental delta
 *           a -- archive delta (full but not active)
 *           f -- inactive incremental
 *   The above codes were selected for backward compatibility with the
 *   former t/f meaning active was true/false
 * data   -- has a LONG RAW with the serialized data
 * @property org.cougaar.core.persistence.database.url Specify the database to use for DatabasePersistence.
 * @property org.cougaar.core.persistence.database.user Specify the database user to use for DatabasePersistence.
 * @property org.cougaar.core.persistence.database.password Specify the database password to use for DatabasePersistence.
 * @property org.cougaar.core.persistence.database.driver Specify the database driver to use for DatabasePersistence.
 **/
public class DatabasePersistence
  extends PersistencePluginAdapter
  implements PersistencePlugin
{
  // Codes used in the active column. Chosen for backward compatibility
  private static final String INCREMENTAL = "'t'";
  private static final String FULL        = "'x'";
  private static final String INACTIVE    = "'f'";
  private static final String ARCHIVE     = "'a'";

  String databaseURL =
    System.getProperty("org.cougaar.core.persistence.database.url");
  String databaseUser =
    System.getProperty("org.cougaar.core.persistence.database.user");
  String databasePassword =
    System.getProperty("org.cougaar.core.persistence.database.password");
  String databaseDriver =
    System.getProperty("org.cougaar.core.persistence.database.driver");

  private Connection theConnection;
  private DatabaseMetaData theMetaData;
  private PreparedStatement getSequenceNumbers;
  private PreparedStatement getArchiveSequenceNumbers;
  private PreparedStatement putSequenceNumbers1;
  private PreparedStatement putSequenceNumbers2;
  private PreparedStatement storeDelta;
  private PreparedStatement getDelta;
  private PreparedStatement checkDelta;
  private PreparedStatement cleanDeltas;
  private String deltaTable;

  public void init(PersistencePluginSupport pps, String name, String[] params, boolean deleteOldPersistence)
    throws PersistenceException
  {
    init(pps, name);
    String clusterName = pps.getMessageAddress().getAddress().replace('-', '_');
    LoggingService ls = pps.getLoggingService();
    deltaTable = "delta_" + clusterName;
    if (databaseDriver != null) {
      try {
        Class.forName(databaseDriver);
      } catch (Exception e) {
        fatalException(e);
      }
    }
    try {
      theConnection =
        DriverManager.getConnection(databaseURL,
                                    databaseUser,
                                    databasePassword);
      theMetaData = theConnection.getMetaData();
      if (theMetaData.supportsTransactions()) {
        theConnection.setAutoCommit(false);
      } else {
        ls.error("Warning!!!! Persistence Database does not support transactions");
      }
      ls.debug("Database transaction isolation is " +
               theConnection.getTransactionIsolation());
      getSequenceNumbers = theConnection.prepareStatement
        ("select count(seqno), min(seqno), max(seqno)+1, max(timestamp) from " + deltaTable +
         " where active =" + FULL + " or active = " + INCREMENTAL);
      getArchiveSequenceNumbers = theConnection.prepareStatement
        ("select seqno, timestamp from " + deltaTable +
         " where active =" + ARCHIVE);
      putSequenceNumbers1 = theConnection.prepareStatement
        ("update " + deltaTable +
         " set active = " + INACTIVE + " where active = " + INCREMENTAL + " and (seqno < ? or seqno >= ?)");
      putSequenceNumbers2 = theConnection.prepareStatement
        ("update " + deltaTable +
         " set active = " + ARCHIVE + " where active = " + FULL + " and (seqno < ? or seqno >= ?)");
      storeDelta = theConnection.prepareStatement
        ("insert into " + deltaTable +
         "(seqno, active, timestamp, data) values (?, ?, ?, ?)");
      getDelta = theConnection.prepareStatement
        ("select data from " + deltaTable +
         " where seqno = ?");
      checkDelta = theConnection.prepareStatement
        ("select timestamp from " + deltaTable +
         " where seqno = ? and (active = " + FULL + ") or (active = " + ARCHIVE + ")");
      cleanDeltas = theConnection.prepareStatement
        ("delete from " + deltaTable +
         " where "
         + (pps.archivingEnabled()
            ? ("active = " + INACTIVE)
            : ("active in (" + INACTIVE + "," + ARCHIVE + ")"))
         + " and seqno >= ? and seqno < ?");
      try {
        ResultSet rs = getSequenceNumbers.executeQuery();
        rs.close();
      }
      catch (SQLException e) {
        createTable(deltaTable);
      }
      if (deleteOldPersistence) deleteOldPersistence();
    }
    catch (SQLException e) {
      ls.error("Persistence connection error");
      ls.error("     URL: " + databaseURL);
      ls.error("    User: " + databaseUser);
      ls.error("Password: " + databasePassword);
      ls.error(" Drivers:");
      for (Enumeration drivers = DriverManager.getDrivers(); drivers.hasMoreElements(); ) {
        ls.error("     " + drivers.nextElement().getClass().getName());
      }
      fatalException(e);
    }
  }

  private void createTable(String tableName) throws SQLException {
    String intDef = "NUMBER";
    String longBinaryDef = "LONG RAW";
    String qry = "create table " + tableName
      + "(seqno "
      + intDef
      + " primary key,"
      + " active char(1),"
      + " data "
      + longBinaryDef
      + ")";
    pps.getLoggingService().info("Creating table: " + qry);
    Statement stmt = theConnection.createStatement();
    stmt.executeUpdate(qry);
  }

  public SequenceNumbers[] readSequenceNumbers(String suffix) {
    if (!suffix.equals("")) {
      if (suffix.startsWith("_")) suffix = suffix.substring(1);
      try {
        int deltaNumber = Integer.parseInt(suffix);
        checkDelta.setInt(1, deltaNumber);
        ResultSet rs = checkDelta.executeQuery();
        if (!rs.next()) throw new IllegalArgumentException("Delta " + deltaNumber + " does not exist");
        long timestamp = rs.getLong(1);
        return new SequenceNumbers[] {new SequenceNumbers(deltaNumber, deltaNumber + 1, timestamp)};
      } catch (Exception e) {
        fatalException(e);
      }
    }
    try {
      List results = new ArrayList();
      ResultSet rs = getSequenceNumbers.executeQuery();
      try {
        if (rs.next()) {
          int count = rs.getInt(1);
          if (count >= 0) {
            int first = rs.getInt(2);
            int last = rs.getInt(3);
            long timestamp = rs.getLong(4);
            results.add(new SequenceNumbers(first, last, timestamp));
          }
        }
      } finally {
        rs.close();
      }
      rs = getArchiveSequenceNumbers.executeQuery();
      try {
        while (rs.next()) {
          int seqno = rs.getInt(1);
          long timestamp = rs.getLong(2);
          results.add(new SequenceNumbers(seqno, seqno + 1, timestamp));
        }
      } finally {
        rs.close();
      }
      return (SequenceNumbers[]) results.toArray(new SequenceNumbers[results.size()]);
    }
    catch (SQLException e) {
      fatalException(e);
      return new SequenceNumbers[0];
    }
  }

  private void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
    try {
      putSequenceNumbers1.setInt(1, sequenceNumbers.first);
      putSequenceNumbers1.setInt(2, sequenceNumbers.current);
      putSequenceNumbers1.executeUpdate();
      putSequenceNumbers2.setInt(1, sequenceNumbers.first);
      putSequenceNumbers2.setInt(2, sequenceNumbers.current);
      putSequenceNumbers2.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  public void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
    try {
      cleanDeltas.setInt(1, cleanupNumbers.first);
      cleanDeltas.setInt(2, cleanupNumbers.current);
      cleanDeltas.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  private void writeDelta(int seqno, InputStream is, int length, boolean full) {
    try {
      storeDelta.setInt(1, seqno);
      storeDelta.setString(2, full ? "x" : "t");
      storeDelta.setLong(3, System.currentTimeMillis());
      storeDelta.setBinaryStream(4, is, length);
      storeDelta.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  // consider replacing superclass with 
  //   org.cougaar.util.LinkedByteOutputStream
  private class MyOutputStream extends ByteArrayOutputStream {
    private int deltaNumber;
    private boolean full;

    public MyOutputStream(int deltaNumber, boolean full) {
      super(8192);
      this.deltaNumber = deltaNumber;
      this.full = full;
    }

    public void close() throws IOException {
      final int cnt = count;
      final byte[] bfr = buf;
      InputStream is = new InputStream() {
        int n = 0;
        public int read() {
          if (n >= cnt) return -1;
          return bfr[n++];
        }
        public int read(byte[] rbuf) {
          return read(rbuf, 0, rbuf.length);
        }
        public int read(byte[] rbuf, int offset, int len) {
          len= Math.min(len, cnt - n);
          if (len == 0) return -1;
          System.arraycopy(bfr, n, rbuf, offset, len);
          n += len;
          return len;
        }
      };
      writeDelta(deltaNumber, is, cnt, full);
      super.close();
      try {
        theConnection.commit();
      }
      catch (SQLException e) {
        fatalException(e);
      }
      releaseDatabaseConnection(DatabasePersistence.this);
    }
  }

  public OutputStream openOutputStream(final int deltaNumber,
                                       final boolean full)
    throws IOException
  {
    getDatabaseConnection(this);
    return new MyOutputStream(deltaNumber, full);
  }

  public void finishOutputStream(SequenceNumbers retainNumbers,
                                 boolean full)
  {
    writeSequenceNumbers(retainNumbers);
  }

  public void abortOutputStream(SequenceNumbers retainNumbers)
  {
    // Nothing to do since we haven't written to the db yet
    // We just abandon the streams.
  }

  public InputStream openInputStream(int deltaNumber)
    throws IOException
  {
    try {
      getDelta.setInt(1, deltaNumber);
      final ResultSet rs = getDelta.executeQuery();
      try {
        if (rs.next()) {
          return new ByteArrayInputStream(rs.getBytes(1));
        } else {
          throw new SQLException("Delta not found");
        }
      }
      finally {
        rs.close();
      }
    }
    catch (SQLException e) {
      fatalException(e);
      return null;
    }
  }

  public void finishInputStream(int deltaNumber)
  {
    // Nothing to do, just quietly abandon the input stream
  }

  private void deleteOldPersistence() {
    try {
      Statement stmt = theConnection.createStatement();
      stmt.executeUpdate("delete from " + deltaTable);
    }
    catch (SQLException se) {
      fatalException(se);
    }
  }

  public void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException
  {
    throw new IOException("storeDataProtectionKey unimplemented for database persistence");
  }

  public DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException
  {
    throw new IOException("retrieveDataProtectionKey unimplemented for database persistence");
  }

  private void fatalException(Exception e) {
    pps.getLoggingService().fatal("Fatal database persistence exception", e);
    System.exit(13);
  }

  private static class EqWrapper {
    Object theObject;
    int theHashCode;
    public EqWrapper(Object anObject) {
      theObject = anObject;
      theHashCode = System.identityHashCode(theObject);
    }
    public int hashCode() {
      return theHashCode;
    }
    public boolean equals(Object o) {
      return ((EqWrapper) o).theObject == theObject;
    }
  }

  private Object connectionLock = new Object();
  private EqWrapper connectionLocker = null;
  private WrappedConnection activeConnection;
  private HashMap wrappedConnections = new HashMap();

  /**
   * Override adapter version since we actually have a database
   * connection we can return instead of throwing an exception.
   **/
  public Connection getDatabaseConnection(Object locker) {
    if (locker == null) throw new IllegalArgumentException("locker is null");
    synchronized (connectionLock) {
      if (connectionLocker != null) {
        if (locker == connectionLocker.theObject) throw new IllegalArgumentException("reentrant locker");
        while (connectionLocker != null) {
          try {
            connectionLock.wait(10000);
          }
          catch (InterruptedException ie) {
          }
        }
      }
      connectionLocker = new EqWrapper(locker);
      activeConnection = (WrappedConnection) wrappedConnections.get(connectionLocker);
      if (activeConnection == null) {
        activeConnection = new WrappedConnection(theConnection);
        wrappedConnections.put(connectionLocker, activeConnection);
      }
      activeConnection.setActive(true);
      return activeConnection;
    }
  }
  public void releaseDatabaseConnection(Object locker) {
    synchronized (connectionLock) {
      if (locker != connectionLocker.theObject) {
        throw new IllegalArgumentException("locker mismatch " +
                                           connectionLocker.theObject);
      }
      activeConnection.setActive(false);
      connectionLocker = null;
      connectionLock.notify();
    }
  }
  
  /**
   * This is a wrapper for a Connection object that delegates most
   * functions to the wrapped object, but interposes some processing
   * of its own to keep track of operations that have been done to the
   * connection. Mainly, we disallow closing, committing, or rolling
   * back the connection. Additionally, we can keep track of open
   * statements to guard against an unreasonable accumulation of them
   * (indicating a failure to close the statement.
   **/
  class WrappedConnection implements Connection {
    Connection c;
    boolean active = false;
    Vector statements = new Vector();
    WrappedConnection(Connection realConnection) {
      c = realConnection;
    }
    
    void closeStatement(WrappedStatement statement) throws SQLException {
      synchronized (statements) {
        statement.theStatement.close();
        statements.removeElement(statement);
      }
    }
    void addStatement(Statement statement) throws SQLException {
      if (statements.size() > 20) throw new SQLException("Too many statements");
      statements.add(statement);
    }
    void setActive(boolean newActive) {
      active = newActive;
    }
    public Statement createStatement() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      Statement statement = new WrappedStatement(c.createStatement());
      addStatement(statement);
      return statement;
    }
    public Statement createStatement(int a, int b) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      Statement statement = new WrappedStatement(c.createStatement(a, b));
      addStatement(statement);
      return statement;
    }
    public PreparedStatement prepareStatement(String sql) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      PreparedStatement statement = (PreparedStatement)new WrappedPreparedStatement(c.prepareStatement(sql));
      addStatement(statement);
      return statement;
    }
    public PreparedStatement prepareStatement(String sql, int a, int b) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      PreparedStatement statement = (PreparedStatement)new WrappedPreparedStatement(c.prepareStatement(sql, a, b));
      addStatement(statement);
      return statement;
    }
    public CallableStatement prepareCall(String sql) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      CallableStatement statement = new WrappedCallableStatement(c.prepareCall(sql));
      addStatement(statement);
      return statement;
    }
    public CallableStatement prepareCall(String sql, int a, int b) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      CallableStatement statement = new WrappedCallableStatement(c.prepareCall(sql, a, b));
      addStatement(statement);
      return statement;
    }
    public String nativeSQL(String sql) throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.nativeSQL(sql);
    }
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      throw new SQLException("setAutoCommit disallowed");
    }
    public boolean getAutoCommit() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.getAutoCommit();
    }
    public void commit() throws SQLException {
      throw new SQLException("commit disallowed");
    }
    public void rollback() throws SQLException {
      throw new SQLException("rollback disallowed");
    }
    public void close() throws SQLException {
      throw new SQLException("close disallowed");
    }
    public boolean isClosed() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return false;
    }
    public DatabaseMetaData getMetaData() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.getMetaData();
    }
    public void setReadOnly(boolean readOnly) throws SQLException {
      throw new SQLException("setReadOnly disallowed");
    }
    public boolean isReadOnly() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.isReadOnly();
    }
    public void setCatalog(String catalog) throws SQLException {
      throw new SQLException("setCatalog disallowed");
    }
    public String getCatalog() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return getCatalog();
    }
    public void setTransactionIsolation(int level) throws SQLException {
      throw new SQLException("setTransactionIsolation disallowed");
    }
    public int getTransactionIsolation() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.getTransactionIsolation();
    }
    public SQLWarning getWarnings() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.getWarnings();
    }
    public void clearWarnings() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      c.clearWarnings();
    }
    public java.util.Map getTypeMap() throws SQLException {
      if (!active) throw new SQLException("getDatabaseConnection not called");
      return c.getTypeMap();
    }
    public void setTypeMap(java.util.Map map) throws SQLException {
      throw new SQLException("setTypeMap disallowed");
    }

    // begin jdk1.4 compatability
    public void setHoldability(int holdability) throws SQLException {
      throw new SQLException("setHoldability disallowed");      
    }
    public int getHoldability() throws SQLException {
      throw new SQLException("setHoldability disallowed");      
    }
    public Savepoint setSavepoint() throws SQLException {
      throw new SQLException("setSavepoint disallowed");      
    }
    public Savepoint setSavepoint(String s) throws SQLException {
      throw new SQLException("setSavepoint(String) disallowed");      
    }
    public void rollback(Savepoint savepoint) throws SQLException {
      throw new SQLException("rollback disallowed");      
    }
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
      throw new SQLException("releaseSavepoint disallowed");      
    }
    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability)
      throws SQLException
    {
      throw new SQLException("createStatement(int,int,int) disallowed");      
    }
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
      throws SQLException
    {
      throw new SQLException("prepareStatement(String,int,int,int) disallowed");      
    }
    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
      throws SQLException
    {
      throw new SQLException("prepareCall(String,int,int,int) disallowed");      
    }
    public PreparedStatement prepareStatement(String sql,
                                              int autoGenerateKeys)
      throws SQLException
    {
      throw new SQLException("prepareStatement(String,int) disallowed");      
    }
    public PreparedStatement prepareStatement(String sql,
                                              int ci[] )
      throws SQLException
    {
      throw new SQLException("prepareStatement(String,int[]) disallowed");      
    }
    public PreparedStatement prepareStatement(String sql,
                                              String cn[])
      throws SQLException
    {
      throw new SQLException("prepareStatement(String,String[]) disallowed");      
    }
    // end jdk1.4 compatability

    /**
     * A wrapper for a Statement object. Most operations are
     * delegated to the wrapped object. The close operation goes
     * through the WrappedConnection wrapper to keep track of which
     * statements have been closed and which haven't.
     */
    class WrappedStatement implements java.sql.Statement {
      java.sql.Statement theStatement;
      public WrappedStatement(java.sql.Statement theStatement) {
        this.theStatement = theStatement;
      }
      public void addBatch(String sql)  throws java.sql.SQLException {
        theStatement.addBatch(sql);
      }
      public void clearBatch() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.clearBatch();
      }
      public int[] executeBatch()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.executeBatch();
      }
      public Connection getConnection()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return WrappedConnection.this;
      }
      public int getFetchDirection()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getFetchDirection();
      }
      public int getFetchSize()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getFetchSize();
      }
      public int getResultSetConcurrency()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getResultSetConcurrency();
      }
      public int getResultSetType()  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getResultSetType();
      }
      public void setFetchDirection( int direction )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setFetchDirection( direction );
      }
      public void setFetchSize( int rows )  throws java.sql.SQLException  {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setFetchSize( rows );
      }
      public java.sql.ResultSet executeQuery(java.lang.String arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.executeQuery(arg0);
      }
      public int executeUpdate(java.lang.String arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.executeUpdate(arg0);
      }
      public void close() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        closeStatement(this);
      }
      public int getMaxFieldSize() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getMaxFieldSize();
      }
      public void setMaxFieldSize(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setMaxFieldSize(arg0);
      }
      public int getMaxRows() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getMaxRows();
      }
      public void setMaxRows(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setMaxRows(arg0);
      }
      public void setEscapeProcessing(boolean arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setEscapeProcessing(arg0);
      }
      public int getQueryTimeout() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getQueryTimeout();
      }
      public void setQueryTimeout(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setQueryTimeout(arg0);
      }
      public void cancel() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.cancel();
      }
      public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getWarnings();
      }
      public void clearWarnings() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.clearWarnings();
      }
      public void setCursorName(java.lang.String arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theStatement.setCursorName(arg0);
      }
      public boolean execute(java.lang.String arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.execute(arg0);
      }
      public java.sql.ResultSet getResultSet() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getResultSet();
      }
      public int getUpdateCount() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getUpdateCount();
      }
      public boolean getMoreResults() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theStatement.getMoreResults();
      }

      // begin jdk 1.4 compatability
      public boolean getMoreResults(int current) throws java.sql.SQLException {
        throw new SQLException("getMoreResults(int) disallowed");
      }
      public ResultSet getGeneratedKeys() throws java.sql.SQLException {
        throw new SQLException("getGeneratedKeys() disallowed");
      }
      public int executeUpdate(String sql, int agk) throws java.sql.SQLException {
        throw new SQLException("executeUpdate(String,int) disallowed");
      }
      public int executeUpdate(String sql, int ci[]) throws java.sql.SQLException { 
        throw new SQLException("executeUpdate(String,int[]) disallowed");
      }
      public int executeUpdate(String sql, String cn[]) throws java.sql.SQLException { 
        throw new SQLException("executeUpdate(String,String[]) disallowed");
      }
      public boolean execute(String sql, int agk)  throws java.sql.SQLException {
        throw new SQLException("execute(String,int) disallowed");
      }
      public boolean execute(String sql, int ci[])  throws java.sql.SQLException {
        throw new SQLException("execute(String,int[]) disallowed");
      }
      public boolean execute(String sql, String cn[])  throws java.sql.SQLException {
        throw new SQLException("execute(String,String[]) disallowed");
      }
      public int getResultSetHoldability() throws java.sql.SQLException {
        throw new SQLException("getResultSetHoldability() disallowed");
      }
      // end jdk 1.4 compatability

    }
    /**
     * A wrapper for a PreparedStatement object. All operations are
     * delegated to the wrapped object. The close operation in the
     * base class goes through the WrappedConnection wrapper to keep
     * track of which statements have been closed and which haven't.
     */
    class WrappedPreparedStatement extends WrappedStatement implements java.sql.PreparedStatement {
      private java.sql.PreparedStatement thePreparedStatement;
      public WrappedPreparedStatement(java.sql.PreparedStatement thePreparedStatement) {
        super(thePreparedStatement);
        this.thePreparedStatement = thePreparedStatement;
      }
      public void addBatch() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.addBatch();
      }
      public ResultSetMetaData getMetaData() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( thePreparedStatement.getMetaData() );
      }
      public void setArray( int i, Array x ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setArray( i, x );
      }
      public void setBlob( int i, Blob x ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setBlob( i, x );
      }
      public void setCharacterStream( int paramIndex, java.io.Reader reader, int length ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setCharacterStream( paramIndex, reader, length );
      }
      public void setClob( int i, Clob x ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setClob( i,x );
      }
      public void setRef( int i, Ref x )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setRef( i, x );
      }
      public void setDate( int i, Date myDate, Calendar cal ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setDate( i, myDate, cal );
      }
      public void setTime( int paramIndex, Time x, Calendar cal ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setTime( paramIndex, x, cal );
      }
      public void setTimestamp( int paramIndex, java.sql.Timestamp x, Calendar cal ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setTimestamp( paramIndex, x ,cal );
      }
      public java.sql.ResultSet executeQuery() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return thePreparedStatement.executeQuery();
      }
      public int executeUpdate() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return thePreparedStatement.executeUpdate();
      }
      public void setNull(int arg0, int arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setNull(arg0, arg1);
      }
      public void setNull(int arg0, int arg1, String typeName ) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setNull(arg0, arg1, typeName);
      }
      public void setBoolean(int arg0, boolean arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setBoolean(arg0, arg1);
      }
      public void setByte(int arg0, byte arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setByte(arg0, arg1);
      }
      public void setShort(int arg0, short arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setShort(arg0, arg1);
      }
      public void setInt(int arg0, int arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setInt(arg0, arg1);
      }
      public void setLong(int arg0, long arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setLong(arg0, arg1);
      }
      public void setFloat(int arg0, float arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setFloat(arg0, arg1);
      }
      public void setDouble(int arg0, double arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setDouble(arg0, arg1);
      }
      public void setBigDecimal(int arg0, java.math.BigDecimal arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setBigDecimal(arg0, arg1);
      }
      public void setString(int arg0, java.lang.String arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setString(arg0, arg1);
      }
      public void setBytes(int arg0, byte[] arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setBytes(arg0, arg1);
      }
      public void setDate(int arg0, java.sql.Date arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setDate(arg0, arg1);
      }
      public void setTime(int arg0, java.sql.Time arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setTime(arg0, arg1);
      }
      public void setTimestamp(int arg0, java.sql.Timestamp arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setTimestamp(arg0, arg1);
      }
      public void setAsciiStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setAsciiStream(arg0, arg1, arg2);
      }
      /**
       * @deprecated in the original
       **/
      public void setUnicodeStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        throw new java.sql.SQLException("Method not supported");
        //	  thePreparedStatement.setUnicodeStream(arg0, arg1, arg2);
      }
      public void setBinaryStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setBinaryStream(arg0, arg1, arg2);
      }
      public void clearParameters() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.clearParameters();
      }
      public void setObject(int arg0, java.lang.Object arg1, int arg2, int arg3) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setObject(arg0, arg1, arg2, arg3);
      }
      public void setObject(int arg0, java.lang.Object arg1, int arg2) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setObject(arg0, arg1, arg2);
      }
      public void setObject(int arg0, java.lang.Object arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        thePreparedStatement.setObject(arg0, arg1);
      }
      public boolean execute() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return thePreparedStatement.execute();
      }
      // begin jdk 1.4 compatability
      public void setURL(int param, URL x) throws java.sql.SQLException {
        throw new SQLException("setURL(int,URL) disallowed");
      }
      public ParameterMetaData getParameterMetaData() throws java.sql.SQLException {
        throw new SQLException("getParameterMetaData() disallowed");
      }
      // end jdk 1.4 compatability
    }
    /**
     * A wrapper for a CallableStatement object. All operations are
     * delegated to the wrapped object. The close operation in the
     * base class goes through the WrappedConnection wrapper to keep
     * track of which statements have been closed and which haven't.
     */
    class WrappedCallableStatement extends WrappedPreparedStatement implements java.sql.CallableStatement {
      private java.sql.CallableStatement theCallableStatement;
      public WrappedCallableStatement(java.sql.CallableStatement theCallableStatement) {
        super(theCallableStatement);
        this.theCallableStatement = theCallableStatement;
      }
      public Array getArray( int i )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getArray( i ) );
      }
      /**
       * @deprecated in the original
       **/
      public java.math.BigDecimal getBigDecimal(int paramIndex)  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getBigDecimal( paramIndex ) );
      }
      public Blob getBlob( int i )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getBlob( i ) );
      }
      public Clob getClob( int i )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getClob( i ) );
      }
      public Date getDate( int paramIndex, Calendar cal )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getDate( paramIndex, cal ) );
      }
      public Object getObject( int i, Map map )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getObject ( i, map ) );
      }
      public Ref getRef ( int i )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return ( theCallableStatement.getRef( i ) );
      }
      public Time getTime ( int paramIndex, Calendar cal )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return (theCallableStatement.getTime ( paramIndex, cal ) );
      }
      public Timestamp getTimestamp( int paramIndex, Calendar cal )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return (theCallableStatement.getTimestamp( paramIndex, cal ) );
      }
      public void registerOutParameter( int paramIndex, int sqlType, String typeName )  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter( paramIndex, sqlType, typeName );
      }
      public void registerOutParameter(int arg0, int arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter(arg0, arg1);
      }
      public void registerOutParameter(int arg0, int arg1, int arg2) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter(arg0, arg1, arg2);
      }
      public boolean wasNull() throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.wasNull();
      }
      public java.lang.String getString(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getString(arg0);
      }
      public boolean getBoolean(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBoolean(arg0);
      }
      public byte getByte(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getByte(arg0);
      }
      public short getShort(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getShort(arg0);
      }
      public int getInt(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getInt(arg0);
      }
      public long getLong(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getLong(arg0);
      }
      public float getFloat(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getFloat(arg0);
      }
      public double getDouble(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getDouble(arg0);
      }
      /** @deprecated in the original **/
      public java.math.BigDecimal getBigDecimal(int arg0, int arg1) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        throw new java.sql.SQLException("Method not supported");
        //	  return theCallableStatement.getBigDecimal(arg0, arg1);
      }
      public byte[] getBytes(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBytes(arg0);
      }
      public java.sql.Date getDate(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getDate(arg0);
      }
      public java.sql.Time getTime(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTime(arg0);
      }
      public java.sql.Timestamp getTimestamp(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTimestamp(arg0);
      }
      public java.lang.Object getObject(int arg0) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getObject(arg0);
      }
      // begin jdk 1.4 compatability
      public void registerOutParameter(String pn, int sqltype) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter(pn, sqltype);
      }
      public void registerOutParameter(String pn, int sqltype, int scale) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter(pn, sqltype,scale);
      }
      public void registerOutParameter(String pn, int sqltype, String tn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.registerOutParameter(pn, sqltype,tn);
      }
      public URL getURL(int pi)  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getURL(pi);
      }
      public void setURL(String pn, URL v)  throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setURL(pn,v);
      }
      public void setNull(String pn, int sqlt) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setNull(pn,sqlt);
      }
      public void setBoolean(String pn, boolean x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setBoolean(pn, x);
      }
      public void setByte(String pn, byte x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setByte(pn, x);
      }
      public void setShort(String pn, short x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setShort(pn,x);
      }
      public void setInt(String pn, int x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setInt(pn,x);
      }
      public void setLong(String pn, long x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setLong(pn,x);
      }
      public void setFloat(String pn, float x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setFloat(pn,x);
      }
      public void setDouble(String pn, double x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setDouble(pn,x);
      }
      public void setBigDecimal(String pn, BigDecimal x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setBigDecimal(pn,x);
      }
      public void setString(String pn, String x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setString(pn, x);
      }
      public void setBytes(String pn, byte[] x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setBytes(pn,x);
      }
      public void setDate(String pn, Date x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setDate(pn,x);
      }
      public void setTime(String pn, Time x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setTime(pn,x);
      }
      public void setTimestamp(String pn, Timestamp x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setTimestamp(pn,x);
      }
      public void setAsciiStream(String pn, InputStream x, int l) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setAsciiStream(pn,x,l);
      }
      public void setBinaryStream(String pn, InputStream x, int l) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setBinaryStream(pn,x,l);
      }
      public void setObject(String pn, Object x, int tt,int s) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setObject(pn,x,tt,s);
      }
      public void setObject(String pn, Object x, int tt) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setObject(pn,x,tt);
      }
      public void setObject(String pn, Object x) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setObject(pn,x);
      }
      public void setCharacterStream(String pn, Reader x, int l) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setCharacterStream(pn,x,l);
      }
      public void setDate(String pn, Date x, Calendar cal) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setDate(pn,x,cal);
      }
      public void setTime(String pn, Time x, Calendar cal) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setTime(pn,x,cal);
      }
      public void setTimestamp(String pn, Timestamp x, Calendar cal) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setTimestamp(pn,x,cal);
      }
      public void setNull(String pn, int st, String tn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        theCallableStatement.setNull(pn,st,tn);
      }
      public String getString(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getString(pn);
      }
      public boolean getBoolean(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBoolean(pn);
      }
      public byte getByte(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getByte(pn);
      }
      public short getShort(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getShort(pn);
      }
      public int getInt(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getInt(pn);
      }
      public long getLong(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getLong(pn);
      }
      public float getFloat(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getFloat(pn);
      }
      public double getDouble(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getDouble(pn);
      }
      public byte[] getBytes(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBytes(pn);
      }
      public Date getDate(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getDate(pn);
      }
      public Time getTime(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTime(pn);
      }
      public Timestamp getTimestamp(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTimestamp(pn);
      }
      public Object getObject(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getObject(pn);
      }
      public BigDecimal getBigDecimal(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBigDecimal(pn);
      }
      public Object getObject(String pn,Map m) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getObject(pn,m);
      }
      public Ref getRef(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getRef(pn);
      }
      public Blob getBlob(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getBlob(pn);
      }
      public Clob getClob(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getClob(pn);
      }
      public Array getArray(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getArray(pn);
      }
      public Date getDate(String pn,Calendar c) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getDate(pn,c);
      }
      public Time getTime(String pn,Calendar c) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTime(pn,c);
      }
      public Timestamp getTimestamp(String pn,Calendar c) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getTimestamp(pn,c);
      }
      public URL getURL(String pn) throws java.sql.SQLException {
        if (!active) throw new SQLException("getDatabaseConnection not called");
        return theCallableStatement.getURL(pn);
      }
      // end jdk 1.4 compatability
    }
  }
}
