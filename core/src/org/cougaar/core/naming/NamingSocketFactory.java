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

package org.cougaar.core.naming;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class NamingSocketFactory
    implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable
{
    private final static String USE_SSL_PROP = "org.cougaar.core.naming.useSSL";
    private final static String USE_SSL_DFLT = "false";
    private final static boolean useSSL =
        System.getProperty(USE_SSL_PROP, USE_SSL_DFLT).equals("true");

    /**
     * Singleton NamingSocketFactory.
     **/
    private static NamingSocketFactory instance;

    public synchronized static NamingSocketFactory getInstance() {
        if (instance == null) {
            instance = new NamingSocketFactory();
        }
        return instance;
    }

    private NamingSocketFactory() {
    }

    public Socket createSocket(String host, int port) throws IOException {
        return useSSL
            ? SSLSocketFactory.getDefault().createSocket(host, port)
            : new Socket(host, port);
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return useSSL
            ? SSLServerSocketFactory.getDefault().createServerSocket(port)
            : new ServerSocket(port);
    }
}
