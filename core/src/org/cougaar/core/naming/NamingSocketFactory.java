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
import java.net.SocketException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocket;

public class NamingSocketFactory
    implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable
{
    private final static String USE_SSL_PROP = "org.cougaar.core.naming.useSSL";
    private final static String USE_SSL_DFLT = "false";
    private final static boolean useSSL =
        System.getProperty(USE_SSL_PROP, USE_SSL_DFLT).equals("true");

    private final static String SSL_INSTRUCTIONS = "\n"
        + "An exception occurred while trying to create an SSLServerSocket. This\n"
        + "has probably happened because you have not setup your security\n"
        + "environment completely. The following checklist should guide you\n"
        + "through this process.\n"
        + "\n"
        + "1. Install jsse (Java Secure Socket Extension). This is a largely\n"
        + "   manual procedure and so is prone to error or omission. Following\n"
        + "   the instructions supplied with the package. In particular, copy the\n"
        + "   three jar files to the jre/lib/ext subdirectory of your java\n"
        + "   installation and edit the jre/lib/security/java.security file to\n"
        + "   add the jsee security provider. The result should resemble this:\n"
        + "      security.provider.1=sun.security.provider.Sun\n"
        + "      security.provider.2=com.sun.rsajca.Provider\n"
        + "      security.provider.3=com.sun.net.ssl.internal.ssl.Provider\n"
        + "2. Create a keystore. The keystore contains keys and certificates to\n"
        + "   be used for SSL sockets. The full procedure involves the use of a\n"
        + "   Certificate Authority to provide properly signed certificates\n"
        + "   embodying a public key and a chain of certificates back to a\n"
        + "   trusted signer. Until our security infrastructure is completed, the\n"
        + "   following will suffice to enable execution:\n"
        + "      keytool -keystore keystore -alias cougaar -genkey\n"
        + "      keytool -keystore keystore -alias cougaar -selfcert\n"
        + "3. Make the keystore be used by the key and trust managers. To do\n"
        + "   this, set these properties in the startup scripts:\n"
        + "      -Djavax.net.ssl.trustStore=keystore\n"
        + "      -Djavax.net.ssl.trustStorePassword=password\n"
        + "      -Djavax.net.ssl.keyStore=keystore\n"
        + "      -Djavax.net.ssl.keyStorePassword=password\n"
        + "   Naturally, the passwords should match those used to create the\n"
        + "   keystore.\n";

    /**
     * Singleton NamingSocketFactory.
     **/
    private static NamingSocketFactory instance;

    /**
     * Print ssl instructions on the first error
     **/
    private static boolean firstError = true;

    public synchronized static NamingSocketFactory getInstance() {
        if (instance == null) {
            instance = new NamingSocketFactory();
        }
        return instance;
    }

    private NamingSocketFactory() {
    }

    public Socket createSocket(String host, int port) throws IOException {
        if (useSSL) {
            try {
                return SSLSocketFactory.getDefault().createSocket(host, port);
            } catch (SocketException se) {
                if (firstError) {
                    firstError = false;
                    System.err.print(SSL_INSTRUCTIONS);
                }
                throw se;
            }
        } else {
            return new Socket(host, port);
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        if (useSSL) {
            try {
                SSLServerSocket s = (SSLServerSocket)
                    SSLServerSocketFactory.getDefault().createServerSocket(port);
                s.setNeedClientAuth(true);
                return s;
            } catch (SocketException se) { 
                if (firstError) {
                    firstError = false;
                    System.err.print(SSL_INSTRUCTIONS);
                }
                throw se;
            }
        } else {
            return new ServerSocket(port);
        }
    }
}
