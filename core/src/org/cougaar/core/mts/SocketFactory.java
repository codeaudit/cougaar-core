/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.mts;

import org.cougaar.core.component.ServiceBroker;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;
import javax.net.ServerSocketFactory;
// import javax.net.SocketFactory; FIXME bug 2494
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * This is not really a factory, it just needs to use AspectFactory's
 * attachAspects method.  The purpose of this class is to create an
 * RMISocketFactory with aspectizable streams.
 * <p>
 * Instantiating this class creates the socket factory.
 *
 * @property org.cougaar.core.security.ssl.KeyRingSSLFactory
 *   SSL socket factory classname (default is JDK's impl).
 * @property org.cougaar.core.security.ssl.KeyRingSSLServerFactory
 *   SSL server socket factory classname (default is JDK's impl).
 * @property org.cougaar.message.transport.server_socket_class
 *   ServerSocketWrapper classname (default is no wrapper).
 */
public class SocketFactory  
    extends RMISocketFactory
    implements java.io.Serializable
{

    // This has to be set very early from outside
    private static transient SocketControlProvisionService PolicyProvider; 

    static void configureProvider(ServiceBroker sb) {
	PolicyProvider = (SocketControlProvisionService)
	    sb.getService(sb, SocketControlProvisionService.class, null);
    }

    // We're not using this anymore but keep it around for reference.
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

    // NAI Linkage

    private static final String SSL_SOCFAC_CLASS =
	"org.cougaar.core.security.ssl.KeyRingSSLFactory";
    private static final String SSL_SERVER_SOCFAC_CLASS =
	"org.cougaar.core.security.ssl.KeyRingSSLServerFactory";

    private static final Class[] FORMALS = {};
    private static final Object[] ACTUALS = {};

    private static SSLSocketFactory getSSLSocketFactory() {
	Class cls = null;
	try {
	    cls = Class.forName(SSL_SOCFAC_CLASS);
	} catch (ClassNotFoundException cnf) {
	    // silently use the default class
	    return (SSLSocketFactory) SSLSocketFactory.getDefault();
	}

	try {
	    Method meth = cls.getMethod("getDefault", FORMALS);
	    return (SSLSocketFactory) meth.invoke(cls, ACTUALS);
	} catch (Exception ex) {
	    ex.printStackTrace();
	    return (SSLSocketFactory) SSLSocketFactory.getDefault();
	}
    }

    private static ServerSocketFactory getSSLServerSocketFactory() {
	Class cls = null;
	try {
	    cls = Class.forName(SSL_SERVER_SOCFAC_CLASS);
	} catch (ClassNotFoundException cnf) {
	    // silently use the default class
	    return SSLServerSocketFactory.getDefault();
	}

	try {
	    Method meth = cls.getMethod("getDefault", FORMALS);
	    return (ServerSocketFactory) meth.invoke(cls, ACTUALS);
	} catch (Exception ex) {
	    ex.printStackTrace();
	    return SSLServerSocketFactory.getDefault();
	}
    }




    private static final String WRAPPER_CLASS_PROP =
	"org.cougaar.message.transport.server_socket_class";

    private static Class serverWrapperClass;
    static {
	String classname = System.getProperty(WRAPPER_CLASS_PROP);
	if (classname != null) {
	    try {
		serverWrapperClass = Class.forName(classname);
	    } catch (Exception ex) {
		System.err.println(ex);
	    }
	}
    }

    // The factory will be serialized along with the MTImpl, and we
    // definitely don't want to include the AspectSupport when that
    // happens.  Instead, the aspect delegation will be handled by a
    // special static call.
    boolean use_ssl, use_aspects;
    transient SocketControlPolicy policy;

    public SocketFactory(boolean use_ssl, boolean use_aspects) 
    {
	this.use_ssl = use_ssl;
	this.use_aspects = use_aspects;
	// get the policy from a service
    }

    public boolean isMTS() {
	return use_aspects;
    }

    public boolean usesSSL() {
	return use_ssl;
    }

    public Socket createSocket(String host, int port) 
	throws IOException, UnknownHostException 
    {
	Socket socket = new Socket();
	InetSocketAddress endpoint = new InetSocketAddress(host, port);
	if (policy == null && PolicyProvider != null) {
	    policy = PolicyProvider.getPolicy();
	}
	
	if (policy != null) {
	    int timeout = policy.getConnectTimeout(this, host, port);
	    socket.connect(endpoint, timeout);
	} else {
	    socket.connect(endpoint);
	}

	if (use_ssl) {
	    SSLSocketFactory socfac = getSSLSocketFactory();
	    socket = socfac.createSocket(socket, host, port, true);
	}

 	return use_aspects ? 
	    AspectSupportImpl.attachRMISocketAspects(socket) :
	    socket;
    }

    



    public ServerSocket createServerSocket(int port) 
	throws IOException 
    {
	ServerSocket s = null;
	if (use_ssl) {
	    ServerSocketFactory factory = getSSLServerSocketFactory();
	    s = factory.createServerSocket(port);
	} else {
	    s = new ServerSocket(port);
	}
	if (serverWrapperClass != null) {
	    try {
		ServerSocketWrapper wrapper = (ServerSocketWrapper)
		    serverWrapperClass.newInstance();
		wrapper.setDelegate(s);
		return wrapper;
	    } catch (Exception ex) {
		System.err.println(ex);
		return s;
	    }
	} else {
	    return s;
	}
    }

    public int hashCode() {
        if (use_ssl) {
            if (use_aspects) return 0;
            return 1;
        } else {
            if (use_aspects) return 2;
            return 3;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass() == this.getClass()) {
            SocketFactory that = (SocketFactory) o;
            return this.use_ssl == that.use_ssl && this.use_aspects == that.use_aspects;
        }
        return false;
    }
}
