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

import org.cougaar.core.society.MessageSecurityManager;
import org.cougaar.core.society.SecureMessage;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransportServer;

import org.cougaar.util.ConfigFileFinder;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import java.security.*;
import java.security.cert.*;

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

  public void setMessageTransport(MessageTransportServer mts) {}

  private static class SignedSecureMessage extends Message implements SecureMessage {
    private SignedObject secret;
    //private java.security.cert.Certificate cert;

    SignedSecureMessage(Message m) {
      secret = SignedMessageSecurityManager.sign(m);
      //cert = SignedMessageSecurityManager.getCert(origin.getAddress());
    }

    Message extract() {
      try {
        java.security.cert.Certificate cert = 
          KeyRing.getCert(getOriginator().getAddress());
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
      Signature se = Signature.getInstance(pk.getAlgorithm());
      return new SignedObject(m, pk, se);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  static boolean verify(SignedObject so, java.security.cert.Certificate cert) {
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
