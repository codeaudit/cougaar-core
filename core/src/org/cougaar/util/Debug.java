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

package org.cougaar.util;

import java.applet.Applet;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;


/**
 * An abstract class that presents a static interface for debugging output.
 * <p>
 * Debugging output is turned on or off by system properties for
 * applications, or parameters for applets.
 * <p>
 * A programmer can use code like the following:
 * <p><code>
 * Debug.message("foo", "Got " + nbytes + " bytes of data.");
 * </code>
 * <p>
 * The message gets printed when the application is run with -Ddebug.foo
 * or when the applet gets run with:
 * <p>
 * &lt param name=debug.foo value= &gt
 * <p>
 * The special token <code>debug.all</code> turns on all debugging for
 * both applets and applications.
 *
 * @author Tom Mitchell (tmitchell@bbn.com)
 *
 */
public abstract class Debug {

    /**
     * Don't allow construction, all methods are static.
     */
    private Debug () {}

    /**
     * Globally enable or disable debugging.
     */
    public static final boolean On = false;



    /**
     * The stream where debugging output should go.  Defualt is System.out.
     */
    public static PrintStream out = System.out;



    /**
     * Flag to indicate whether all debug messages should get printed.
     * This is shorthand for defining all the debug symbols.
     */
    public static boolean debugAll = false;



    /**
     * The user specified flag to indicate all debugging is on.
     * Default is "all".
     */
    public static String debugAllToken = "all";


    private static Hashtable dbgTable = new Hashtable();
    private static String debugTokenHeader = "debug.";

    public static String debugUnimplementedMessageToken = "unimplemented";
   /**
     * Employ this message to give developers a heads-up
     * about what methods are not yet implemented.
     * This is especially useful when developers are developing
     * in parallel and due to time constraints, some methods are
     * not fully implemented.  It is expected that when someone
     * encounters this message, they should investigate the source
     * of the code and contact the developer listed in the whoToContact
     * parameter immediatly for resolution.
     * To enable, use either Debug.debugAllToken
     * or Debug.debugUnimplementedMessageToken.
     * @param className - String valued name associated with the method's class.
     * @param method - String valued name of the method that is not implemented.
     * @param whoToContact - String valued name of the developers(s) who should be contacted.
     * @param msg - String valued message for additional context.
     */
   public static void unimplementedMethod(String className,
                                     String method,
                                     String whoToContact,
                                     String msg)
    {
        Debug.message(debugUnimplementedMessageToken,
           "Unimplemented method tripped: " + className + "." + method + " : " + msg + " : contact " + whoToContact + " for more details. ");
    }



    /**
     * Initialize debugging for the given applet.  Applets must pass
     * an array of parameters because the applet Parameters list cannot
     * be accessed in whole, only queried.  The parameters list looks
     * something like this:
     * <p><pre><code>
     * String[] debugTokens = {
     *  "debug.debug",		// org.cougaar.util.Debug
     *  "debug.component",	// org.cougaar.component
     *  "debug.plugin",	    // com.bbn.openmap.awt.MapPanel
     *  "debug.unimplemented"		// unimplemented method warning
     *  };
     * </code></pre>
     *
     * @param applet The applet
     * @param parameters The debugging flags to look for in the applet's
     *                   parameters list
     */
    public static void init (Applet applet, String[] parameters) {
	if (applet == null) {
	    init(System.getProperties());
	}
	else {
	    for (int i=0; i<parameters.length; i++) {
		String pname = parameters[i];
		if (pname.startsWith(debugTokenHeader) &&
		    (applet.getParameter(parameters[i]) != null)) {
		    String token = pname.substring(debugTokenHeader.length());
		    dbgTable.put(token, Boolean.TRUE);
		}
	    }
	    // look for special debug.all token!
	    if (applet.getParameter(debugTokenHeader + debugAllToken) != null) {
		dbgTable.put(debugAllToken, Boolean.TRUE);
	    }

	}

	Debug.postInit();
    }



    /**
     * Initialize debugging for an application.  Debugging symbols are
     * detected in the given properties list, and must have the form
     * "debug.X", where X is a debug token used in the application.
     *
     * @param p A properties list, usually System.getProperties()
     */
    public static void init (Properties p) {
	Enumeration e = p.propertyNames();
	while (e.hasMoreElements()) {
	    String name = e.nextElement().toString();
	    if (name.startsWith(debugTokenHeader)) {
		String token = name.substring(debugTokenHeader.length());
		dbgTable.put(token, Boolean.TRUE);
	    }
	}
	Debug.postInit();
    }



    /**
     * Common inits, regardless of applet or application.
     */
    private static void postInit () {
	debugAll = dbgTable.containsKey(debugAllToken);
    }



    /**
     * Indicates if the debugging for the named token is on or off.
     *
     * @param token a candidate token
     * @return true if debugging is on, false otherwise.
     */
    public static boolean debugging (String token) {
	return On && (debugAll || dbgTable.containsKey(token));
    }



    /**
     * Prints <code>message</code> if <code>dbgToken</code> debugging
     * is on.
     *
     * @param dbgToken a token to be tested by debugging()
     * @param message a message to be printed
     */
    public static void message (String dbgToken, String message) {
	if (Debug.On && Debug.debugging(dbgToken)) {
	    Debug.out.println(message);
	}
        /*
        else {
          System.err.println("\nCheck debug call:");
          Thread.dumpStack();
        }
        */
    }



    /**
     * Sets the debugging output stream to the named stream.
     *
     * @param out the desired debugging output stream
     */
    public static void setPrintStream (PrintStream out) {
	Debug.out = out;
    }



    /**
     * Accessor for the current debugging output stream.
     *
     * @return the current debugging output stream.
     */
    public static PrintStream getPrintStream () {
	return out;
    }



    /**
     * Dummy function to illustrate usage of the debugging class.
     */
    public static void sampleUsage () {
	if (Debug.On && Debug.debugging("debug")) {
	    Debug.out.println("debug message");
	} else {
	    Debug.out.println("try again");
	}
    }



    /**
     * <code>main</code> routine used in unit testing.
     */
    public static void main (String args[]) {
	Debug.init(System.getProperties());
	Debug.sampleUsage();
    }
}
