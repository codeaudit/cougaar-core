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

/*
 * Originally from delta/fgi package mil.darpa.log.alpine.delta.plugin;
 * Copyright 1997 BBN Systems and Technologies, A Division of BBN Corporation
 * 10 Moulton Street, Cambridge, MA 02138 (617) 873-3000
 */

package org.cougaar.util;

import java.sql.*;
import java.util.*;

/**
 * A database connection manager that creates pools of db connections that can
 * be reused to improve performance.
 * 
 * System properties:
 * org.cougaar.util.DBConnectionPool.maxConnections=10  number of simulataneous 
 * connections allowed per pool.
 * org.cougaar.util.DBConnectionPool.timeoutCheckInterval=5000 milliseconds between
 * checks to see if any old connections should be collected.
 * org.cougaar.util.DBConnectionPool.timeout=10000 milliseconds that a connection
 * must be idle in order to be collected by the reaper.
 * org.cougaar.util.DBConnectionPool.verbosity=0 should verbose debugging
 * messages be turned on? 1=progress, 2=warnings, 3=loud progress
 **/
public class DBConnectionPool {

  /**
   * A hash table relating the database URL and user to a connection
   * pool.
   */
  private static HashMap dbConnectionPools = new HashMap();

  /**
   * The separator inserted between the database URL and user to form
   * the key to the dbConnectionPools hash table.
   */
  private static final String SEP = "#";

  /** How often to run the timeout out checker, in milliseconds. */
  private static long TIMEOUT_CHECK_INTERVAL = 5*1000L;

  /**
   * How long to keep old connections before closing and releasing
   * them.
   *
   * The default value may be controlled with the
   * System Property org.cougaar.util.DBConnectionPool.maxConnections
   */
  private static long TIMEOUT = 10*1000L;

  /**
   * The number of cursors created after which we always release the
   * connection. This tries to avoid an accumulation of never released
   * cursors as the pooled connection is re-used.  Default is 5.
   */
  private static int MAX_CONNECTIONS = 5;

  /** 
   * Is debugging enabled?  Defaults to 0 (quiet).  1=progress, 2=warnings, 3=loudprogress
   **/
  private static int VERBOSITY = 0;

  static {
    String prefix = "org.cougaar.util.DBConnectionPool.";

    TIMEOUT_CHECK_INTERVAL = (Long.valueOf(System.getProperty(prefix+"timeoutCheckInterval", 
                                                              String.valueOf(TIMEOUT_CHECK_INTERVAL)))).longValue();
    TIMEOUT = (Long.valueOf(System.getProperty(prefix+"timeout", 
                                               String.valueOf(TIMEOUT)))).longValue();
    MAX_CONNECTIONS = (Integer.valueOf(System.getProperty(prefix+"maxConnections", 
                                                          String.valueOf(MAX_CONNECTIONS)))).intValue();

    VERBOSITY = (Integer.valueOf(System.getProperty(prefix+"verbosity", 
                                                    String.valueOf(VERBOSITY)))).intValue();
  }

  /**
   * Record the key for this pool for debugging purposes.
   */
  private String key;

  /**
   * Construct a new pool. Record the key for debugging.
   */
  private DBConnectionPool(String key, int max_connections) {
    this.key = key;
    this.maxConnections = (max_connections>0?max_connections:MAX_CONNECTIONS);
  }

  /**
   * Construct a new pool. Record the key for debugging.
   */
  private DBConnectionPool(String key) {
    this.key = key;
    this.maxConnections = MAX_CONNECTIONS;
  }

  int entryCounter = 0;

  /** how many clients are waiting for a connection in this pool? **/
  int waitingCounter = 0;

  /**
   * Inner class to record individual connections
   */
  class DBConnectionPoolEntry {
    /**
     * Construct an entry for a given connection that is not in use.
     */
    int entryNumber = ++entryCounter;
    DBConnectionPoolEntry(Connection aConnection) {
      theConnection = aConnection;
    }

    /**
     * Return the pool that this entry is in.
     */
    DBConnectionPool getDBConnectionPool() {
      return DBConnectionPool.this;
    }

    /**
     * Create a PoolConnection to return to the user.
     */
    Connection getPoolConnection() {
      return new PoolConnection(theConnection);
    }

    /**
     *  This assumes that the entry will no longer be used.  There is no mechanism
     *  to reopen theConnection.
     */
    private void destroy() {
      try {
        theConnection.close();
      } catch (SQLException sqle) {
        sqle.printStackTrace();
      }
    }
    
    /**
     * The connection of this entry.
     */
    Connection theConnection;

    /**
     * Indicates if this entry is in use.
     */
    boolean inUse = false;

    /**
     * Records when this connection was last used.
     */
    long lastUsed = System.currentTimeMillis();

    /**
     * This is a wrapper for a Connection object that delegates most
     * functions to the wrapped object, but interposes some processing
     * of its own to keep track of operations that have been done to
     * the connection. This record permits the connection to be
     * logically closed, but to remain actually open. In particular,
     * Statement objects created for the connection can be closed if
     * the program has not already done so. In this way, the
     * connection starts out in a clean state when reused.
     */
    class PoolConnection implements Connection {
      Connection c;
      boolean closed = false;
      ArrayList statements = new ArrayList();
      PoolConnection(Connection realConnection) {
	c = realConnection;
      }
      private void destroyPool() {
	DBConnectionPoolEntry entry = DBConnectionPoolEntry.this;
        // If this Connection is in the pool, then destroy the pool,
        // otherwise leave it alone.
        if (entry.getDBConnectionPool().containsConnection(entry))
          entry.getDBConnectionPool().destroyPool();
      }
      private void closeStatement(PoolStatement statement) throws SQLException {
	synchronized (statements) {
	  statement.theStatement.close();
	  statements.remove(statement);
	}
      }
      public Statement createStatement() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	Statement statement = null;
        int rc = 0;             // retry count
        while (true) {
          try {
            statement = new PoolStatement(c.createStatement());
            statements.add(statement);
            return statement;
          } catch (SQLException sqle) {
            if (isRetryable(sqle) && rc < maxRetries) {
              rc++;
              try {
                Thread.sleep(retryTimeout);
              } catch (InterruptedException ie) {}
            } else {
              destroyPool();
              throw sqle;
            }
          }
        }
      }
      public Statement createStatement(int a, int b) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	Statement statement = null;

        int rc = 0;             // retry count
        while (true) {
          try {
            statement = new PoolStatement(c.createStatement(a, b));
            statements.add(statement);
            return statement;
          } catch (SQLException sqle) {
            if (isRetryable(sqle) && rc < maxRetries) {
              rc++;
              try {
                Thread.sleep(retryTimeout);
              } catch (InterruptedException ie) {}
            } else {
              destroyPool();
              throw sqle;
            }
          }
        }
      }
      public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	PreparedStatement statement = null;
        try {
          statement = (PreparedStatement)new PoolPreparedStatement(c.prepareStatement(sql));
	  statements.add(statement);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
	return statement;
      }
      public PreparedStatement prepareStatement(String sql, int a, int b) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	PreparedStatement statement = null;
        try {
          statement = (PreparedStatement)new PoolPreparedStatement(c.prepareStatement(sql, a, b));
	  statements.add(statement);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
	return statement;
      }
      public CallableStatement prepareCall(String sql) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	CallableStatement statement = null;
        try {
          statement = new PoolCallableStatement(c.prepareCall(sql));
	  statements.add(statement);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
	return statement;
      }
      public CallableStatement prepareCall(String sql, int a, int b) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
	CallableStatement statement = null;
        try {
          statement = new PoolCallableStatement(c.prepareCall(sql, a, b));
	  statements.add(statement);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
	return statement;
      }
      public String nativeSQL(String sql) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        String str = null;
        try {
	  str = c.nativeSQL(sql);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return str;
      }
      public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.setAutoCommit(autoCommit);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public boolean getAutoCommit() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        boolean b;
        try {
	  b = c.getAutoCommit();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return b;
      }
      public void commit() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.commit();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public void rollback() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.rollback();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public void finalize() {
	if (closed) return;	// Connection already closed (normal case)
	try {			// Connection was never closed (abandoned)
	  close();		// Simply close it now (it will get reused)
	}
	catch (SQLException e ) {
	  e.printStackTrace();	// Ignore exceptions
	}
      }
      public void close() throws SQLException {
	if (closed) return;
        closed = true;
        try {
          c.commit();
	  synchronized (statements) {
	    while (statements.size() > 0) {
	      PoolStatement statement = (PoolStatement) statements.get(0);
	      closeStatement(statement);
	    }
	  }
        } catch (SQLException sqle) {
          // Since the entry that contains this Connection still exists, we
          // should explicitly kill this entry after the pool is des
          destroyPool();
          DBConnectionPoolEntry entry = DBConnectionPoolEntry.this;
          entry.destroy();
          throw sqle;
        }
        // If the pool contains the entry, return it, otherwise, the pool has
        // been destroyed so destroy the entry
        DBConnectionPoolEntry entry = DBConnectionPoolEntry.this;
        if (entry.getDBConnectionPool().containsConnection(entry))
          entry.getDBConnectionPool().release(entry);
        else {
          entry.destroy();
        }
      }
      public boolean isClosed() throws SQLException {
	return closed;
      }
      public DatabaseMetaData getMetaData() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        DatabaseMetaData data = null;
        try {
          data = c.getMetaData();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return data;        
      }
      public void setReadOnly(boolean readOnly) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.setReadOnly(readOnly);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public boolean isReadOnly() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        boolean b;
        try {
          b = c.isReadOnly();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return b;
      }
      public void setCatalog(String catalog) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.setCatalog(catalog);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public String getCatalog() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        String str = null;
        try {
	  str = getCatalog();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return str;
      }
      public void setTransactionIsolation(int level) throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.setTransactionIsolation(level);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public int getTransactionIsolation() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        int i;
        try {
	  i = c.getTransactionIsolation();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return i;
      }
      public SQLWarning getWarnings() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        SQLWarning warn = null;
        try {
	  warn = c.getWarnings();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return warn;
      }
      public void clearWarnings() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        try {
	  c.clearWarnings();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      public java.util.Map getTypeMap() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
        java.util.Map map = null;
        try {
	  map = c.getTypeMap();
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
        return map;
      }
      public void setTypeMap(java.util.Map map) throws SQLException {
	if (closed) throw new SQLException("Connection is closed");
        try {
	  c.setTypeMap(map);
        } catch (SQLException sqle) {
          destroyPool();
          throw sqle;
        }
      }
      /**
       * A wrapper for a Statement object. Most operations are
       * delegated to the wrapped object. The close operation goes
       * through the PoolConnection wrapper to keep track of which
       * statements have been closed and which haven't.
       */
      class PoolStatement implements java.sql.Statement {
	java.sql.Statement theStatement;
	public PoolStatement(java.sql.Statement theStatement) {
	  this.theStatement = theStatement;
	}
	public void addBatch( String sql )  throws java.sql.SQLException {
          try {
            theStatement.addBatch( sql );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void clearBatch()  throws java.sql.SQLException {
          try {
            theStatement.clearBatch();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public int[] executeBatch()  throws java.sql.SQLException {
          int[] i;
          try {
            i = theStatement.executeBatch();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public Connection getConnection()  throws java.sql.SQLException {
          Connection conn = null;
          try {
            conn = theStatement.getConnection();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return conn;
	}
	public int getFetchDirection()  throws java.sql.SQLException {
          int i;
          try {
            i = theStatement.getFetchDirection();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public int getFetchSize()  throws java.sql.SQLException {
          int i;
          try {
            i = theStatement.getFetchSize();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public int getResultSetConcurrency()  throws java.sql.SQLException {
          int i;
          try {
            i = theStatement.getResultSetConcurrency();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public int getResultSetType()  throws java.sql.SQLException {
          int i;
          try {
            i = theStatement.getResultSetType();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void setFetchDirection( int direction )  throws java.sql.SQLException {
          try {
            theStatement.setFetchDirection( direction );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setFetchSize( int rows )  throws java.sql.SQLException  {
          try {
            theStatement.setFetchSize( rows );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public java.sql.ResultSet executeQuery(java.lang.String arg0) throws java.sql.SQLException {
          java.sql.ResultSet rs = null;
          try {
	    rs = theStatement.executeQuery(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return rs;
	}
	public int executeUpdate(java.lang.String arg0) throws java.sql.SQLException {
          int i;
          try {
	    i = theStatement.executeUpdate(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void close() throws java.sql.SQLException {
          try {
	    closeStatement(this);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public int getMaxFieldSize() throws java.sql.SQLException {
          int i;
          try {
	    i = theStatement.getMaxFieldSize();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void setMaxFieldSize(int arg0) throws java.sql.SQLException {
          try {
	    theStatement.setMaxFieldSize(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public int getMaxRows() throws java.sql.SQLException {
          int i;
          try {
	    i = theStatement.getMaxRows();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void setMaxRows(int arg0) throws java.sql.SQLException {
          try {
	    theStatement.setMaxRows(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setEscapeProcessing(boolean arg0) throws java.sql.SQLException {
          try {
	    theStatement.setEscapeProcessing(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public int getQueryTimeout() throws java.sql.SQLException {
          int i;
          try {
	    i = theStatement.getQueryTimeout();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void setQueryTimeout(int arg0) throws java.sql.SQLException {
          try {
	    theStatement.setQueryTimeout(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void cancel() throws java.sql.SQLException {
          try {
	    theStatement.cancel();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
          java.sql.SQLWarning warn = null;
          try {
	    warn = theStatement.getWarnings();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return warn;
	}
	public void clearWarnings() throws java.sql.SQLException {
          try {
	    theStatement.clearWarnings();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setCursorName(java.lang.String arg0) throws java.sql.SQLException {
          try {
	    theStatement.setCursorName(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public boolean execute(java.lang.String arg0) throws java.sql.SQLException {
          boolean b;
          try {
	    b = theStatement.execute(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public java.sql.ResultSet getResultSet() throws java.sql.SQLException {
          java.sql.ResultSet rs = null;
          try {
	    rs = theStatement.getResultSet();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return rs;
	}
	public int getUpdateCount() throws java.sql.SQLException {
          int i;
          try {
	    i = theStatement.getUpdateCount();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public boolean getMoreResults() throws java.sql.SQLException {
          boolean b;
          try {
	    b = theStatement.getMoreResults();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
      }
      /**
       * A wrapper for a PreparedStatement object. All operations are
       * delegated to the wrapped object. The close operation in the
       * base class goes through the PoolConnection wrapper to keep
       * track of which statements have been closed and which haven't.
       */
      class PoolPreparedStatement extends PoolStatement implements java.sql.PreparedStatement {
	private java.sql.PreparedStatement thePreparedStatement;
	public PoolPreparedStatement(java.sql.PreparedStatement thePreparedStatement) {
	  super(thePreparedStatement);
	  this.thePreparedStatement = thePreparedStatement;
	}
	public void addBatch() throws java.sql.SQLException {
          try {
            thePreparedStatement.addBatch();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public ResultSetMetaData getMetaData() throws java.sql.SQLException {
          ResultSetMetaData data = null;
          try {
            data = thePreparedStatement.getMetaData();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return data;
	}
	public void setArray( int i, Array x ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setArray( i, x );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setBlob( int i, Blob x ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setBlob( i, x );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setCharacterStream( int paramIndex, java.io.Reader reader, int length ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setCharacterStream( paramIndex, reader, length );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setClob( int i, Clob x ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setClob( i,x );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setRef( int i, Ref x )  throws java.sql.SQLException {
          try {
            thePreparedStatement.setRef( i, x );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setDate( int i, java.sql.Date myDate, Calendar cal ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setDate( i, myDate, cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setTime( int paramIndex, Time x, Calendar cal ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setTime( paramIndex, x, cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setTimestamp( int paramIndex, java.sql.Timestamp x, Calendar cal ) throws java.sql.SQLException {
          try {
            thePreparedStatement.setTimestamp( paramIndex, x ,cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public java.sql.ResultSet executeQuery() throws java.sql.SQLException {
          java.sql.ResultSet rs = null;
          try {
            rs = thePreparedStatement.executeQuery();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return rs;
	}
	public int executeUpdate() throws java.sql.SQLException {
          int i;
          try {
	    i = thePreparedStatement.executeUpdate();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public void setNull(int arg0, int arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setNull(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setNull(int arg0, int arg1, String typeName ) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setNull(arg0, arg1, typeName);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setBoolean(int arg0, boolean arg1) throws java.sql.SQLException {
          try {
            thePreparedStatement.setBoolean(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setByte(int arg0, byte arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setByte(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setShort(int arg0, short arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setShort(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setInt(int arg0, int arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setInt(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setLong(int arg0, long arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setLong(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setFloat(int arg0, float arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setFloat(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setDouble(int arg0, double arg1) throws java.sql.SQLException {
          try {
 	    thePreparedStatement.setDouble(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setBigDecimal(int arg0, java.math.BigDecimal arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setBigDecimal(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setString(int arg0, java.lang.String arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setString(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setBytes(int arg0, byte[] arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setBytes(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setDate(int arg0, java.sql.Date arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setDate(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setTime(int arg0, java.sql.Time arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setTime(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setTimestamp(int arg0, java.sql.Timestamp arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setTimestamp(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setAsciiStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setAsciiStream(arg0, arg1, arg2);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
        /** @deprecated **/
	public void setUnicodeStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
          throw new java.sql.SQLException("Method not supported");
          //	  thePreparedStatement.setUnicodeStream(arg0, arg1, arg2);
	}
	public void setBinaryStream(int arg0, java.io.InputStream arg1, int arg2) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setBinaryStream(arg0, arg1, arg2);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void clearParameters() throws java.sql.SQLException {
          try {
	    thePreparedStatement.clearParameters();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setObject(int arg0, java.lang.Object arg1, int arg2, int arg3) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setObject(arg0, arg1, arg2, arg3);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setObject(int arg0, java.lang.Object arg1, int arg2) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setObject(arg0, arg1, arg2);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void setObject(int arg0, java.lang.Object arg1) throws java.sql.SQLException {
          try {
	    thePreparedStatement.setObject(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public boolean execute() throws java.sql.SQLException {
          boolean b;
          try {
	    b = thePreparedStatement.execute();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
      }
      /**
       * A wrapper for a CallableStatement object. All operations are
       * delegated to the wrapped object. The close operation in the
       * base class goes through the PoolConnection wrapper to keep
       * track of which statements have been closed and which haven't.
       */
      class PoolCallableStatement extends PoolPreparedStatement implements java.sql.CallableStatement {
	private java.sql.CallableStatement theCallableStatement;
	public PoolCallableStatement(java.sql.CallableStatement theCallableStatement) {
	  super(theCallableStatement);
	  this.theCallableStatement = theCallableStatement;
	}
	public Array getArray( int i )  throws java.sql.SQLException {
          Array a = null;
          try {
            a = theCallableStatement.getArray( i );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return a;
	}
        /** @deprecated **/
	public java.math.BigDecimal getBigDecimal(int paramIndex)  throws java.sql.SQLException {
          java.math.BigDecimal bd;
          try {
            bd = theCallableStatement.getBigDecimal( paramIndex );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return bd;
	}
	public Blob getBlob( int i )  throws java.sql.SQLException {
          Blob b = null;
          try {
            b = theCallableStatement.getBlob( i );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public Clob getClob( int i )  throws java.sql.SQLException {
          Clob c = null;
          try {
            c = theCallableStatement.getClob( i );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return c;
	}
	public java.sql.Date getDate( int paramIndex, Calendar cal )  throws java.sql.SQLException {
          java.sql.Date d = null;
          try {
            d = theCallableStatement.getDate( paramIndex, cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return d;
	}
	public Object getObject( int i, Map map )  throws java.sql.SQLException {
          Object o = null;
          try {
            o = theCallableStatement.getObject ( i, map );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return o;
	}
	public Ref getRef ( int i )  throws java.sql.SQLException {
          Ref r = null;
          try {
            r = theCallableStatement.getRef( i );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return r;
	}
	public Time getTime ( int paramIndex, Calendar cal )  throws java.sql.SQLException {
          Time t = null;
          try {
            t = theCallableStatement.getTime ( paramIndex, cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return t;
	}
	public Timestamp getTimestamp( int paramIndex, Calendar cal )  throws java.sql.SQLException {
          Timestamp ts = null;
          try {
            ts = theCallableStatement.getTimestamp( paramIndex, cal );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return ts;
	}
	public void registerOutParameter( int paramIndex, int sqlType, String typeName )  throws java.sql.SQLException {
          try {
            theCallableStatement.registerOutParameter( paramIndex, sqlType, typeName );
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void registerOutParameter(int arg0, int arg1) throws java.sql.SQLException {
          try {
	    theCallableStatement.registerOutParameter(arg0, arg1);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public void registerOutParameter(int arg0, int arg1, int arg2) throws java.sql.SQLException {
          try {
	    theCallableStatement.registerOutParameter(arg0, arg1, arg2);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
	}
	public boolean wasNull() throws java.sql.SQLException {
          boolean b;
          try {
	    b = theCallableStatement.wasNull();
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public java.lang.String getString(int arg0) throws java.sql.SQLException {
          java.lang.String str = null;
          try {
	    str = theCallableStatement.getString(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return str;
	}
	public boolean getBoolean(int arg0) throws java.sql.SQLException {
          boolean b;
          try {
	    b = theCallableStatement.getBoolean(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public byte getByte(int arg0) throws java.sql.SQLException {
          byte b;
          try {
	    b = theCallableStatement.getByte(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public short getShort(int arg0) throws java.sql.SQLException {
          short s;
          try {
	    s = theCallableStatement.getShort(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return s;
	}
	public int getInt(int arg0) throws java.sql.SQLException {
          int i;
          try {
	    i = theCallableStatement.getInt(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return i;
	}
	public long getLong(int arg0) throws java.sql.SQLException {
          long l;
          try {
	    l = theCallableStatement.getLong(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return l;
	}
	public float getFloat(int arg0) throws java.sql.SQLException {
          float f;
          try {
	    f = theCallableStatement.getFloat(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return f;
	}
	public double getDouble(int arg0) throws java.sql.SQLException {
          double d;
          try {
	    d = theCallableStatement.getDouble(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return d;
	}
	public java.math.BigDecimal getBigDecimal(int arg0, int arg1) throws java.sql.SQLException {
          throw new java.sql.SQLException("Method not supported");
          //	  return theCallableStatement.getBigDecimal(arg0, arg1);
	}
	public byte[] getBytes(int arg0) throws java.sql.SQLException {
          byte[] b;
          try {
	    b = theCallableStatement.getBytes(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return b;
	}
	public java.sql.Date getDate(int arg0) throws java.sql.SQLException {
          java.sql.Date date = null;
          try {
	    date = theCallableStatement.getDate(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return date;
	}
	public java.sql.Time getTime(int arg0) throws java.sql.SQLException {
          java.sql.Time time = null;
          try {
	    time = theCallableStatement.getTime(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return time;
	}
	public java.sql.Timestamp getTimestamp(int arg0) throws java.sql.SQLException {
          java.sql.Timestamp ts = null;
          try {
	    ts = theCallableStatement.getTimestamp(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return ts;
	}
	public java.lang.Object getObject(int arg0) throws java.sql.SQLException {
          java.lang.Object o = null;
          try {
	    o = theCallableStatement.getObject(arg0);
          } catch (SQLException sqle) {
            PoolConnection.this.destroyPool();
            throw sqle;
          }
          return o;
	}
      }
    }
  }

  /**
   * One of the three main functions of this class. This is intended
   * as a drop-in replacement for DriverManager.getConnection(). The
   * connection return may have been previously used.
   */
  public static Connection getConnection(String dbURL,
					 Properties props)
    throws SQLException {
    return getConnection(dbURL, (String) props.get("user"), (String) props.get("password"));
  }

  /**
   * One of the three main functions of this class. This is intended
   * as a drop-in replacement for DriverManager.getConnection(). The
   * connection return may have been previously used.
   */
  public static Connection getConnection(String dbURL,
					 String user,
					 String passwd)
    throws SQLException {
    String key = dbURL + SEP + user;
    DBConnectionPool pool;

    synchronized (dbConnectionPools) {
      pool = (DBConnectionPool) dbConnectionPools.get(key);
      if (pool == null) {
	pool = new DBConnectionPool(key);
	dbConnectionPools.put(key, pool);
      }
    }

    return pool.findConnection(dbURL, user, passwd);
  }

  private static Thread timer = new Thread() {
      public void run() {
        try {
          while (true) {		// Never ceases
            try {
              Thread.sleep(TIMEOUT_CHECK_INTERVAL);
            } 
            catch (InterruptedException e) { } // Pro forma catch
            DBConnectionPool[] pools;
            synchronized (dbConnectionPools) {
              int l = dbConnectionPools.size();
              if (l != 0) {
                pools = new DBConnectionPool[l];
                {
                  int i = 0;
                  for (Iterator e = dbConnectionPools.values().iterator(); e.hasNext(); ) {
                    pools[i++] = (DBConnectionPool) e.next();
                  }
                }
                long now = System.currentTimeMillis();
                for (int i = 0; i < pools.length; i++) {
                  try {
                    pools[i].checkTimeout(now);
                  }
                  catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }
        } 
        catch (Throwable t) {
          // Emergency measures to keep thread from dying.
          System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\nUncaught Exception or Error in DBConnectionPool:");
          t.printStackTrace();
          System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
      }
    };

  static {
    timer.setName("DBConnectionPool Timer");
    timer.setDaemon(true);
    timer.start();
  }

  /**
   * A List of entries for connections we have opened.
   */
  private ArrayList entries = new ArrayList();

  /**
   * The maximum number of connections we permit in this
   * pool. Obtained from the first connection opened in this pool.
   */
  private int maxConnections = -1;

  /** How many times to retry a getConnection when failed due to a recoverable exception **/
  private int maxRetries = 30;  // retry for a whole minute.
  
  /** How frequently should recoverable getConnections be retried? **/
  private long retryTimeout = 5*1000L; // 5 seconds

  /**
   *
   */
  private synchronized boolean containsConnection(DBConnectionPoolEntry entry) {
    return entries.contains(entry);
  }

  /**
   * Closes all of the currently unused Connections in this pool.  Connections
   * that are currently open must destroy themselves when they become unused.
   */
  private synchronized void destroyPool() {
    while (!entries.isEmpty()) {
      DBConnectionPoolEntry entry = (DBConnectionPoolEntry) entries.get(0);
      entries.remove(entry);
      if (!entry.inUse)
        entry.destroy();
    }
  }

  /** Encapsulate logic to decide if a given exception is likely to be 
   * transitory and so the connection attempt worthwhile retrying.
   **/
  private static boolean isRetryable(SQLException e) {
    while (e != null) {
      String m = e.getMessage();
      if (m.startsWith("ORA-00604")) { // recursive error
        e = e.getNextException();
      } else if (m.startsWith("ORA-00020") || // maximum number of processes (100) exceeded  [too many connections]
                 m.startsWith("ORA-00018") || // maximum number of sessions exceeded [?]
                 m.startsWith("ORA-01000")    // maximum open cursors exceeded  [too many open statements]
                 ) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  private synchronized Connection findConnection(String dbURL, String user, String passwd)
    throws SQLException 
  {
    int retries = 0;            // how many retries have we done?
    boolean waitingP = false;
    while (true) {
      for (Iterator e = entries.iterator(); e.hasNext(); ) {
        DBConnectionPoolEntry entry = (DBConnectionPoolEntry) e.next();
        if (!entry.inUse) {
          entry.inUse = true;
          return entry.getPoolConnection();
        }
      }
    
      if (maxConnections < 0 || entries.size() < maxConnections) {
        try {
          Connection conn = DriverManager.getConnection(dbURL, user, passwd);
          if (maxConnections < 0) {
            maxConnections = conn.getMetaData().getMaxConnections();
            if (maxConnections < 1 || maxConnections > MAX_CONNECTIONS) {
              maxConnections = MAX_CONNECTIONS;
            }
          }
          DBConnectionPoolEntry entry = new DBConnectionPoolEntry(conn);
          entries.add(entry);
          if (waitingP) {
            waitingCounter--;
            if (VERBOSITY>=1) {
              if (VERBOSITY>=3) {
                System.err.println("DBConnectionPool finished waiting for "+key+" ("+waitingCounter+")");
              } else {
                System.err.print("f");
              }
            }
          }
        } catch (SQLException sqle) {
          if (VERBOSITY>=2) System.err.println("DBConnectionPool "+key+" saw: "+sqle);
          if (isRetryable(sqle) && retries<maxRetries) {
            retries++;
            waitingP = true;
            waitingCounter++;
            if (VERBOSITY>=1) {
              if (VERBOSITY>=3) {
                System.err.println("DBConnectionPool waiting to retry for "+key+" ("+waitingCounter+")");
              } else {
                System.err.print("w");
              }
            }
            try {
              wait(retryTimeout);
            } catch (InterruptedException e) {}
          } else {
            throw sqle;
          }
        }
      } else {
        try {
          if (!waitingP) {
            waitingP = true;
            waitingCounter++;
            if (VERBOSITY>=1) {
              if (VERBOSITY>=3) {
                System.err.println("DBConnectionPool waiting for "+key+
                                   " ("+waitingCounter+" of "+entries.size()+"/"+maxConnections+")");
              } else {
                System.err.print("w");
              }
            }
          }
          wait();
        } catch (InterruptedException e) { }
      }
    }
  }

  private synchronized void delete(DBConnectionPoolEntry entry) {
    entries.remove(entry);
    entry.destroy();
  }

  private synchronized void release(DBConnectionPoolEntry entry) {
    entry.inUse = false;
    entry.lastUsed = System.currentTimeMillis();
    notifyAll();
  }

  private synchronized void checkTimeout(long now) {
    if (entries.size() > 0) {
      ArrayList entriesToDelete = new ArrayList();
      for (Iterator e = entries.iterator(); e.hasNext(); ) {
        DBConnectionPoolEntry entry = (DBConnectionPoolEntry) e.next();
        if (!entry.inUse) {
          if (entry.lastUsed < (now - TIMEOUT)) {
            entriesToDelete.add(entry);
          }
        }
      }
      int ndrops = entriesToDelete.size();
      if (ndrops > 0) {
        if (VERBOSITY>=3)
          System.err.println("DBConnectionPool "+key+" dropping "+entriesToDelete.size()+" entries");
        for (Iterator e = entriesToDelete.iterator(); e.hasNext(); ) {
          DBConnectionPoolEntry entry = (DBConnectionPoolEntry) e.next();
          delete(entry);
        }
      }
    }
  }

  //
  // driver registration
  //

  /** A Map of drivernames to Driver classes **/
  private static Map drivers = new HashMap(); 

  /** Register a driver carefully **/
  public static void registerDriver(String driverName)
    throws Exception
  {
    synchronized (drivers) {
      if (drivers.get(driverName) != null)
        return;
      
      Driver driver = (Driver)(Class.forName(driverName).newInstance());
      DriverManager.registerDriver(driver);
      drivers.put(driverName, driver);
    }
  }


  /*
  // hack test routine - take three args "host:port:SID" "user" "password" 
  public static void main(String arg[]) {
    try {
      DBConnectionPool.registerDriver("oracle.jdbc.driver.OracleDriver");

      final String url = "jdbc:oracle:thin:@"+arg[0];
      final String user = arg[1];
      final String password = arg[2];
      
      int nthreads = 100;

      Thread threads[] = new Thread[nthreads];

      // each cycle hits the database with lots at the same time
      for (int cycle = 0; cycle<10; cycle++) {
        System.out.print("Cycle "+cycle+":");
        
        class Acc {
          long x;
        }
        final Acc waitA = new Acc();
        final Acc queryA = new Acc();

        for (int i = 0; i< nthreads; i++) {
          threads[i] = new Thread(new Runnable() {
              public void run() {
                try {
                long t0 = System.currentTimeMillis();
                Connection c = DBConnectionPool.getConnection(url,user,password);
                long t1 = System.currentTimeMillis();
                waitA.x += (t1-t0);
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select container_20_ft_qty from ue_summary_mtmc");
                int n =0;
                while(rs.next()) { n++; }
                // 44 rows
                if (n != 44) throw new RuntimeException("Oops! Got the wrong count.");
                s.close();
                c.close();
                long t2 = System.currentTimeMillis();
                queryA.x += (t2-t1);
                } catch (SQLException e) {
                  e.printStackTrace();
                }
              }
            });
        }
        System.out.print(" .");
        for (int i = 0; i< nthreads; i++) {
          threads[i].start();
        }
        System.out.print(" .");
        for (int i = 0; i< nthreads; i++) {
          threads[i].join();
        }
        
        double avwait = waitA.x/((double)nthreads);
        double avquery = queryA.x/((double)nthreads);
        System.out.println();
        System.out.println("\tAverage Connection wait = "+avwait);
        System.out.println("\tAverage Query wait  = "+avquery);

      // wait for a bit to let the reaper run
      Thread.sleep(2*1000);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  */

}
