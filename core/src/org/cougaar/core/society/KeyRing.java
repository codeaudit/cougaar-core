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

import org.cougaar.util.ConfigFinder;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import java.security.*;
import java.security.cert.*;

/** A common holder for Security keystore information and functionality
 **/

public final class KeyRing {
  private static String ksPass;
  private static String ksPath;
  
  private static KeyStore keystore = null;

  static {
    Properties props = System.getProperties();

    String installpath = props.getProperty("org.cougaar.install.path");
    String defaultKeystorePath = installpath + File.separatorChar
                                + "configs" + File.separatorChar + "common"
                                + File.separatorChar + ".keystore";

    ksPass = props.getProperty("org.cougaar.security.keystore.password","alpalp");
    ksPath = props.getProperty("org.cougaar.security.keystore", defaultKeystorePath);

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
 
