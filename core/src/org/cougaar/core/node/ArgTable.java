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

import org.cougaar.core.mts.*;

import java.util.Hashtable;

/**
* This class provides the containment of all arguments passed into the main command line.
*       The class is fully self contained version of a Hashtable that accepts the String [] args
*       list passed in to the command line and creates a arg->value relationship.
*       all args not followed by a value point to a empty String.
*       Everything is stored as a String object.
*       <p>
*       Usage: java [CLASSNAME] [-c] [-d dbName] [-f configFileName ] [-h] [-help] [-n nodeName] [-t testFileName] [-w time]");
*       <ul>
*               <li>-c     Clear the database file.  Used with SANode only.</li>
*       <li>-d     Database name.  Default = Registry.db. Used with SANode only.</li>
*       <li>-f     Congiuration file name.  Default = [NAME]. Example test will map to file test.ini</li>
*       <li>-h     Print the help message.</li>
*       <li>-help  Print the help message.</li>
*       <li>-n     Name. Allows you to name the Node or Administrator.  Default Node = Computer's name / SANode = Administrator</li>
*       <li>-config config. Allows you to specify the configuration to use (instead of looking in the current directory) </li>
*       <li>-t     Test file to use for the test. Used with SANode only.</li>
*       <li>-w     Time delay for testing or testing cycle in milliseconds.  Used with SANode only.</li>
*       </ul>
**/
public final class ArgTable extends Hashtable implements ArgTableIfc {

  /**
   *   Default Constructor.
   *    Accepts the args list and either prints out the usage help or maps the args.
   *    Zero args is an acceptable command.
   *    <p>
   *    @param args The array of strings passed in at the command line.
   **/
  public ArgTable( String[] args ) {
    //Check to see if help was requested
    checkForHelp( args );
    //if not hte map the args
    mapArgs( args );
  }

  /**
   * Convert a List of Strings to a <code>String[]</code> and call the
   * <tt>ArgTable(String[])</tt> constructor.
   */
  public ArgTable(java.util.List l) {
    this((String[])l.toArray(new String[l.size()]));
  }

  /**
   *    This method takes inthe String[] and parses out the values and stores them into the 
   *    Hashtable.
   *    <p>
   *    @param args The array of strings passed in at the command line.
   **/
  public void mapArgs(  String[] args ){
    int argc = args.length;
    String check = null;
    String next = null;
    boolean sawname = false;
    for( int x = 0; x < argc;){
      check = args[x++];
      if (! check.startsWith("-") && !sawname) {
        sawname = true;
        if ("admin".equals(check)) 
          put(NAME_KEY, "Administrator");
        else
          put(NAME_KEY, check);

        // setup admin defaults
        if ("admin".equals(check) ||
            "Administrator".equals(check)) {
          put(REGISTRY_KEY, "");
          put(PORT_KEY, "8000" );
        }
      } else if (check.equals("-c"))
        put(CLEAR_KEY, "");
      else if (check.equals("-config"))
        put(CONFIG_KEY, args[x++]);
      else if (check.equals("-cs"))
        put(CS_KEY, args[x++]);
      else if (check.equals("-ns"))
        put(NS_KEY, args[x++]);
      else if (check.equals("-d"))
        put(DBNAME_KEY, args[x++] );
      else if (check.equals("-f"))
        put(FILE_KEY, args[x++] );
      else if (check.equals("-n")) {
        put(NAME_KEY, args[x++] );
        sawname = true;
      } else if (check.equals("-p"))
        put(PORT_KEY, args[x++] );
      else if (check.equals("-l")) 
        put(LOCAL_KEY, "");
      else if (check.equals("-r")) 
        put(REGISTRY_KEY, "");
      else if (check.equals("-t"))
        put(TEST_KEY, args[x++] );
      else if (check.equals("-w"))
        put(WAIT_KEY, args[x++] );
      else if (check.equals("-s"))
        put(SIGNED_PLUGIN_JARS, args[x++] );
      else if (check.equals("-X"))
        put(EXPERIMENT_ID_KEY, args[x++] );
      else if( check.indexOf("-") == 0 ){
        System.out.println("Bad argument encountered in parsing command line.");
        printHelp();
      }
                
    }
  }

  /**
   *    This method iterates over the argument list to se if the -h is include if so it
   *    class printHelp to print the usage and exit.
   *    <p>
   *    @param args The array of strings passed in at the command line.
   **/
  private void checkForHelp( String[] args  ){
    int argc = args.length;
    String check = null;
    for( int x = 0; x < argc; x++ ){
      check = args[x];
      if( check.equals("-h") || check.equals("-help") ){
        printHelp();
      }
    }
        
  }
    
  /**
   *    Prints the usage lines to System.out and then exits the program.
   **/
  private void printHelp(){
    System.out.println("Usage: java [CLASSNAME] [-c] [-d dbName] [-f configFileName ] [-h] [-help] [-n nodeName] [-p port] [-t testFileName] [-w time]");
    System.out.println("\t-c\tClear the database file.  Used with SANode only.");
    System.out.println("\t-d\tDatabase name.  Default = Registry.db. Used with SANode only.");
    System.out.println("\t-f\tCongiuration file name.  Default = [NAME]. Example test will map to file test.ini");
    System.out.println("\t-h\tPrint this help message.");
    System.out.println("\t-help\tPrint this help message.");
    System.out.println("\t-n\tName. Allows you to name the Node or Administrator.  Default Node = Computer's name / SANode = Administrator");
    System.out.println("\t-p\tPort.  The port number to use for the name server.");
    System.out.println("\t-r\tRegistry.  The flag to tell the Node to create the Registry.");
    System.out.println("\t-t\tTest file to use for the test. Used with SANode only.");
    System.out.println("\t-w\tTime delay for testing or testing cycle in milliseconds.  Used with SANode only.");
    System.exit(1);
  }


}
