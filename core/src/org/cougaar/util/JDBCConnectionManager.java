/*
 * <copyright>
 *  Copyright 2001 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.io.*;
import java.sql.*;
import java.util.*;

/** A manager of JDBC operation.
 * Maintains a heterogeneous pool of open Connections.  All connections are 
 * kept in the same pool and so are bound by the same upper limit, as defined
 * by the parameters.  New connections are allocated to pool on demand (subject 
 * to the maxConnections limit) and kept idle until collected by a reaper 
 * process which runs periodically.
 * 
 * controlling parameters:
 *
 **/
public class JDBCConnectionManager {

  //
  //  per-VM parameters
  //

  public static final String MAX_CONNECTIONS = "org.cougaar.util.JDBCConnectionManager.maxConnections";
  public static final String REAP_TIME = "org.cougaar.util.JDBCConnectionManager.reapTime";
  public static final String REAP_IDLE = "org.cougaar.util.JDBCConnectionManager.reapIdle";

  /** Maximum simultaneous open connections for the VM.  The default is 10. **/
  private static final int DEFAULT_MAX_CONNECTIONS = 10;
  /** Time in milliseconds between idle connection reaps.  The default is 60 seconds. **/
  private static final long DEFAULT_REAP_TIME = 60*1000;
  /** Time in milleseconds that a connection must have been idle before it is 
   * considered recoverable by the reaper. The default is 60 seconds. 
   **/
  private static final long DEFAULT_REAP_IDLE = 60*1000;

  //
  // singleton pattern
  //

  /** hold the singleton instance **/
  private static JDBCConnectionManager instance = null;

  /** Singleton pattern access to the instance **/
  public static synchronized JDBCConnectionManager getInstance() {
    if (instance == null) 
      instance = new JDBCConnectionManager();
    return instance;
  }

  //
  // instance behavior
  //

  /** Maximum simultaneous open connections for the VM.  The default is 20. **/
  private int maxConnections = DEFAULT_MAX_CONNECTIONS;
  /** Time in milliseconds between idle connection reaps.  The default is 60 seconds. **/
  private long reapTime = DEFAULT_REAP_TIME;
  /** Time in milleseconds that a connection must have been idle before it is 
   * considered recoverable by the reaper. The default is 60 seconds. **/
  private long reapIdle = DEFAULT_REAP_IDLE;
 
  private static final int getInt(String prop, int def) {
    return (Integer.valueOf(System.getProperty(prop, String.valueOf(def)))).intValue();
  }


  /** all the active connections.  Elements are of type ManagedConnection. 
   * The idle and active lists are separated to avoid contention when reaping.
   * Any accessors of this list must be synchronized.
   **/
  private List activeConnections;

  /** all the idle connections.  Elements are of type ManagedConnection. 
   * The idle and active lists are separated to avoid contention when reaping.
   * Any accessors of this list must be synchronized.
   **/
  private List idleConnections;

  /** A thread which periodically closes idle connections **/
  private Thread reaper;

  /** total alive connections **/
  private int aliveCount = 0;

  /** private constructor sets up the manager by initializing the connection
   * list and fires up the Reaper.
   **/
  private JDBCConnectionManager() {
    maxConnections = getInt(MAX_CONNECTIONS, maxConnections);
    reapTime = getInt(REAP_TIME, (int)reapTime);
    reapIdle = getInt(REAP_IDLE, (int)reapIdle);

    activeConnections = new ArrayList(maxConnections);
    idleConnections = new ArrayList(maxConnections);

    Runnable runner = new Runnable() {
        public void run() {
          while (true) {
            try {
              Thread.sleep(reapTime);
              reapConnections();    //  call
            } catch (InterruptedException ie) {
              // ignore interruptions.
            } catch (RuntimeException e) {
              e.printStackTrace();
            }
          }
        }};

    reaper = new Thread(runner, "JDBC Connection Reaper");
    reaper.setDaemon(true);
    reaper.setPriority(Thread.MIN_PRIORITY);
    reaper.start();

    //System.err.println("New JDBCConnectionManager: "+maxConnections+" "+reapTime+" "+reapIdle);    
  }

  //
  // standard management policy implementation
  //
  
  /** get a connection to a database from the connection pool 
   * conforming to the specified information.  The method will
   * block until such a connection is available.  Connection errors will
   * be throw back to the caller.
   * @param url JDBC connection URL of the database required.
   * @param user User to use to connect to the database.  Should be null 
   * if the url specifies a database which does not require a user/password.
   * @param password Password to use to connect to the database.  Should be null 
   * if the url specifies a database which does not require a user/password.
   * @param timeout milliseconds to wait for a connection before
   * giving up.  If this happens, null will be returned. 
   **/
  public synchronized Connection getConnection(String url, String user, String password, long millis) 
    throws SQLException
  {
    if (url == null) throw new IllegalArgumentException("URL must be supplied.");
    Connection c = null;
    while (true) {
      // Check idle list for likely match
      if ((c = recycleConnection(url, user, password)) != null)
        return c;
      
      // see if we need to (and can) drop a currently idle connection
      if (aliveCount==maxConnections) {
        dropIdleConnection();
      }

      // Consider starting a new connection
      if ((c = createConnection(url, user, password)) != null)
        return c;
      
      if (millis < 0) {         // we waited and still failed
        return null;
      }

      if (millis == 0) {
        // wait (forever) for a slot
        try {
          wait(millis);
        } catch (InterruptedException ex) {}
      } else {
        // wait (for a while) for a slot
        long startTime = System.currentTimeMillis();
        try {
          wait(millis);
        } catch (InterruptedException ex) {
        }
        long waited = System.currentTimeMillis() - startTime;
        millis = millis-waited;
        if (millis <= 0) millis = -1;
      }
    }
  }

  public Connection getConnection(String url) throws SQLException {
    return getConnection(url, null, null, 0);
  }

  public Connection getConnection(String url, String user, String password) throws SQLException {
    return getConnection(url, user, password, 0);
  }

  public Connection getConnection(String url, int timeout) throws SQLException {
    return getConnection(url, null, null, timeout);
  }

  public synchronized void returnConnection(Connection c) {
    if (!(c instanceof ManagedConnection)) {
      throw new IllegalArgumentException("only managed connections may be released.");
    }
    ManagedConnection mc = (ManagedConnection) c;
    if (!activeConnections.remove(mc)) {
      throw new IllegalArgumentException("May not return non-active Connections.");
    }
    if (! mc.idle()) {
      aliveCount--;             // prevent a Connection leak.
      throw new RuntimeException("ManagedConnection could not be idled: "+mc);
    }
    idleConnections.add(mc);
    notifyAll();
    //System.err.print("I");
  }
      

  /** Utility for dropping an old (idle) connection.  Removes the first,
   * that is, the oldest idle connection.
   * @return the (now dead) reaped connection or null.
   **/
  private synchronized Connection dropIdleConnection() {
    if (! idleConnections.isEmpty()) {
      ManagedConnection c = (ManagedConnection) idleConnections.get(0);
      if (c.reap()) {           // just to be sure...
        idleConnections.remove(0);
        aliveCount--;
        notifyAll();
        //System.err.print("X");
        return c;
      }
    }
    return null;
  }

  /** utility for recycling an old (idle) connection **/
  private synchronized Connection recycleConnection(String url, String user, String password) {
    if (! idleConnections.isEmpty()) {
      for (Iterator i = idleConnections.iterator(); i.hasNext(); ) {
        ManagedConnection c = (ManagedConnection) i.next();
        if (c.tryActivate(url,user,password)) {
          // found a match!
          i.remove();         // remove it from the idle list
          activeConnections.add(c); // add it to the active list
          //System.err.print("R");
          return c;           // return it
        }
      }
    }
    return null;
  }

  /** utility for starting a new connection **/
  private synchronized Connection createConnection(String url, String user, String password) 
    throws SQLException
  {
    if (aliveCount<maxConnections) {// room in the pool?
      // fire up a new connection!
      Properties props = new Properties();
      if (user != null) {
        props.put("user", user);
        props.put("password", password);
      }
      Connection c = DriverManager.getConnection(url, props);
      ManagedConnection mc = new ManagedConnection(c, url, user, password);
      mc.activate();
      activeConnections.add(mc);
      aliveCount++;
      //System.err.print("N");
      //notifyAll();  // no need to notify since there cannot be any waiters.
      return mc;
    }
    return null;
  }


  
  //
  // reaper support
  //
  
  private synchronized void reapConnections() {
    for (Iterator i = idleConnections.iterator(); i.hasNext(); ) {
      ManagedConnection c = (ManagedConnection) i.next();
      if (c.tryReap(reapIdle)) {
        i.remove();
        aliveCount--;
        notifyAll();
        //System.err.print("!");
      }
    }
  }
    
  //
  // driver registration
  //

  /** A Map of drivernames to Driver classes **/
  private Map drivers = new HashMap(); 

  /** Register a driver carefully **/
  public void registerDriver(String driverName)
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


  //
  // ManagedConnection class
  //

  /** Managed JDBC Connections keep track of the connection itself and
   * additional information like idle state, etc.
   **/
  public static class ManagedConnection implements Connection {
    /** The associated jdbc connection. **/
    Connection connection;

    String url;
    String user;
    String password;

    ManagedConnection(Connection c, String url, String user, String password) {
      this.connection = c;
      this.url = url;
      this.user = user;
      this.password = password;
      idleP = true;             // idle until activated.
      aliveP = true;
    }

    /** @return true IFF the connection matches the supplied criteria. **/
    public boolean compareWith(String purl, String puser, String ppass) {
      return (url.equals(purl) && strcmp(user,puser) && strcmp(password,ppass));
    }

    /** Stupid little utility to deal with ((String)null).equals(x) errors. **/
    private static final boolean strcmp(String s1, String s2) {
      return (s1 ==null)?(s2==null):(s1.equals(s2));
    }

    // state management

    /** is the connection alive (not dead)? **/
    boolean aliveP;

    /** is the connection idle? **/
    boolean idleP;

    /** Time at which the connection was idled or 0 if active. **/
    long idleTime;
    
    /** Activate the connection carefully so that we do not interact
     * with the reaper.
     * @return true on success.
     **/
    synchronized boolean activate() {
      if (aliveP && idleP) {
        idleP = false;
        idleTime = 0;
        return true;
      } else {
        return false;
      }
    }

    /** Attempt to active a matching idle connection **/
    synchronized boolean tryActivate(String url, String u, String p) {
      if (aliveP && idleP) {
        if (compareWith(url,u,p)) {
          return activate();
        }
      }
      return false;
    }

    /** Idle the connection carefully.
     * @return true on success.
     **/
    synchronized boolean idle() {
      if (aliveP && !idleP) {
        idleP = true;
        idleTime = System.currentTimeMillis(); // idle it
        return true;
      } else {
        return false;           // fail
      }
    }

    /** reap the connection by shutting down the wrapped connection.
     * Checks for interactions with other threads, but does not check
     * for length of idle time.
     * Called both from the reaper thread via tryReap and from various
     * other threads via the Manager.  The latter case is used to reclaim 
     * pool slots connections when the pool has idle connections which don't match
     * the required connection.
     * @return true if the connection is closed and dead.
     **/
    synchronized boolean reap() {
      if (aliveP && idleP ) {
        try {
          connection.close();
        } catch (SQLException e) {
          synchronized(System.err) {
            System.err.println("Warning: reaped a closed Connection:"+e);
            e.printStackTrace();
          }
        }
        connection = null;
        aliveP = false;
        return true;
      } else {
        return false;
      }
    }        

    /** Consider reaping the connection, doing so if it seems like a good idea.
     * Called from the reaper thread to decide if a given connection should be
     * closed.
     * @return true if the connection is closed and 
     **/
    synchronized boolean tryReap(long reapIdle) {
      if (aliveP && idleP) {
        if ((System.currentTimeMillis()-idleTime)>=reapIdle) {
          return reap();
        }
      }
      return false;      
    }

    //
    // implement Connection
    //

    public void clearWarnings() throws SQLException {
      connection.clearWarnings();
    }
    /** complain if someone tries to close this.  The manager uses reap and/or 
     * idle to do similar things.
     **/
    public void close() {
      throw new RuntimeException("ManagedException instances should not be closed");
    }
    public void commit() throws SQLException {
      connection.commit();
    }
    public Statement createStatement() throws SQLException {
      return connection.createStatement();
    }
    public Statement createStatement(int rst, int rsc) throws SQLException {
      return connection.createStatement(rst, rsc);
    }
    public boolean getAutoCommit() throws SQLException {
      return connection.getAutoCommit();
    }
    public String getCatalog() throws SQLException {
      return connection.getCatalog();
    }
    public DatabaseMetaData getMetaData() throws SQLException {
      return connection.getMetaData();
    }
    public int getTransactionIsolation() throws SQLException {
      return connection.getTransactionIsolation();
    }
    public Map getTypeMap() throws SQLException {
      return connection.getTypeMap();
    }
    public SQLWarning getWarnings() throws SQLException {
      return connection.getWarnings();
    }
    public boolean isClosed() throws SQLException {
      return connection.isClosed();
    }
    public boolean isReadOnly() throws SQLException {
      return connection.isReadOnly();
    }
    public String nativeSQL(String sql) throws SQLException {
      return connection.nativeSQL(sql);
    }
    public CallableStatement prepareCall(String sql) throws SQLException {
      return connection.prepareCall(sql);
    }
    public CallableStatement prepareCall(String sql, int rst, int rsc) throws SQLException {
      return connection.prepareCall(sql, rst, rsc);
    }
    public PreparedStatement prepareStatement(String sql) throws SQLException {
      return connection.prepareStatement(sql);
    }
    public PreparedStatement prepareStatement(String sql, int rst, int rsc) throws SQLException {
      return connection.prepareStatement(sql, rst, rsc);
    }
    public void rollback() throws SQLException {
      connection.rollback();
    }
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      connection.setAutoCommit(autoCommit);
    }
    public void setCatalog(String cat) throws SQLException {
      connection.setCatalog(cat);
    }
    public void setReadOnly(boolean readOnly) throws SQLException {
      connection.setReadOnly(readOnly);
    }
    public void setTransactionIsolation(int level) throws SQLException {
      connection.setTransactionIsolation(level);
    }
    public void setTypeMap(Map map) throws SQLException {
      connection.setTypeMap(map);
    }
  }
  

  /*
  public static void main(String arg[]) {
    Properties p = System.getProperties();
    p.put(MAX_CONNECTIONS, "2"); // only two connections open
    p.put(REAP_TIME, "1000");   // run the reaper every second
    p.put(REAP_IDLE, "2000");   // reap anything idle for more than a second

    try {
      final JDBCConnectionManager man = JDBCConnectionManager.getInstance();
      
      man.registerDriver("oracle.jdbc.driver.OracleDriver");

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
                Connection c = man.getConnection(url,user,password);
                long t1 = System.currentTimeMillis();
                waitA.x += (t1-t0);
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select container_20_ft_qty from ue_summary_mtmc");
                while(rs.next()) { }
                // 44 rows
                s.close();
                man.returnConnection(c);
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
  
