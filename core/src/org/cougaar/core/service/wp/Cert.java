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

package org.cougaar.core.service.wp;

import java.io.Serializable;
import java.security.cert.Certificate;

/**
 * A Cert holds the certification for an AddressEntry.
 * <p>
 * There are currently four built-in certs:<ul>
 *   <li>{@link #NULL}</li>
 *   <li>{@link #PROXY}</li>
 *   <li>{@link #Direct}</li>
 *   <li>{@link #Indirect}</li>
 * </ul>
 * User-defined Cert subclasses are also permitted.
 *
 * @deprecated see new AddressEntry factory method
 */
public abstract class Cert implements Serializable {

  /**
   * A "null" cert indicates that the client should contact the
   * entry address with no security (ie "in the open").
   */
  public static final Cert NULL = new Cert() {
    private Object readResolve() { return NULL; }
    public String toString() { return "null_cert"; }
  };

  /**
   * A "proxy" cert indicates that the client should contact
   * the agent with the name "CertProvider".
   * <p>
   * The cert provider agent will never specify a PROXY cert.
   */
  public static final Cert PROXY = new Cert() {
    private Object readResolve() { return PROXY; }
    public String toString() { return "proxy_cert"; }
  };

  /**
   * A "direct" cert contains a JAAS Crypto certificate.
   */
  public static final class Direct extends Cert {
    private final Certificate cert;
    private transient int _hc;
    public Direct(Certificate cert) { 
      this.cert = cert;
      if (cert == null) {
        throw new IllegalArgumentException("null cert");
      }
    }
    public Certificate getCertificate() { return cert; }
    public String toString() { return "(cert="+cert+")"; }
    public boolean equals(Object o) {
      return 
        (o == this || 
         (o instanceof Direct && cert.equals(((Direct)o).cert)));
    }
    public int hashCode() {
      if (_hc == 0) _hc = cert.hashCode();
      return _hc;
    }
  }

  /**
   * An "indirect" cert contains a query string which the client
   * should use to lookup the certificate.
   * <p>
   * The lookup location is application specific, but typically
   * refers to either a local file name, a distributed certificate
   * authority, or a yellow pages.
   */
  public static final class Indirect extends Cert {
    private final String query;
    public Indirect(String query) {
      this.query = query;
      if (query == null) {
        throw new IllegalArgumentException("null query");
      }
    }
    public String getQuery() { return query; }
    public String toString() { return "(query="+query+")"; }
    public boolean equals(Object o) {
      return
        (o == this ||
         (o instanceof Indirect && query.equals(((Indirect)o).query)));
    }
    public int hashCode() { return query.hashCode(); }
  }
}
