/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.TimeZone;

/**
*   This class is responsible for creating and maintaining a Log file for a
*   specifc Alp Entity.  any entity may create its own Log Writer if it has access permission
*   to the computer resources managed by node
**/
public class LogWriter implements Runnable {

  /** The Buffered Writer for this file **/
  private BufferedWriter theWriter;
  /** The File name **/
  private String theFileName;
  /** The Logfiles maximum size **/
  private int theFileSize;
  /** The wrap arround flag default value true **/
  private boolean theWrapArround;
  /** The busy flag default value true **/
  private boolean theBusyFlag;
  /**  The sequence number or each logged event **/
  private int theItemNum = 0;
  /** The number of characters per line **/
  private int theCharsPerLine;
  /** the container of all the events that require logging
   *   This container eleminates the Race condition of receiving a new
   *   event during the logging of an exisiting event **/
  private Vector theEvents = null;

  private SimpleDateFormat dateFormat;


  /**
   *   Constructor.
   *   <p>
   *   Adds the default file name of Unkown.log
   **/
  public LogWriter(  ){
    this( "Unknown.log" );
  }

  /**
   *   Chained Constructor with a defined file name.
   *   <p><PRE>
   *   Adds the default values of
   *       fileSize = 1024
   *       columns = 80
   *       wrapArround = true
   *   <PRE/>
   *   @param aFullPath A String object that contains the full path for the file.
   *   If we do not specifiy any path and only a file name the file will be placed in the
   *   current working directory ( ie directory that the app was launched in)
   **/
  public LogWriter( String aFullPath  ) {
    this( aFullPath, 1024, 80, true );
  }

  /**
   *   Chained Constructor that enables the user to define all variables.
   *   <p>
   *   @param aFullPath A String object that contains the full path for the file.
   *   If we do not specifiy any path and only a file name the file will be placed in the
   *   current working directory ( ie directory that the app was launched in)
   *   @param aFileSize  The int value for the maximum length of the file.  Currently unsupported.
   *   @param charsPerLine The int value for the number of characters per line.  Currently unsupported.
   *   @param wrapArround  The boolean for weather the file is wrapArround or allowed to grow.  Currently all
   *   files grow wrap arround is unsupported
   ***/
  public LogWriter( String aFullPath, int aFileSize, int charsPerLine, boolean wrapArround ) {
    setFileName( aFullPath );
    setFileSize( aFileSize );
    setCharsPerLine( charsPerLine );
    setWrapArround( wrapArround );
    setWriter( );
    setEvents( new Vector() );
    setBusyFlag(false);
    dateFormat = new SimpleDateFormat( "EEE, MMM dd, yyyy 'at' hh:mm:ss z" );
    dateFormat.setTimeZone( TimeZone.getDefault() );
  }

  /**
   *   Method to close the logfile.
   *   <p><PRE>
   *       PRE CONDITION:  log file open to write to
   *       POST CONDITION: Saves and closes the log file
   *       INVARIANCE:     Data in file not compromised by closing call
   *   <PRE/>
   **/
  public synchronized void closeLog(){
    getEvents().addElement( "Closing Log File: " + getFileName() );
    logAllEvents();
    BufferedWriter w = getWriter();
    if (w != null) {
      try{
        w.flush();
        w.close();
      }
      catch ( IOException e ){
        System.err.println( "Error in LogWriter::closeLog: " + e + " I am: " + this );
      }
    }
  }

  /**
   *   Accessor method for theBusyFlag property.
   *   <p>
   *   @return boolean The boolean to mark the logging mechanism as having tasks to complete
   **/
  private final boolean getBusyFlag( ) { return theBusyFlag; }

  /**
   *   Accessor method for theCharsPerLine property.
   *   <p>
   *   @return int The int value for te number of characters per line
   **/
  private final int getCharsPerLine() { return theCharsPerLine; }

  /**
   *   Accessor method for theEvent property.
   *   <p>
   *   @return Vector The container holding all the objects to log to the file
   **/
  private final Vector getEvents() { return theEvents; }

  /**
   *   Accessor method for theFileName property.
   *   <p>
   *   @return String The full path/name of the file  If it only
   *   holds a file name then the path is the current working directory
   **/
  private final String getFileName( ) { return theFileName; }

  /**
   *   Accessor method of theFileSize property size is expresed in Bytes (1024 = 1KB).
   *   <p>
   *   @return int The int value for the file size in bytes**/
  private final int getFileSize( ) { return theFileSize; };

  /**
   *   Accessor method for theWrapArround property.
   *   <p>
   *   @return boolean The boolean value to state if the file is wrapArround or allowed to grow
   *   beyond the file size
   **/
  private final boolean getWrapArround( ) { return theWrapArround; }

  /**
   *   Accessor method for theWriter property.
   *   <p>
   *   @return BufferedWriter The buffered writer object controlling the file
   **/
  private final BufferedWriter getWriter( ) { return theWriter; }

  /**
   *   Accessor method for theWrapArround property.
   *   <p>
   *   @return boolean The boolean value to state if the file is wrapArround or allowed to grow
   *   beyond the file size
   **/
  private final boolean isWrapArround( ) { return theWrapArround; }
    
  /**
   *   Accessor method for theWrapArround property.
   *   <p>
   *   @return boolean The boolean value to state if the theBusyFlag
   **/
  public final boolean isBusyFlag( ) { return theBusyFlag; }


  /**
   *   Method to iterate through the events container and write to the log.
   *   Each event is removed from the vector as it is used
   *   <p><PRE>
   *       PRE CONDITION:  Thread was just created or just woken up by the addition of a log event
   *       POST CONDITION: Logs each object to the file an inforces the file constraints
   *       INVARIANCE:     No race conditions that would allow the loss of logging events
   *   <PRE/>
   **/
  private synchronized void logAllEvents(){
    setBusyFlag( true );
    Vector myEvents = null;
    myEvents = (Vector)getEvents();
    Enumeration e = myEvents.elements();
    while (e.hasMoreElements()) {
      Object nextEvent = e.nextElement();
      writeLogInfo();
      writeObject( nextEvent );
    }
        
    myEvents.removeAllElements();
    setBusyFlag(false);

  }

  /**
   *   Method to accept any Java object for logging to file and notify the
   *   Thread that an event is ready to be written to the log.
   *   <p><PRE>
   *       PRE CONDITION:  Object passed into the log object
   *       POST CONDITION: Posts the object tot he logging queue and wakews up the log thread
   *       INVARIANCE:
   *   <PRE/>
   *   @param anEvent The Object object that contains the event to log
   *   A log writer can thus log any object in the Alp system
   **/
  public synchronized void logEvent( Object anEvent ){
    if (getWriter() == null) return;
    getEvents().addElement( anEvent );
    notify();
  }

  /**
   *   Method from the runnable interface.
   *   <p><PRE>
   *       PRE CONDITION:  Object was just created or just received a notify
   *       POST CONDITION: Excutes the behavoir of this object in a continual loop
   *           And suspends itself when there is nothin else to do
   *       INVARIANCE:
   *   <PRE/>
   **/
  public synchronized void run() {
    if (getWriter() == null) return; // don't bother if no log.
    while(true){  //loop for ever

      logAllEvents();

      try{
          wait();  //only wake up when there is an event to log
      }
      catch( InterruptedException e ) {
        System.err.println( " InterruptedException in LogWriter::run: " + e );
      }
    }
  }

  /**
   *   Modifier method for theBusyFlag property.
   *   <p>
   *   @param aBusyFlag   The boolean value for the setting theBusyFlag
   **/
  public final void setBusyFlag( boolean aBusyFlag ) { theBusyFlag = aBusyFlag; }

  /**
   *   Modifier method for theCharsPerLine.
   *   <p>
   *   @param someChars    The int value of the nimber of characters per line
   **/
  public final void setCharsPerLine( int someChars ) { theCharsPerLine = someChars; }

  /**
   *   Modifer method for theEvent object.
   *   <p>
   *   @param someEvents    The Vector object containing events for logging
   **/
  private final void setEvents( Vector someEvents ) { theEvents = someEvents; }

  /**
   *   Modifier method for theFileName property.
   *   <p>
   *   @param aFileName    The String object containing the file name for logging
   **/
  private final void setFileName( String aFileName ) { theFileName = aFileName; }

  /**
   *   Modifier method of theFileSize property size is expresed in Bytes (1024 = 1KB).
   *   <p>
   *   @param aFileSize    The int value for the file size
   **/
  public final void setFileSize( int aFileSize ) { theFileSize = aFileSize; }

  /**
   *   Modifier method for theWrapArround property.
   *   <p>
   *   @param wrap    The boolean value for the setting the wrap arround function of the file
   **/
  public final void setWrapArround( boolean wrap ) { theWrapArround = wrap; }

  /**
   *   Modifier method for theWriter property.
   *   Constructs a new BufferedWriter controlling the log file
   *   <p>
   **/
  private final void setWriter(){
    try{
      theWriter = new BufferedWriter(new FileWriter( getFileName() ));
    }
    catch( IOException e ){
      System.err.println("Error in LogWriter::setWriter. Unable to allocate a BufferedReader: "+e+"\n\tWill not log." );
    }
  }

  /**
   *   Method to write the new object to the logfile.
   *   <p><PRE>
   *       PRE CONDITION:  An object to write to the log
   *       POST CONDITION: Write the string version of hte object to the log.
   *       INVARIANCE:     Encapsulated object data.
   *   <PRE/>
   *   @param anEvent  The object to write to the log
   **/
  private synchronized void writeObject( Object anEvent ){
    //now parse out the string into blocks of getCharsPerLine - 10
    String line = anEvent.toString();
    String base = "          ";  // indent by 10 spaces to offset from the log entry line

    //keep writing until the endIndex is out of bounds
    BufferedWriter w = getWriter();
    if (w != null) {
      try{
        w.write( base );
        w.write(line);
        w.newLine();
      }
      catch( IOException e ) {
        System.err.println("Error in LogWriter::writeObject: " + e + " I am: " + this );
      }
    }
  }



  /**
   *   Method to write the a new entry line into the log.
   *   <p><PRE>
   *       PRE CONDITION:  Called when an item needs to be written to the log
   *       POST CONDITION: writes a standrard log info block before each event
   *       INVARIANCE:     Logg file constraints are not violated
   *   <PRE/>
   **/
  private synchronized void writeLogInfo( ) {
    // Increment the theItemNumber
    theItemNum++;
    //Write the item number and the data time stamp
    String date = dateFormat.format( new Date() );
    BufferedWriter w = getWriter();
    if (w != null) {
      try{
        w.write( String.valueOf( theItemNum ));
        w.write( ".    ");
        w.write( date.toString());
        w.write(":" );
        w.newLine();
        w.flush();
      }
      catch( IOException e ) {
        System.err.println("Error in LogWriter::writeLogInfo: " + e + " I am: " + this );
      }
    }

  }


}


