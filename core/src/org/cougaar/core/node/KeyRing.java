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

package org.cougaar.core.node;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import org.cougaar.util.ConfigFinder;

/** A common holder for Security keystore information and functionality
 * @property org.cougaar.install.path Used to find keystore as "org.cougaar.install.path/configs/common/.keystore"
 * @property org.cougaar.security.keystore.password The password to the cougaar keystore.
 * @property org.cougaar.security.keystore The URL of the cougaar keystore.
 **/

public final class KeyRing {
  private static String ksPass;
  private static String ksPath;
  
  private static KeyStore keystore = null;

  static {
    String installpath = System.getProperty("org.cougaar.install.path");
    String defaultKeystorePath = installpath + File.separatorChar
                                + "configs" + File.separatorChar + "common"
                                + File.separatorChar + ".keystore";

    ksPass = System.getProperty("org.cougaar.security.keystore.password","alpalp");
    ksPath = System.getProperty("org.cougaar.security.keystore", defaultKeystorePath);

    System.out.println("Secure message keystore: path=" + ksPath + ", pass=" + ksPass);
  }
  
  private static void init() {
    try {
      keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream kss = ConfigFinder.getInstance().open(ksPath);
      keystore.load(kss, ksPass.toCharArray());
      kss.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static Object guard = new Object();
  
  public static KeyStore getKeyStore() { 
    synchronized (guard) {
      if (keystore == null) 
        init();
      return keystore; 
    }
  }

  private static HashMap privateKeys = new HashMap(89);
  static PrivateKey getPrivateKey(String name) {
    PrivateKey pk = null;
    try {
      synchronized (privateKeys) {
        pk = (PrivateKey) privateKeys.get(name);
        if (pk == null) {
          pk = (PrivateKey) getKeyStore().getKey(name, ksPass.toCharArray());
          privateKeys.put(name, pk);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to get PrivateKey for \""+name+"\": "+e);
      e.printStackTrace();
    }
    return pk;
  }

  private static HashMap certs = new HashMap(89);

  static java.security.cert.Certificate getCert(String name) {
    java.security.cert.Certificate cert = null;
    try {
      synchronized (certs) {
        cert = (java.security.cert.Certificate) certs.get(name);
        if (cert == null) {
          cert = getKeyStore().getCertificate(name);
          certs.put(name, cert);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to get Certificate for \""+name+"\": "+e);
    }
    return cert;
  }


}
 
