/*
 * <copyright>
 *  Copyright 1997-2003 Networks Associates Technology, Inc
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
 *
 * Created on September 12, 2001, 10:55 AM
 */

package org.cougaar.core.service.identity;


/**
 * Reasons for revoking a certificate.
 */
public class CrlReason
{
  // revocation reasons:
  public static final int UNSPECIFIED            = 0;
  public static final int KEY_COMPROMISE         = 1;
  public static final int CA_COMPROMISE          = 2;
  public static final int AFFILIATION_CHANGED    = 3;
  public static final int SUPERSEDED             = 4;
  public static final int CESSATION_OF_OPERATION = 5;
  public static final int CERTIFICATE_HOLD       = 6;
  public static final int REMOVE_FROM_CRL        = 8;
  public static final int PRIVILEGE_WITHDRAWN    = 9;
  public static final int AA_COMPROMISE          = 10;

  private int reason;

  // private constructor, called only within this class
  public CrlReason(int value) {
    reason = value;
  }

  public int getReason() {
    return reason;
  }

  public String getReasonAsString() {
    switch (reason) {
      case UNSPECIFIED: return "UNSPECIFIED";
      case KEY_COMPROMISE: return "KEY_COMPROMISE";
      case CA_COMPROMISE: return "CA_COMPROMISE";
      case AFFILIATION_CHANGED: return "AFFILIATION_CHANGED";
      case SUPERSEDED: return "SUPERSEDED";
      case CESSATION_OF_OPERATION: return "CESSATION_OF_OPERATION";
      case CERTIFICATE_HOLD: return "CERTIFICATE_HOLD";
      case REMOVE_FROM_CRL: return "REMOVE_FROM_CRL";
      case PRIVILEGE_WITHDRAWN: return "PRIVILEGE_WITHDRAWN";
      case AA_COMPROMISE: return "AA_COMPROMISE";
      default: return "UNKNOWN ("+reason+")";
    }
  }

  public String toString() {
    return "Certificate revoked due to "+getReasonAsString();
  }
}
