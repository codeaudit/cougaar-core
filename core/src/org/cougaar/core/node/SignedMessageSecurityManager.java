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

import java.io.PrintStream;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.security.cert.Certificate;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageSecurityManager;

/** A useful MessageSecurityManager which signs each message, but depends on
 * the destination to recover the public key of the sender.
 *
 **/

public class SignedMessageSecurityManager implements MessageSecurityManager {

  public SignedMessageSecurityManager() {
  }

  public Message secureMessage(Message m) {
    return new SignedSecureMessage(m);
  }

  public Message unsecureMessage(SecureMessage m) {
    if (m instanceof SignedSecureMessage) {
      return ((SignedSecureMessage)m).extract();
    } else {
      return null;
    }
  }



  private static class SignedSecureMessage extends Message implements SecureMessage {
    private SignedObject secret;
    //private java.security.cert.Certificate cert;

    SignedSecureMessage(Message m) {
      super(m.getOriginator(), m.getTarget());
      secret = SignedMessageSecurityManager.sign(m);
      //cert = SignedMessageSecurityManager.getCert(origin.getAddress());
    }

    Message extract() {
      try {
        java.security.cert.Certificate cert = 
          KeyRing.getCert(getOriginator().getAddress());
        if (cert == null) {
          System.err.println("\nWarning: Dropping message, No public certificate for Origin \""+
                             getOriginator().getAddress()+"\": "+secret.getObject());
          return (Message) secret.getObject();
        }
        if (verify(secret, cert)) {
          return (Message) secret.getObject();
        } else {
          return null;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }
  }


  static SignedObject sign(Message m) {
    try {
      String origin = m.getOriginator().getAddress();
      PrivateKey pk = KeyRing.getPrivateKey(origin);
      if (pk == null) {
        System.err.println("\nWarning: Dropping message, Could not find private key for Origin \""+
                           origin+"\": "+m);
        return null;
      }
      Signature se = Signature.getInstance(pk.getAlgorithm());
      return new SignedObject(m, pk, se);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  static boolean verify(SignedObject so, java.security.cert.Certificate cert) {
    // check for bogus conditions.
    if (so == null || cert == null) return false;
    try {
      PublicKey pk = cert.getPublicKey();
      Signature ve = Signature.getInstance(so.getAlgorithm());
      return so.verify(pk, ve);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

}
