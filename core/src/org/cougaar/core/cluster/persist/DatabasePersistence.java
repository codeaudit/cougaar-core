/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.cluster.Envelope;
import org.cougaar.core.cluster.EnvelopeTuple;
import org.cougaar.core.cluster.PersistenceEnvelope;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.domain.planning.ldm.plan.Plan;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLWarning;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.Clob;
import java.sql.Blob;
import java.sql.Ref;
import java.sql.Time;
import java.sql.Types;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Array;
import java.sql.CallableStatement;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Map;
import java.util.Calendar;

/**
 * This persistence class saves plan objects in a database. It saves and
 * restores persistence deltas in RDB tables.
 *
 * We store the deltas for each cluster in a separate table named
 * after the cluster: delta_<clustername>. The table has three columns:
 * seqno  -- has an INTEGER delta sequence number
 * active -- has a BOOLEAN indicating that the delta is still active
 * data   -- has a LONG RAW with the serialized data
 **/
public class DatabasePersistence
  extends BasePersistence
  implements Persistence
{
  String databaseURL =
    System.getProperty("org.cougaar.core.cluster.persistence.database.url");
  String databaseUser =
    System.getProperty("org.cougaar.core.cluster.persistence.database.user");
  String databasePassword =
    System.getProperty("org.cougaar.core.cluster.persistence.database.password");
  String databaseDriver =
    System.getProperty("org.cougaar.core.cluster.persistence.database.driver");

  private Connection theConnection;

  private DatabaseMetaData theMetaData;

  private File persistenceDirectory;


  public static Persistence find(final ClusterContext clusterContext)
    throws PersistenceException
  {
    return BasePersistence.findOrCreate(clusterContext,
                                        new PersistenceCreator() {
      public BasePersistence create() throws PersistenceException {
	return new DatabasePersistence(clusterContext);
      }
    });
  }

  private PreparedStatement getSequenceNumbers;
  private PreparedStatement putSequenceNumbers;
  private PreparedStatement storeDelta;
  private PreparedStatement getDelta;
  private PreparedStatement cleanDeltas;
  private String deltaTable;

  private DatabasePersistence(ClusterContext clusterContext)
    throws PersistenceException
  {
    super(clusterContext);
    String clusterName = clusterContext.getClusterIdentifier().getAddress().replace('-', '_');
    deltaTable = "delta_" + clusterName;
    persistenceDirectory = new File(FilePersistence.persistenceRoot, clusterName);
    if (!persistenceDirectory.isDirectory()) {
      if (!persistenceDirectory.mkdirs()) {
	throw new PersistenceException("Not a directory: " + persistenceDirectory);
      }
    }
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
        System.err.println("Warning!!!! Persistence Database does not support transactions");
      }
      System.out.println("Database transaction isolation is " +
                         theConnection.getTransactionIsolation());
      getSequenceNumbers = theConnection.prepareStatement
        ("select count(seqno), min(seqno), max(seqno)+1 from " + deltaTable +
         " where active ='t'");
      putSequenceNumbers = theConnection.prepareStatement
        ("update " + deltaTable +
         " set active = 'f' where seqno < ? or seqno >= ?");
      storeDelta = theConnection.prepareStatement
        ("insert into " + deltaTable +
         "(seqno, active, data) values (?, ?, ?)");
      getDelta = theConnection.prepareStatement
        ("select data from " + deltaTable +
         " where seqno = ?");
      cleanDeltas = theConnection.prepareStatement
        ("delete from " + deltaTable +
         " where seqno >= ? and seqno < ?");
      try {
        ResultSet rs = getSequenceNumbers.executeQuery();
        rs.close();
      }
      catch (SQLException e) {
        createTable(deltaTable);
      }
    }
    catch (SQLException e) {
      System.err.println("Persistence connection error");
      System.err.println("     URL: " + databaseURL);
      System.err.println("    User: " + databaseUser);
      System.err.println("Password: " + databasePassword);
      System.err.println(" Drivers:");
      for (Enumeration drivers = DriverManager.getDrivers(); drivers.hasMoreElements(); ) {
        System.err.println("     " + drivers.nextElement().getClass().getName());
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
    System.out.println("Creating table: " + qry);
    Statement stmt = theConnection.createStatement();
    stmt.executeUpdate(qry);
  }

  protected SequenceNumbers readSequenceNumbers() {
    try {
      ResultSet rs = getSequenceNumbers.executeQuery();
      try {
        if (rs.next()) {
          int count = rs.getInt(1);
          if (count == 0) return null;
          int first = rs.getInt(2);
          int last = rs.getInt(3);
          SequenceNumbers result = new SequenceNumbers(first, last);
          return result;
        } else {
          return null;
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

  private void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
    try {
      putSequenceNumbers.setInt(1, sequenceNumbers.first);
      putSequenceNumbers.setInt(2, sequenceNumbers.current);
      putSequenceNumbers.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  protected void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
    try {
      cleanDeltas.setInt(1, cleanupNumbers.first);
      cleanDeltas.setInt(2, cleanupNumbers.current);
      cleanDeltas.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  private void writeDelta(int seqno, InputStream is, int length) {
    try {
      storeDelta.setInt(1, seqno);
      storeDelta.setString(2, "t");
      storeDelta.setBinaryStream(3, is, length);
      storeDelta.executeUpdate();
    }
    catch (SQLException e) {
      fatalException(e);
    }
  }

  private class MyOutputStream
    extends ByteArrayOutputStream
  {
    private int deltaNumber;
    public MyOutputStream(final int deltaNumber) {
      super(8192);
      this.deltaNumber = deltaNumber;
    }
    
    public void close() throws IOException {
      InputStream is = new InputStream() {
        int n = 0;
        public int read() {
          if (n >= count) return -1;
          return buf[n++];
        }
        public int read(byte[] rbuf) {
          return read(rbuf, 0, rbuf.length);
        }
        public int read(byte[] rbuf, int offset, int len) {
          len= Math.min(len, count - n);
          if (len == 0) return -1;
          System.arraycopy(buf, n, rbuf, offset, len);
          n += len;
          return len;
        }
      };
      writeDelta(deltaNumber, is, count);
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

  protected ObjectOutputStream openObjectOutputStream(final int deltaNumber)
    throws IOException
  {
    getDatabaseConnection(this);
    return new ObjectOutputStream(new MyOutputStream(deltaNumber));
  }

  protected void closeObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput)
  {
    try {
      writeSequenceNumbers(retainNumbers);
      currentOutput.close();
    }
    catch (IOException e) {
      fatalException(e);
    }
  }

  protected void abortObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput)
  {
    // Nothing to do since we haven't written to the db yet
    // We just abandon the streams.
  }

  protected ObjectInputStream openObjectInputStream(int deltaNumber)
    throws IOException
  {
    try {
      getDelta.setInt(1, deltaNumber);
      final ResultSet rs = getDelta.executeQuery();
      try {
        if (rs.next()) {
          InputStream theStream = new ByteArrayInputStream(rs.getBytes(1));
          return new ObjectInputStream(theStream);
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

  protected void closeObjectInputStream(int deltaNumber,
                                        ObjectInputStream currentInput)
  {
    try {
      currentInput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected PrintWriter getHistoryWriter(int deltaNumber, String prefix)
    throws IOException
  {
    String seq = "" + (100000+deltaNumber);
    File historyFile = new File(persistenceDirectory, prefix + seq.substring(1));
    return new PrintWriter(new FileWriter(historyFile));
  }

  protected void deleteOldPersistence() {
    try {
      Statement stmt = theConnection.createStatement();
      stmt.executeUpdate("delete from " + deltaTable);
    }
    catch (SQLException se) {
      fatalException(se);
    }
  }

  private void fatalException(Exception e) {
    System.err.println("Fatal database persistence exception: " + e);
    e.printStackTrace();
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
      /** @deprecated in the original **/
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
      /** @deprecated in the original **/
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
    }
  }
}
